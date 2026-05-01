package com.wpb.spky.splunk;

public record ParsedSplunkUrl(
        String url,
        String sid,
        String spl,
        String earliest,
        String latest
) {
    public boolean hasSid() {
        return sid != null && !sid.isBlank();
    }

    public boolean hasSearch() {
        return spl != null && !spl.isBlank();
    }
}
