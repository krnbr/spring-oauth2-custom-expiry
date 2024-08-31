package in.neuw.oauth2.client.config.reactive;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

class CustomOAuth2AccessTokenResponseBodyExtractor
		implements BodyExtractor<Mono<OAuth2AccessTokenResponse>, ReactiveHttpInputMessage> {

	private final Logger logger = LoggerFactory.getLogger(CustomOAuth2AccessTokenResponseBodyExtractor.class);

	private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

	private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {};

	private final long expiresIn;

	CustomOAuth2AccessTokenResponseBodyExtractor(long expiresIn) {
		this.expiresIn = expiresIn;
	}

	@Override
	public Mono<OAuth2AccessTokenResponse> extract(ReactiveHttpInputMessage inputMessage, Context context) {
		BodyExtractor<Mono<Map<String, Object>>, ReactiveHttpInputMessage> delegate = BodyExtractors
			.toMono(STRING_OBJECT_MAP);
		return delegate.extract(inputMessage, context)
			.onErrorMap((ex) -> new OAuth2AuthorizationException(
					invalidTokenResponse("An error occurred parsing the Access Token response: " + ex.getMessage()),
					ex))
			.switchIfEmpty(Mono.error(() -> new OAuth2AuthorizationException(
					invalidTokenResponse("Empty OAuth 2.0 Access Token Response"))))
			.map(this::parse)
			.flatMap(this::oauth2AccessTokenResponse)
			.map(this::oauth2AccessTokenResponse);
	}

	private TokenResponse parse(Map<String, Object> json) {
		try {
			return TokenResponse.parse(new JSONObject(json));
		}
		catch (ParseException ex) {
			OAuth2Error oauth2Error = invalidTokenResponse(
					"An error occurred parsing the Access Token response: " + ex.getMessage());
			throw new OAuth2AuthorizationException(oauth2Error, ex);
		}
	}

	private OAuth2Error invalidTokenResponse(String message) {
		return new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE, message, null);
	}

	private Mono<AccessTokenResponse> oauth2AccessTokenResponse(TokenResponse tokenResponse) {
		if (tokenResponse.indicatesSuccess()) {
			return Mono.just(tokenResponse).cast(AccessTokenResponse.class);
		}
		TokenErrorResponse tokenErrorResponse = (TokenErrorResponse) tokenResponse;
		ErrorObject errorObject = tokenErrorResponse.getErrorObject();
		OAuth2Error oauth2Error = getOAuth2Error(errorObject);
		return Mono.error(new OAuth2AuthorizationException(oauth2Error));
	}

	private OAuth2Error getOAuth2Error(ErrorObject errorObject) {
		if (errorObject == null) {
			return new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
		}
		String code = (errorObject.getCode() != null) ? errorObject.getCode() : OAuth2ErrorCodes.SERVER_ERROR;
		String description = errorObject.getDescription();
		String uri = (errorObject.getURI() != null) ? errorObject.getURI().toString() : null;
		return new OAuth2Error(code, description, uri);
	}

	private OAuth2AccessTokenResponse oauth2AccessTokenResponse(AccessTokenResponse accessTokenResponse) {
		AccessToken accessToken = accessTokenResponse.getTokens().getAccessToken();
		OAuth2AccessToken.TokenType accessTokenType = null;
		if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(accessToken.getType().getValue())) {
			accessTokenType = OAuth2AccessToken.TokenType.BEARER;
		}
		long expiresIn = accessToken.getLifetime();
		Set<String> scopes = (accessToken.getScope() != null)
				? new LinkedHashSet<>(accessToken.getScope().toStringList()) : Collections.emptySet();
		String refreshToken = null;
		if (accessTokenResponse.getTokens().getRefreshToken() != null) {
			refreshToken = accessTokenResponse.getTokens().getRefreshToken().getValue();
		}
		Map<String, Object> additionalParameters = new LinkedHashMap<>(accessTokenResponse.getCustomParameters());

		var oauth2AccessTokenResponseBuilder = OAuth2AccessTokenResponse.withToken(accessToken.getValue())
				.tokenType(accessTokenType)
				.scopes(scopes)
				.refreshToken(refreshToken)
				.additionalParameters(additionalParameters);

		// this is one example of the expiry, one may override other attributes accordingly. :-)
		if (expiresIn <= 0) {
			logger.info("the expiry of the access token originally was -> {}", expiresIn);
			oauth2AccessTokenResponseBuilder.expiresIn(this.expiresIn);
			logger.info(
					"the expiry of the access token after it was overridden is -> {}",
					Duration.between(Instant.now(), Instant.now().plusSeconds(this.expiresIn)).toSeconds()
			);
		}

		var oauth2AccessTokenResponse = oauth2AccessTokenResponseBuilder.build();

		return oauth2AccessTokenResponse;
	}

}