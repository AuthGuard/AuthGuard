package com.nexblocks.authguard.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XmlRequestParser {
    static Document secureParse(final String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            try (var inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
                return documentBuilder.parse(inputStream);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse SAMLRequest", e);
        }
    }

    static Element getFirstElement(final Document doc, final QName qname) {
        NodeList nodes = doc.getElementsByTagNameNS(qname.getNamespaceURI(), qname.getLocalPart());
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    static Element getFirstChild(final Element parent, final QName qname) {
        NodeList nodes = parent.getElementsByTagNameNS(qname.getNamespaceURI(), qname.getLocalPart());

        if (nodes.getLength() == 0) {
            return null;
        }

        return (Element) nodes.item(0);
    }

    static String getAttribute(final Element element, final String attributeName) {
        String value = element.getAttribute(attributeName);
        return value == null || value.isBlank() ? null : value.trim();
    }

    static Boolean boolBooleanAttribute(final Element element, final String attributeName) {
        String value = getAttribute(element, attributeName);

        if (value == null) {
            return null;
        }

        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
