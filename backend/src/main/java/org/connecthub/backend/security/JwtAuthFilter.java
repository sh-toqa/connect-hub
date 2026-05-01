package org.connecthub.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts incoming HTTP requests, extracts the JWT token from the Authorization header,
 * validates it, and sets the authentication in the SecurityContext if the token is valid.
 * This filter is executed once per request and ensures that only authenticated users can access protected endpoints.
 * It uses the JwtUtil to validate the token and extract user information, and UserDetailsService to load user details for authentication.
 * The filter checks for the presence of a Bearer token in the Authorization header, validates it, and if valid, sets the authentication context for the request.
 * This allows the application to secure endpoints and ensure that only authenticated users can access certain resources.
 * The filter is registered in the security configuration to be applied to incoming requests.
 */

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil            jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {
            
        // Extract token from Authorization header
        String token = extractBearerToken(request);
        // Validate token and set authentication if valid
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String      email       = jwtUtil.getEmailFromToken(token);
            // Load user details and set authentication in the security context
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            // Create an authentication token and set it in the security context
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            // Set additional details and update the security context
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    // Extracts the Bearer token from the Authorization header
    // Checks if the header is present and starts with "Bearer ", then returns the token part
    // If the header is not present or does not start with "Bearer ", returns null
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
