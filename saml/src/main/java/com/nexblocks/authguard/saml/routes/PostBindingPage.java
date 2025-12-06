package com.nexblocks.authguard.saml.routes;


public final class PostBindingPage {
    private PostBindingPage() {}

    public static String render(final String acsUrl, final String base64SAMLResponse, final String relayState) {
        String safeRelay = relayState == null ? "" : relayState;

        String escapedACS   = html(acsUrl);
        String escapedResp  = html(base64SAMLResponse);
        String escapedRelay = html(safeRelay);

        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta http-equiv=\"Cache-Control\" content=\"no-store\"/>" +
                "<title>SAML POST</title></head><body onload=\"document.forms[0].submit()\">" +
                "<form method=\"post\" action=\"" + escapedACS + "\">" +
                "<input type=\"hidden\" name=\"SAMLResponse\" value=\"" + escapedResp + "\"/>" +
                (safeRelay.isEmpty() ? "" :
                        "<input type=\"hidden\" name=\"RelayState\" value=\"" + escapedRelay + "\"/>") +
                "<noscript><button type=\"submit\">Continue</button></noscript>" +
                "</form></body></html>";
    }

    private static String html(final String s) {
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}

