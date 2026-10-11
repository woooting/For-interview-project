package com.velrix.platform.infrastructure.security;

import com.velrix.platform.infrastructure.redis.TokenBlacklistStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistStore tokenBlacklistStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                if (!JwtService.TYP_ACCESS.equals(claims.get(JwtService.CLAIM_TYP, String.class))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String jti = claims.getId();
                if (StringUtils.hasText(jti) && tokenBlacklistStore.isBlacklisted(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                AuthUser authUser = new AuthUser(Long.valueOf(claims.getSubject()), claims.get("username", String.class));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authUser, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // token 无效：不设置认证，交给授权规则拒绝
            }
        }

        filterChain.doFilter(request, response);
    }
}
