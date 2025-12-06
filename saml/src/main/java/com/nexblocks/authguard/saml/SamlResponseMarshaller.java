package com.nexblocks.authguard.saml;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SamlResponseMarshaller {
    public static String toBase64XmlString(final XMLObject xmlObject) {
        String xmlResponse = xmlToString(xmlObject);

        return Base64.getEncoder()
                .encodeToString(xmlResponse.getBytes(StandardCharsets.UTF_8));
    }

    public static String xmlToString(final XMLObject xmlObject) {
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport
                    .getMarshallerFactory()
                    .getMarshaller(xmlObject);
            if (marshaller == null) {
                throw new RuntimeException("No marshaller for " + xmlObject.getElementQName());
            }

            Element element = marshaller.marshall(xmlObject);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();

        } catch (MarshallingException | RuntimeException e) {
            throw new RuntimeException("Failed to marshall XMLObject", e);
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        }
    }
}
