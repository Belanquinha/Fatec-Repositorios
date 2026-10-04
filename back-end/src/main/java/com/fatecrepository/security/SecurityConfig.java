package com.fatecrepository.security;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Vem de {@code app.cors.allowed-origins} (ver application.yml). Antes a lista
     * vivia hardcoded aqui, o que obrigava a recompilar o Java para trocar a porta
     * do front — a mudança mais comum durante o desenvolvimento.
     *
     * <p>String separada por vírgula vira List&lt;String&gt; automaticamente.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configurando security filter chain com autenticação JWT");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .userDetailsService(customUserDetailsService)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/instituicoes/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/instituicoes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/instituicoes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/instituicoes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/uploads").authenticated()
                .requestMatchers(HttpMethod.GET, "/professores/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/projetos").authenticated()
                .requestMatchers(HttpMethod.GET, "/projetos/meus").authenticated()
                .requestMatchers(HttpMethod.GET, "/projetos/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/v3/api-docs").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Authentication failed for request: {}", request.getRequestURI());
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"codigo\": 401, \"mensagem\": \"Não autorizado\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("Access denied for request: {}", request.getRequestURI());
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"codigo\": 403, \"mensagem\": \"Acesso negado\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        log.info("Origens liberadas no CORS: {}", allowedOrigins);
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
