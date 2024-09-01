package in.neuw.oauth2.client.config.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class Oauth2ReactiveConfig implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(Oauth2ReactiveConfig.class);

    private static final String RESOURCE_ENDPOINT = "_resource_endpoint";
    private static final String OAUTH2_TOKEN_ENDPOINT = "_oauth2_token_endpoint";

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity httpSecurity) {
        return httpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    public WebClient webClientStandard(final @Value("${resource.base}") String resourceBase,
                                       final ReactiveClientRegistrationRepository clientRegistrationRepository,
                                       final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        var oauth2RegistrationId = "mock-standard";
        var tokenEndpointLogger = LoggerFactory.getLogger(oauth2RegistrationId+OAUTH2_TOKEN_ENDPOINT);
        WebClient tokenEndpointWebClient = WebClient.builder()
                .filter(logTokenRequest(tokenEndpointLogger, oauth2RegistrationId))
                .filter(logTokenResponse(tokenEndpointLogger, oauth2RegistrationId))
                .build();

        var accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
        accessTokenResponseClient.setWebClient(tokenEndpointWebClient);

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                .builder().clientCredentials(c -> {
                    c.accessTokenResponseClient(accessTokenResponseClient).build();
                }).build();
        return oauth2WebClient(
                oauth2RegistrationId,
                resourceBase,
                authorizedClientProvider,
                clientRegistrationRepository,
                authorizedClientRepository);
    }

    @Bean
    public WebClient webClientNotStandard(final @Value("${resource.base}") String resourceBase,
                                          final @Value("${custom.expiry}") long expirySeconds,
                                          final ReactiveClientRegistrationRepository clientRegistrationRepository,
                                          final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        var oauth2RegistrationId = "mock-not-standard";
        var tokenEndpointLogger = LoggerFactory.getLogger(oauth2RegistrationId+OAUTH2_TOKEN_ENDPOINT);
        WebClient tokenEndpointWebClient = WebClient.builder()
                .filter(logTokenRequest(tokenEndpointLogger, oauth2RegistrationId))
                .filter(logTokenResponse(tokenEndpointLogger, oauth2RegistrationId))
                .build();

        var accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
        accessTokenResponseClient.setBodyExtractor(new CustomOAuth2AccessTokenResponseBodyExtractor(expirySeconds));
        accessTokenResponseClient.setWebClient(tokenEndpointWebClient);

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                .builder().clientCredentials(c -> {
                    c.accessTokenResponseClient(accessTokenResponseClient).build();
                }).build();
        return oauth2WebClient(
                oauth2RegistrationId,
                resourceBase,
                authorizedClientProvider,
                clientRegistrationRepository,
                authorizedClientRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing Oauth2ReactiveConfig...");
    }

    private WebClient oauth2WebClient(final String oauth2RegistrationId,
                                      final String baseResourceUrl,
                                      final ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider,
                                      final ReactiveClientRegistrationRepository clientRegistrationRepository,
                                      final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        var resourceEndpointLogger = LoggerFactory.getLogger(oauth2RegistrationId+RESOURCE_ENDPOINT);

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        oauth.setDefaultClientRegistrationId(oauth2RegistrationId);

        return WebClient.builder()
                // base path of the client, this way we need to set the complete url again
                .baseUrl(baseResourceUrl)
                .filter(oauth)
                .filter(logResourceRequest(resourceEndpointLogger, oauth2RegistrationId))
                .filter(logResourceResponse(resourceEndpointLogger, oauth2RegistrationId))
                .build();
    }

    private static ExchangeFilterFunction logTokenRequest(final Logger logger, final String clientRegistrationName) {
        return ExchangeFilterFunction.ofRequestProcessor(c -> {
            logger.info("For Client-Registration {}, Sending OAUTH2 Token Request {}: {}", clientRegistrationName, c.method(), c.url());
            return Mono.just(c);
        });
    }

    private static ExchangeFilterFunction logTokenResponse(final Logger logger, final String clientRegistrationName) {
        return ExchangeFilterFunction.ofResponseProcessor(c -> {
            logger.info("For Client-Registration {}, OAUTH2 Token Response: {}", clientRegistrationName, c.statusCode());
            return Mono.just(c);
        });
    }

    private static ExchangeFilterFunction logResourceRequest(final Logger logger, final String clientName) {
        return ExchangeFilterFunction.ofRequestProcessor(c -> {
            logger.info("For Client {}, Sending OAUTH2 protected Resource Request to {}: {}", clientName, c.method(), c.url());
            return Mono.just(c);
        });
    }

    private static ExchangeFilterFunction logResourceResponse(final Logger logger, final String clientName) {
        return ExchangeFilterFunction.ofResponseProcessor(c -> {
            logger.info("For Client {}, OAUTH2 protected Resource Response status: {}", clientName, c.statusCode());
            return Mono.just(c);
        });
    }
}
