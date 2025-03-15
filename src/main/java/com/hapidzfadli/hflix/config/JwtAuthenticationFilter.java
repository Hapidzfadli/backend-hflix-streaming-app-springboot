package com.hapidzfadli.hflix.config;

import com.hapidzfadli.hflix.app.service.impl.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if(StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)){
                Optional<Authentication> authentication = tokenProvider.getAuthentication(jwt);

                authentication.ifPresent(auth -> {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Set Authentication to security context for '{}' with role {}",
                            auth.getName(), auth.getAuthorities());
                });
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeaderName());

        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix())){
            return bearerToken.substring(jwtProperties.getTokenPrefix().length());
        }

        return null;
    }
}
