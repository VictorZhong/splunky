package com.wpb.spky.splunk;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SplunkUrlParser {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    public Optional<ParsedSplunkUrl> parseFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) return Optional.empty();
        var matcher = URL_PATTERN.matcher(rawText);
        if (!matcher.find()) return Optional.empty();

        String url = stripTrailingPunctuation(matcher.group());
        URI uri = URI.create(url);
        if (uri.getHost() == null || !uri.getHost().toLowerCase(Locale.ROOT).contains("splunk")) {
            return Optional.empty();
        }
        Map<String, String> params = new HashMap<>();
        params.putAll(parseParams(uri.getRawQuery()));
        params.putAll(parseFragmentParams(uri.getRawFragment()));

        String sid = firstNonBlank(params, "sid", "job", "search_id");
        String spl = firstNonBlank(params, "q", "query", "search");
        String earliest = firstNonBlank(params, "earliest", "earliest_time", "earliestTime");
        String latest = firstNonBlank(params, "latest", "latest_time", "latestTime");

        if (spl != null) spl = stripSearchPrefix(spl);
        if (isBlank(sid) && isBlank(spl)) {
            throw new IllegalArgumentException("Splunk URL must include a search query parameter such as q= or a sid= job id.");
        }
        return Optional.of(new ParsedSplunkUrl(url, blankToNull(sid), blankToNull(spl),
                blankToNull(earliest), blankToNull(latest)));
    }

    private static Map<String, String> parseFragmentParams(String rawFragment) {
        if (isBlank(rawFragment)) return Map.of();
        int queryStart = rawFragment.indexOf('?');
        if (queryStart < 0 || queryStart == rawFragment.length() - 1) return Map.of();
        return parseParams(rawFragment.substring(queryStart + 1));
    }

    private static Map<String, String> parseParams(String rawQuery) {
        if (isBlank(rawQuery)) return Map.of();
        Map<String, String> params = new HashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(decode(key).toLowerCase(Locale.ROOT), decode(value));
        }
        return params;
    }

    private static String firstNonBlank(Map<String, String> params, String... names) {
        for (String name : names) {
            String value = params.get(name.toLowerCase(Locale.ROOT));
            if (!isBlank(value)) return value;
        }
        return null;
    }

    private static String stripSearchPrefix(String spl) {
        String value = spl.trim();
        return value.regionMatches(true, 0, "search ", 0, "search ".length())
                ? value.substring("search ".length()).trim()
                : value;
    }

    private static String stripTrailingPunctuation(String url) {
        String value = url.trim();
        while (!value.isEmpty() && ".,);]}'\"".indexOf(value.charAt(value.length() - 1)) >= 0) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
