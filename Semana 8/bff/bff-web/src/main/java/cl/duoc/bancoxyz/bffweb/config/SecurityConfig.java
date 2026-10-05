package cl.duoc.bancoxyz.bffweb.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**",
                                "/error"
                        ).permitAll()

                        .requestMatchers(
                                EndpointRequest.to("health", "info")
                        ).permitAll()

                        .requestMatchers("/api/web/**")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
                )

                .oauth2Login(oauth2 ->
                        oauth2.defaultSuccessUrl(
                                "/api/web/cuentas",
                                true
                        )
                );

        return http.build();
    }
}
