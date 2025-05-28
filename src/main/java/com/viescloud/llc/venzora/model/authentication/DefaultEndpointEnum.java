package com.viescloud.llc.venzora.model.authentication;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DefaultEndpointEnum {
    LOGGER_STATUS_HTML("^/api/v1/playbook/loggers/item/\\d+/status/html$"),
    LOGGER_STATUS_GIF("^/api/v1/playbook/loggers/item/\\d+/status/gif$"),
    H2_DB("^/h2-console$"),
    WEBHOOK_GITHUB("^/api/v1/webhooks/github$"),
    WEBHOOK_GITLAB("^/api/v1/webhooks/gitlab$"),
    FALVICON_ICON("^/favicon.ico$"),
    HEALTH_CHECK("^/healthCheck$"),
    STATUS_HEALTH("^/_status/healthz$"),
    LOGIN("^/api/v1/authenticators/login$"),
    LOGIN_OAUTH2("^/api/v1/authenticators/login/oauth2$"),
    WS("^/api/v1/ws$"),
    SWAGGER("^/$"),
    OPENID_PUBLIC("^/api/v1/open/id/providers/public$");

    private final String endpoint;

    public static final List<Pattern> patterns;

    static {
        patterns = Arrays.stream(values()).map(DefaultEndpointEnum::getEndpoint).map(Pattern::compile).toList();
    }

    public static boolean isDefaultEndpoint(String endpoint) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(endpoint);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
}
