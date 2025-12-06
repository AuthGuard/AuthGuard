package com.nexblocks.authguard.saml.exchange;

import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlConditionalAuthn;
import com.nexblocks.authguard.service.model.*;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class XmlSamlResponseBuilder {

    private static boolean openSamlInitialized = false;

    static {
        initializeOpenSAML();
    }

    private static void initializeOpenSAML() {
        if (!openSamlInitialized) {
            try {
                InitializationService.initialize();
                BasicParserPool parserPool = new BasicParserPool();
                parserPool.initialize();
                openSamlInitialized = true;
            } catch (InitializationException | ComponentInitializationException e) {
                throw new RuntimeException("Failed to initialize OpenSAML", e);
            }
        }
    }

    public static Response createResponse(final SamlAuthnRequest requestInfo,
                                          final String issuerUrl,
                                          final DateTime now) {
        String responseId = "_" + UUID.randomUUID();

        return createResponse(responseId, requestInfo, issuerUrl, now);
    }

    public static Assertion createAssertion(final SamlAuthnRequest request,
                                            final AccountBO account,
                                            final TokenOptionsBO tokenOptions,
                                            final String issuerUrl,
                                            final DateTime now) {
        String assertionId = "_" + UUID.randomUUID();
        String sessionIndex = "_" + tokenOptions.getTrackingSession(); // TODO should be stored to allow single log-out

        DateTime notOnOrAfter = now.plusSeconds(300); // 5 minutes validity
        DateTime notBefore = now.minusSeconds(5);

        return createAssertion(request, account, tokenOptions, issuerUrl, assertionId, sessionIndex,
                now, notBefore, notOnOrAfter);
    }

    private static Response createResponse(final String responseId,
                                           final SamlAuthnRequest request,
                                           final String issuerUrl,
                                           final DateTime now) {
        // Get Response builder
        Response response = (Response) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Response.DEFAULT_ELEMENT_NAME).buildObject(Response.DEFAULT_ELEMENT_NAME);

        response.setID(responseId);
        response.setVersion(SAMLVersion.VERSION_20);
        response.setIssueInstant(now);
        response.setDestination(request.getAcsUrl());
        if (StringUtils.isNotBlank(request.getRequestId())) {
            response.setInResponseTo(request.getRequestId());
        }

        // Create Issuer
        Issuer issuer = (Issuer) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(issuerUrl);
        response.setIssuer(issuer);

        // Create Status
        Status status = (Status) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Status.DEFAULT_ELEMENT_NAME).buildObject(Status.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = (StatusCode) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);
        response.setStatus(status);

        return response;
    }

    private static Assertion createAssertion(final SamlAuthnRequest request,
                                             final AccountBO account,
                                             final TokenOptionsBO tokenOptions,
                                             final String issuerUrl,
                                             final String assertionId,
                                             final String sessionIndex,
                                             final DateTime now,
                                             final DateTime notBefore,
                                             final DateTime notOnOrAfter) {
        // Create Assertion
        Assertion assertion = (Assertion) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Assertion.DEFAULT_ELEMENT_NAME).buildObject(Assertion.DEFAULT_ELEMENT_NAME);

        assertion.setID(assertionId);
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(now);

        // Create Issuer
        Issuer issuer = (Issuer) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(issuerUrl);
        assertion.setIssuer(issuer);

        // Create Subject
        Subject subject = createSubjectAssertion(request, account, notOnOrAfter);

        assertion.setSubject(subject);

        // Create Conditions
        Conditions conditions = createConditions(request, notBefore, notOnOrAfter);
        assertion.setConditions(conditions);

        // Create AuthnStatement
        AuthnStatement authnStatement = createAuthStatement(request, tokenOptions, now, sessionIndex, notOnOrAfter);
        assertion.getAuthnStatements().add(authnStatement);

        // Create AttributeStatement
        AttributeStatement attributeStatement = createAttributesAssertion(request, account);
        assertion.getAttributeStatements().add(attributeStatement);

        return assertion;
    }

    private static Subject createSubjectAssertion(final SamlAuthnRequest request, final AccountBO account,
                                                  final DateTime notOnOrAfter) {
        // Create Subject
        Subject subject = (Subject) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Subject.DEFAULT_ELEMENT_NAME).buildObject(Subject.DEFAULT_ELEMENT_NAME);

        // Create NameID
        NameID nameIdObj = (NameID) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(NameID.DEFAULT_ELEMENT_NAME).buildObject(NameID.DEFAULT_ELEMENT_NAME);

        nameIdObj.setSPNameQualifier(request.getIssuer());
        // TODO should we do nameIdObj.setNameQualifier() too?

        if (request.getNameIdFormat() == SamlAuthnRequest.NameIdFormat.TRANSIENT) {
            nameIdObj.setFormat(NameIDType.TRANSIENT);
            nameIdObj.setValue("_" + UUID.randomUUID());
        } else if (request.getNameIdFormat() == SamlAuthnRequest.NameIdFormat.PERSISTENT) {
            nameIdObj.setFormat(NameIDType.PERSISTENT);
            nameIdObj.setValue(String.valueOf(account.getId()));
        } else if (request.getNameIdFormat() == SamlAuthnRequest.NameIdFormat.EMAIL_ADDRESS) {
            nameIdObj.setFormat(NameIDType.EMAIL);
            nameIdObj.setValue(account.getEmail().getEmail());
        } else {
            nameIdObj.setFormat(NameIDType.PERSISTENT);
            nameIdObj.setValue(String.valueOf(account.getId()));
        }

        subject.setNameID(nameIdObj);

        // Create SubjectConfirmation
        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME).buildObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);

        SubjectConfirmationData subjectConfirmationData = (SubjectConfirmationData) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME).buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        subjectConfirmationData.setNotOnOrAfter(notOnOrAfter);
        subjectConfirmationData.setRecipient(request.getAcsUrl());

        if (StringUtils.isNotBlank(request.getRequestId())) {
            subjectConfirmationData.setInResponseTo(request.getRequestId());
        }

        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        subject.getSubjectConfirmations().add(subjectConfirmation);

        return subject;
    }

    private static Conditions createConditions(final SamlAuthnRequest requestInfo,
                                               final DateTime notBefore,
                                               final DateTime notOnOrAfter) {
        Conditions conditions = (Conditions) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Conditions.DEFAULT_ELEMENT_NAME).buildObject(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(notBefore);
        conditions.setNotOnOrAfter(notOnOrAfter);

        AudienceRestriction audienceRestriction = (AudienceRestriction) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME).buildObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);

        Audience audienceObj = (Audience) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Audience.DEFAULT_ELEMENT_NAME).buildObject(Audience.DEFAULT_ELEMENT_NAME);
        audienceObj.setAudienceURI(requestInfo.getIssuer());
        audienceRestriction.getAudiences().add(audienceObj);

        conditions.getAudienceRestrictions().add(audienceRestriction);

        return conditions;
    }

    private static AuthnStatement createAuthStatement(final SamlAuthnRequest request,
                                                      final TokenOptionsBO tokenOptions,
                                                      final DateTime now,
                                                      final String sessionIndex,
                                                      final DateTime notOnOrAfter) {

        /*
         * TODO we need to move this check upstream, this is meant to only build
         *  the response after all checks had passed
         */
        String currentPossibleClassRef = tokenSourceToClassRef(tokenOptions);

        // 2) Enforce RequestedAuthnContext (if any)
        final SamlAuthnRequest.AuthnClassRefs rac = request.getAuthnClassRefs();
        if (!(rac == null || rac.isUnset())) {
            if (!SamlConditionalAuthn.satisfiesRequestedContext(rac, currentPossibleClassRef)) {
                // You can also check request.isPassive() here and choose a different error (e.g., NoPassive)
                throw new IllegalStateException("RequestedAuthnContext not satisfied: requested="
                        + rac.getClassRefs() + " comparison=" + rac.getComparison() + " actual=" + currentPossibleClassRef);
            }
        }

        AuthnStatement authnStatement = (AuthnStatement) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME).buildObject(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(now);
        authnStatement.setSessionIndex(sessionIndex);
        authnStatement.setSessionNotOnOrAfter(notOnOrAfter);

        AuthnContext authnContext = (AuthnContext) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME).buildObject(AuthnContext.DEFAULT_ELEMENT_NAME);

        AuthnContextClassRef authnContextClassRef = (AuthnContextClassRef) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME).buildObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);

        authnContextClassRef.setAuthnContextClassRef(currentPossibleClassRef);

        authnContext.setAuthnContextClassRef(authnContextClassRef);

        authnStatement.setAuthnContext(authnContext);

        return authnStatement;
    }

    private static String tokenSourceToClassRef(TokenOptionsBO tokenOptions) {
        return switch (tokenOptions.getSource()) {
            case "basic" -> AuthnContext.PPT_AUTHN_CTX;
            case "otp", "totp" -> AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX;
            default -> AuthnContext.PPT_AUTHN_CTX;           // sane default
        };
    }

    private static AttributeStatement createAttributesAssertion(final SamlAuthnRequest request,
                                                                final AccountBO account) {
        AttributeStatement attributeStatement = (AttributeStatement) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME).buildObject(AttributeStatement.DEFAULT_ELEMENT_NAME);

        account.getIdentifiers().stream()
                .filter(identifier -> identifier.getType() == UserIdentifier.Type.USERNAME)
                .findFirst()
                .map(UserIdentifierBO::getIdentifier)
                .ifPresent(username -> {
                    Attribute usernameAttribute = createAttribute("username", username);
                    attributeStatement.getAttributes().add(usernameAttribute);
                });

        if (account.getEmail() != null) {
            Attribute emailAttribute = createAttribute("email", account.getEmail().getEmail());
            Attribute emailVerifiedAttribute = createAttribute("emailVerified", String.valueOf(account.getEmail().isVerified()));

            attributeStatement.getAttributes().add(emailAttribute);
            attributeStatement.getAttributes().add(emailVerifiedAttribute);
        }

        if (account.getRoles() != null) {
            Attribute rolesAttribute = createAttribute("roles", account.getRoles());
            attributeStatement.getAttributes().add(rolesAttribute);
        }

        if (account.getPermissions() != null) {
            List<String> permissions = account.getPermissions().stream().map(PermissionBO::toString).collect(Collectors.toList());

            Attribute permissionsAttribute = createAttribute("permissions", permissions);
            attributeStatement.getAttributes().add(permissionsAttribute);
        }

        return attributeStatement;
    }

    private static Attribute createAttribute(final String name, final String value) {
        Attribute attribute = (Attribute) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Attribute.DEFAULT_ELEMENT_NAME).buildObject(Attribute.DEFAULT_ELEMENT_NAME);

        attribute.setName(name);

        XSString attributeValue = createAttributeValue(value);

        attribute.getAttributeValues().add(attributeValue);

        return attribute;
    }

    private static Attribute createAttribute(final String name, final Collection<String> values) {
        Attribute attribute = (Attribute) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Attribute.DEFAULT_ELEMENT_NAME).buildObject(Attribute.DEFAULT_ELEMENT_NAME);

        attribute.setName(name);

        values.stream().map(XmlSamlResponseBuilder::createAttributeValue)
                .forEach(attributeValue -> attribute.getAttributeValues().add(attributeValue));

        return attribute;
    }

    private static XSString createAttributeValue(final String value) {
        XSString attributeValue = (XSString) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(XSString.TYPE_NAME).buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);

        attributeValue.setValue(value);

        return attributeValue;
    }
}
