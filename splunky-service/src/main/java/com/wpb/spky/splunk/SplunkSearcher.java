package com.wpb.spky.splunk;

import com.wpb.spky.session.UserSession;

public interface SplunkSearcher {

    SplunkSearchResult search(UserSession session, SplunkSearchRequest request);

    SplunkSearchResult resultsForSid(UserSession session, String sid, String splunkUrl);
}
