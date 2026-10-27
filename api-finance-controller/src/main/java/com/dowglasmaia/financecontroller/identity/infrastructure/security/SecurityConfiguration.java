package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.shared.error.ProblemResponseWriter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
class SecurityConfiguration {

    private static final String AUTH_PATTERN = "/api/v1/auth/**";
    private static final int BCRYPT_STRENGTH = 12;
    private static final long HSTS_SECONDS = 31_536_000L;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ProblemResponseWriter problems,
            IdentityProperties properties) {
        var handlers = new ProblemJsonSecurityHandlers(problems);
        http
            // CSRF desligado: a API é sem estado e autentica por cabeçalho Authorization (bearer), que o navegador
            // não anexa sozinho. O cookie de renovação é SameSite e só vale nas rotas /auth (CORS com origem fixa).
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                // O Spring só emite HSTS em requisições HTTPS, logo não aparece em desenvolvimento local (HTTP).
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_SECONDS)))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                    "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resource -> resource
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(handlers)
                .accessDeniedHandler(handlers))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(handlers)
                .accessDeniedHandler(handlers))
            .addFilterBefore(new RequestBodySizeLimitFilter(properties.maxRequestBody().toBytes(), problems),
                CorsFilter.class);
        return http.build();
    }

    /** Origens explícitas; credenciais (cookie de renovação) só nas rotas /auth. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(IdentityProperties properties) {
        List<String> origins = properties.cors().allowedOrigins();
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(AUTH_PATTERN, cors(origins, true));
        source.registerCorsConfiguration("/**", cors(origins, false));
        return source;
    }

    private static CorsConfiguration cors(List<String> origins, boolean credentials) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(credentials);
        configuration.setMaxAge(3600L);
        return configuration;
    }
}
