package org.connecthub.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
* Utility class for generating and validating JWT tokens.
* This class provides methods to create JWT tokens with user information (email) and to validate incoming tokens.
* It uses a secret key for signing the tokens and checks for token expiration.
* The generateToken method creates a JWT token with the user's email as the subject and an expiration time.
* The getEmailFromToken method extracts the email from a valid token.
* The validateToken method checks if the token is well-formed, signed with the correct key, and not expired.
* This class is used by the authentication service to generate tokens on login and by the JWT filter to validate tokens on protected requests.
* The secret key and token expiration time are injected from application properties, allowing for secure configuration.
*/

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long      expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        this.signingKey   = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    // Generates a JWT token with the user's email as the subject and an expiration time based on the configured properties
    public String generateToken(String email) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // Extracts the email (subject) from a valid JWT token by parsing the claims
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Validates the JWT token by parsing it and catching any exceptions that indicate invalidity (e.g., expired, malformed, unsupported)
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e)   { log.warn("JWT expired: {}",           e.getMessage()); }
        catch (UnsupportedJwtException e)  { log.warn("JWT unsupported: {}",       e.getMessage()); }
        catch (MalformedJwtException e)    { log.warn("JWT malformed: {}",         e.getMessage()); }
        catch (SecurityException e)        { log.warn("JWT bad signature: {}",     e.getMessage()); }
        catch (IllegalArgumentException e) { log.warn("JWT empty/null claims: {}", e.getMessage()); }
        return false;
    }

    // Parses the JWT token and returns the claims if the token is valid, otherwise throws an exception
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
