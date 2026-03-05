package com.freightfox.tollplaza.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class MapplsTokenProvider {

    private final RestClient restClient;

    @Value("${mappls.client-id}")
    private String clientId;

    @Value("${mappls.client-secret}")
    private String clientSecret;

    @Value("${mappls.token-url}")
    private String tokenUrl;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public MapplsTokenProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    public synchronized String getAccessToken() {
        if (Instant.now().isAfter(expiresAt.minusSeconds(300))) {
            refreshToken();
        }
        return accessToken;
    }

    private void refreshToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("MAPPLS_CLIENT_ID and MAPPLS_CLIENT_SECRET are required to compute routes");
        }

        String body = "grant_type=client_credentials"
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        Map<String, Object> response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.get("access_token") == null || response.get("expires_in") == null) {
            throw new IllegalStateException("Invalid response received from Mappls token API");
        }

        accessToken = (String) response.get("access_token");
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        expiresAt = Instant.now().plusSeconds(expiresIn);

        log.info("Mappls access token refreshed, expires in {} hours", expiresIn / 3600);
    }
}
