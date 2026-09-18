package org.connecthub.backend.security;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Security configuration for the ConnectHub application.
 * This class sets up JWT-based authentication, CORS configuration, and password encoding.
 * It defines the security filter chain, including the JWT authentication filter, and configures which endpoints are publicly accessible.
 * The class also provides a UserDetailsService bean to load user details from the database and a PasswordEncoder bean for hashing passwords.
 * CORS is configured to allow requests only from the specified React frontend origin, enhancing security by preventing unauthorized cross-origin requests.
 * The session management is set to stateless to ensure that the application does not maintain server-side sessions, relying solely on JWT tokens for authentication.
 * This configuration ensures that only authenticated users can access protected resources while allowing public access to registration and login endpoints.
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter  jwtAuthFilter;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    // Define the security filter chain, configuring HTTP security settings, CORS, session management, and endpoint authorization
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        // Static file serving for uploaded images
                        .requestMatchers("/uploads/**").permitAll()
                        // Actuator health check
                        .requestMatchers("/actuator/health").permitAll()
                        // Protected endpoints
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/content/**").authenticated()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // add JWT filter before the default username/password filter

        return http.build();
    }

    // Bean for password encoding using BCrypt with a strength of 10
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }



    // CORS configuration to allow requests from the React frontend and specify allowed methods and headers
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin)); // define allowed origin in application.properties
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // allow all standard HTTP methods
        config.setAllowedHeaders(List.of("*")); // allow all headers (content-type, authorization, etc.)
        config.setAllowCredentials(true); // allow cookies and auth headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // apply this CORS config to all endpoints
        return source;
    }
}
