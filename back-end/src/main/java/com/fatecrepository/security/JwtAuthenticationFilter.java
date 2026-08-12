package com.fatecrepository.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        try {
            String jwt = getJwtFromRequest(request);
            if (jwt != null && jwtTokenProvider.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtTokenProvider.extractEmail(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Usuário autenticado via JWT: {}", email);
            } else if (jwt == null && isProtectedResource(requestUri)) {
                log.debug("Nenhum token fornecido para recurso protegido: {}", requestUri);
            }
        } catch (JWTVerificationException e) {
            log.warn("Erro ao verificar token JWT para request: {}", requestUri, e);
            SecurityContextHolder.clearContext();
        } catch (UsernameNotFoundException e) {
            log.warn("Token contém usuário inexistente para request: {}", requestUri);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isProtectedResource(String requestUri) {
        return !requestUri.startsWith("/auth") 
            && !requestUri.startsWith("/swagger-ui") 
            && !requestUri.startsWith("/v3/api-docs");
    }
}
