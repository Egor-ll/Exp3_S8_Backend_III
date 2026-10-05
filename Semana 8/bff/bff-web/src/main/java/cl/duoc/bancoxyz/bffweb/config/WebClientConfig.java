package cl.duoc.bancoxyz.bffweb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Builder integrado con Spring Cloud LoadBalancer.
     *
     * Permite resolver URLs lógicas como:
     * http://bank-backend
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Manager OAuth2 asociado a la sesión HTTP del BFF.
     *
     * Permite recuperar el cliente OAuth2 autorizado y su
     * access token desde la sesión del usuario autenticado.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository
                );

        manager.setAuthorizedClientProvider(authorizedClientProvider);

        return manager;
    }

    /**
     * WebClient utilizado por BankBackendClient.
     *
     * Mantiene LoadBalancer/Eureka y agrega automáticamente
     * el access token OAuth2 como Bearer Token.
     */
    @Bean
    public WebClient webClient(
            @LoadBalanced WebClient.Builder loadBalancedWebClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${backend.base-url}") String backendBaseUrl) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(
                        authorizedClientManager
                );

        oauth2.setDefaultOAuth2AuthorizedClient(true);
        oauth2.setDefaultClientRegistrationId("bancoxyz-client");

        return loadBalancedWebClientBuilder
                .clone()
                .baseUrl(backendBaseUrl)
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}