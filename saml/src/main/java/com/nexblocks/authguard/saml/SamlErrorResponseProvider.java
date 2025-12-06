package com.nexblocks.authguard.saml;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;

import java.util.UUID;

public class SamlErrorResponseProvider {
    public static Response noPassive(final String idpEntityID,
                                     final String acsUrl,
                                     final String inResponseTo) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.RESPONDER, StatusCode.NO_PASSIVE, "Passive authentication not possible");
    }

    public static Response authnFailed(final String idpEntityID,
                                       final String acsUrl,
                                       final String inResponseTo,
                                       final String message) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.RESPONDER, StatusCode.AUTHN_FAILED,
                message != null ? message : "Authentication failed");
    }

    public static Response invalidNameIDPolicy(final String idpEntityID,
                                               final String acsUrl,
                                               final String inResponseTo) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.REQUESTER, StatusCode.INVALID_NAMEID_POLICY,
                "Requested NameID format not supported");
    }

    public static Response unsupportedBinding(final String idpEntityID,
                                              final String acsUrl,
                                              final String inResponseTo) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.RESPONDER, StatusCode.UNSUPPORTED_BINDING,
                "Requested protocol binding not supported");
    }

    public static Response requestDenied(final String idpEntityID,
                                         final String acsUrl,
                                         final String inResponseTo,
                                         final String reason) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
                reason != null ? reason : "Request denied");
    }

    public static Response requester(final String idpEntityID,
                                     final String acsUrl,
                                     final String inResponseTo,
                                     final String message) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.REQUESTER, null, message != null ? message : "Invalid request");
    }

    public static Response responder(final String idpEntityID,
                                     final String acsUrl,
                                     final String inResponseTo,
                                     final String message) {
        return build(idpEntityID, acsUrl, inResponseTo,
                StatusCode.RESPONDER, null, message != null ? message : "Unable to fulfill request");
    }

    private static Response build(final String idpEntityID,
                                  final String acsUrl,
                                  final String inResponseTo,
                                  final String topLevelStatus,
                                  final String secondLevelStatus,
                                  final String statusMessage) {
        Response resp = (Response) build(Response.DEFAULT_ELEMENT_NAME);
        resp.setID("_" + UUID.randomUUID());
        resp.setVersion(SAMLVersion.VERSION_20);
        resp.setIssueInstant(DateTime.now(DateTimeZone.UTC));
        resp.setDestination(acsUrl);
        if (inResponseTo != null) resp.setInResponseTo(inResponseTo);

        Issuer issuer = (Issuer) build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(idpEntityID);
        resp.setIssuer(issuer);

        StatusCode top = (StatusCode) build(StatusCode.DEFAULT_ELEMENT_NAME);
        top.setValue(topLevelStatus);

        if (secondLevelStatus != null) {
            StatusCode sub = (StatusCode) build(StatusCode.DEFAULT_ELEMENT_NAME);
            sub.setValue(secondLevelStatus);
            top.setStatusCode(sub);
        }

        Status status = (Status) build(Status.DEFAULT_ELEMENT_NAME);
        status.setStatusCode(top);

        if (statusMessage != null && !statusMessage.isBlank()) {
            StatusMessage sm = (StatusMessage) build(StatusMessage.DEFAULT_ELEMENT_NAME);
            sm.setMessage(statusMessage);
            status.setStatusMessage(sm);
        }

        resp.setStatus(status);
        return resp;
    }

    private static XMLObject build(final javax.xml.namespace.QName qname) {
        return XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilderOrThrow(qname)
                .buildObject(qname);
    }
}

