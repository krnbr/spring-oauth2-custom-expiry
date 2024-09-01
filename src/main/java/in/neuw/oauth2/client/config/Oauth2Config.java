package in.neuw.oauth2.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class Oauth2Config implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(Oauth2Config.class);

    private static final String RESOURCE_ENDPOINT = "_resource_endpoint";
    private static final String OAUTH2_TOKEN_ENDPOINT = "_oauth2_token_endpoint";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf((csrf) -> csrf.disable())
                .build();
    }

    @Bean
    public WebClient webClientStandard(final @Value("${resource.base}") String resourceBase,
                                       final ClientRegistrationRepository clientRegistrationRepository,
                                       final OAuth2AuthorizedClientRepository authorizedClientRepository) {
        final String oauth2RegistrationId = "mock-standard";
        var defaultClientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
        defaultClientCredentialsTokenResponseClient.setRestOperations(getRestTemplateForTokenEndPoint(oauth2RegistrationId));
        return getOauth2WebClient(
            resourceBase,
            oauth2RegistrationId,
            clientRegistrationRepository,
            authorizedClientRepository,
            defaultClientCredentialsTokenResponseClient
        );
    }

    @Bean
    public WebClient webClientNotStandard(final @Value("${resource.base}") String resourceBase,
                                          final @Value("${custom.expiry}") long expirySeconds,
                                          final ClientRegistrationRepository clientRegistrationRepository,
                                          final OAuth2AuthorizedClientRepository authorizedClientRepository) {

        var oauth2RegistrationId = "mock-not-standard";
        var defaultClientCredentialsTokenResponseClient = new CustomClientCredentialsTokenResponseClient(
                expirySeconds,
                getRestTemplateForTokenEndPoint(oauth2RegistrationId)
        );

        return getOauth2WebClient(
            resourceBase,
            oauth2RegistrationId,
            clientRegistrationRepository,
            authorizedClientRepository,
            defaultClientCredentialsTokenResponseClient
        );
    }

    private WebClient getOauth2WebClient(final String resourceBase,
                                         final String oauth2RegistrationId,
                                         final ClientRegistrationRepository clientRegistrationRepository,
                                         final OAuth2AuthorizedClientRepository authorizedClientRepository,
                                         final OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> oauth2TokenResponseClient) {
        var resourceEndpointLogger = LoggerFactory.getLogger(oauth2RegistrationId+RESOURCE_ENDPOINT);

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(c -> c.accessTokenResponseClient(oauth2TokenResponseClient))
                .build();

        var oauth2AuthorizedClientManager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        oauth2AuthorizedClientManager.setAuthorizedClientProvider(provider);

        var oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oauth2AuthorizedClientManager);
        oauth.setDefaultClientRegistrationId(oauth2RegistrationId);

        return WebClient.builder()
                // base path of the client, just path while calling is required
                .baseUrl(resourceBase)
                .apply(oauth.oauth2Configuration())
                .filter(logResourceRequest(resourceEndpointLogger, oauth2RegistrationId))
                .filter(logResourceResponse(resourceEndpointLogger, oauth2RegistrationId))
                .build();
    }

    private static ExchangeFilterFunction logResourceRequest(final Logger logger, final String clientName) {
        return ExchangeFilterFunction.ofRequestProcessor(c -> {
            logger.info(
                    "For Client {}, Sending OAUTH2 protected Resource Request to {}: {}",
                    clientName, c.method(), c.url()
            );
            return Mono.just(c);
        });
    }

    private RestTemplate getRestTemplateForTokenEndPoint(String oauth2RegistrationId) {
        var tokenEndpointLogger = tokenEndpointLogger(oauth2RegistrationId);
        var restTemplateForTokenEndPoint = new RestTemplate();
        restTemplateForTokenEndPoint
                .setMessageConverters(
                        List.of(new FormHttpMessageConverter(),
                                new OAuth2AccessTokenResponseHttpMessageConverter()
                        ));
        restTemplateForTokenEndPoint
                .setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        restTemplateForTokenEndPoint
                .setInterceptors(List.of(restTemplateRequestInterceptor(tokenEndpointLogger, oauth2RegistrationId)));
        return restTemplateForTokenEndPoint;
    }

    private Logger tokenEndpointLogger(final String oauth2RegistrationId) {
        return LoggerFactory.getLogger(oauth2RegistrationId+OAUTH2_TOKEN_ENDPOINT);
    }

    private static ExchangeFilterFunction logResourceResponse(final Logger logger, final String clientName) {
        return ExchangeFilterFunction.ofResponseProcessor(c -> {
            logger.info("For Client {}, OAUTH2 protected Resource Response status: {}", clientName, c.statusCode());
            return Mono.just(c);
        });
    }

    private static ClientHttpRequestInterceptor restTemplateRequestInterceptor(final Logger logger, final String clientName) {
        return (request, body, execution) -> {
            logger.info("For Client {}, Sending OAUTH2 Token Request to {}", clientName, request.getURI());
            var clientResponse = execution.execute(request, body);
            logger.info("For Client {}, OAUTH2 Token Response: {}", clientName, clientResponse.getStatusCode());
            return clientResponse;
        };
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing Oauth2Config...");
    }
}
