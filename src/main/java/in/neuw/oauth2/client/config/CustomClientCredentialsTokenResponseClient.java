package in.neuw.oauth2.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.client.RestOperations;

import java.time.Duration;
import java.time.Instant;

public class CustomClientCredentialsTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {

    private final Logger logger = LoggerFactory.getLogger(CustomClientCredentialsTokenResponseClient.class);
    private final DefaultClientCredentialsTokenResponseClient defaultClient;
    private final long expirationOverrideSeconds;

    public CustomClientCredentialsTokenResponseClient(long expirationOverrideSeconds, RestOperations restOperations) {
        this.defaultClient = new DefaultClientCredentialsTokenResponseClient();
        this.expirationOverrideSeconds = expirationOverrideSeconds;
        // inject the default template to the default client also.
        this.defaultClient.setRestOperations(restOperations);
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2ClientCredentialsGrantRequest grantRequest) {
        return getTokenResponse(grantRequest, this.expirationOverrideSeconds);
    }

    public OAuth2AccessTokenResponse getTokenResponse(OAuth2ClientCredentialsGrantRequest grantRequest,
                                                      long expirationOverrideSeconds) {
        OAuth2AccessTokenResponse originalResponse = defaultClient.getTokenResponse(grantRequest);
        Instant expiresAt = originalResponse.getAccessToken().getExpiresAt();

        long validity = Duration.between(originalResponse.getAccessToken().getIssuedAt(), expiresAt).toSeconds();
        logger.info("access Token had validity of '{}' second(s)", validity);

        // 1 second is when the expiry property is other than "expires_in", spring sets it internally!
        expirationOverrideSeconds = validity <= 1 ? expirationOverrideSeconds : validity;
        logger.info("access Token validity to be used finally is = '{}' seconds", expirationOverrideSeconds);

        return OAuth2AccessTokenResponse
                .withResponse(originalResponse)
                .expiresIn(expirationOverrideSeconds)
                .build();
    }
}