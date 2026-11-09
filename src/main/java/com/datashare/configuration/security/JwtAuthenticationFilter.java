package com.datashare.configuration.security;

import com.datashare.model.ApiError;
import com.datashare.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Filtre JWT explicite, comme dans le projet 2 ; conserve les erreurs JSON du contrat DataShare. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final CustomUserDetailService users;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwt, CustomUserDetailService users, ObjectMapper mapper) {
        this.jwt = jwt;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // L'inscription et la connexion ne doivent pas dependre d'un ancien JWT du client.
        return "/auth/register".equals(path) || "/auth/login".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                && !authorization.equalsIgnoreCase("Bearer"))) {
            chain.doFilter(request, response);
            return;
        }
        String token = authorization.length() > 6 ? authorization.substring(7).trim() : "";
        try {
            String email = jwt.extractEmail(token);
            var user = users.loadUserByUsername(email);
            new AccountStatusUserDetailsChecker().check(user);
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ApiError().code(401)
                    .message("Token invalide, expire ou compte non autorise.")
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC)));
            return;
        }
        chain.doFilter(request, response);
    }
}
