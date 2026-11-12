package com.bakouan.app.config;


import com.bakouan.app.security.BaSecurityErrorHandler;
import com.bakouan.app.security.BaUserDetailsService;
import com.bakouan.app.security.jwt.JWTConfigurer;
import com.bakouan.app.security.jwt.RevokedTokenService;
import com.bakouan.app.security.jwt.TokenProvider;
import com.bakouan.app.utils.BaConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Profile("prod")
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfigForProd {

    private final BaUserDetailsService userDetailsService;
    private final TokenProvider tokenProvider;
    private final RevokedTokenService revokedTokenService;
    private final BaSecurityErrorHandler securityErrorHandler;

    /**
     * Configure security.
     *
     * @param http
     * @return chain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler));

        http.headers(h -> {
            h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
        });
        http.sessionManagement(session -> {
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        http.authorizeHttpRequests(auth -> {
            auth
                    .requestMatchers(HttpMethod.POST, BaConstants.URL.BASE_URL + BaConstants.URL.AUTHENTICATE).permitAll()
                    .requestMatchers(HttpMethod.POST, BaConstants.URL.BASE_URL + BaConstants.URL.LOGOUT).permitAll()
                    .requestMatchers(HttpMethod.PUT, BaConstants.URL.BASE_URL + BaConstants.URL.USER + "/request-reset-password",
                            BaConstants.URL.BASE_URL + BaConstants.URL.USER + "/complete-reset-password").permitAll()
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.CSRF_TOKEN).permitAll()
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.BASE_URL + BaConstants.URL.DOCUMENT + "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.BASE_URL + BaConstants.URL.PUBLIC + "/**").permitAll()
                    .anyRequest().authenticated();
        });

        http.with(securityConfigurerAdapter(), a -> {

        });


        return http.build();
    }

    /**
     * Build cors filter.
     *
     * @return CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        return new CorsFilter(source);
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider, userDetailsService, revokedTokenService, securityErrorHandler);
    }

    /**
     * RestTemplate for the profile dev.
     *
     * @param builder RestTemplateBuilder
     * @return restTemplate
     */
    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder builder) {
        return builder.build();
    }

}
