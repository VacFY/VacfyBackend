package com.dev.vacfy.iam.infrastructure.authorization.sfs.configuration;

import com.dev.vacfy.iam.infrastructure.authorization.sfs.pipeline.AuthorizationRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private final AuthenticationEntryPoint unauthorizedRequestHandlerEntryPoint;
    private final AuthorizationRequestFilter authorizationRequestFilter;

    public WebSecurityConfiguration(AuthenticationEntryPoint unauthorizedRequestHandlerEntryPoint, AuthorizationRequestFilter authorizationRequestFilter) {
        this.unauthorizedRequestHandlerEntryPoint = unauthorizedRequestHandlerEntryPoint;
        this.authorizationRequestFilter = authorizationRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var permittedRequestPatterns = new String[]{
                "/error",
                "/api/v1/authentication/sign-up",
                "/api/v1/authentication/sign-in",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/ws/device/**"
        };

        http.cors(configurer -> configurer.configurationSource(_ -> {
            var cors = new CorsConfiguration();
            cors.setAllowedOrigins(List.of("http://localhost:8000"));
            cors.setAllowedHeaders(List.of("*"));
            cors.setAllowedMethods(List.of("*"));
            cors.setAllowCredentials(true);
            cors.setMaxAge(3600L);
            return cors;
        }));

        http.csrf(AbstractHttpConfigurer::disable);

        http.exceptionHandling(configurer -> configurer.authenticationEntryPoint(unauthorizedRequestHandlerEntryPoint));

        http.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(configurer -> configurer.requestMatchers(permittedRequestPatterns).permitAll().anyRequest().authenticated());

        http.addFilterBefore(authorizationRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}