package com.scaffold.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final AccountStateChecker accountStateChecker;

    public JwtAuthenticationFilter(JwtService jwtService, TokenStore tokenStore, AccountStateChecker accountStateChecker) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.accountStateChecker = accountStateChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String username = jwtService.parseUsername(token);
            boolean activeToken = username != null && tokenStore.usernameOf(token)
                    .map(username::equals)
                    .orElse(false);
            AccountStateChecker.State state = activeToken ? accountStateChecker.check(username) : null;
            if (state != null && !state.enabled()) {
                tokenStore.revoke(token);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"账号已停用或登录已失效\"}");
                return;
            }
            String path = request.getRequestURI();
            boolean passwordEndpoint = path.equals("/api/auth/password") || path.equals("/api/auth/logout") || path.equals("/api/auth/me");
            if (state != null && state.mustChangePassword() && !passwordEndpoint) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"请先修改初始密码\"}");
                return;
            }
            if (activeToken && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, token, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
