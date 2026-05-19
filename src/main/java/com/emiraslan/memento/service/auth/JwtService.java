package com.emiraslan.memento.service.auth;

import com.emiraslan.memento.entity.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    // Access Token methods

    // Generating a 15-minute JWT for users to use the api with
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {

        // casting userdetails to our User entity to reach getUserId method
        User user = (User) userDetails;

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(String.valueOf(user.getUserId())) // we put the user's id as the subject of JWT
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 min short-lived JWT access token
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token methods

    // Generating a 14-day refresh JWT for users to renew their Access JWTs. Refresh JWTs are rotated whenever a new Access JWT is requested.
    public String generateRefreshJwt(User user, Integer deviceId, String jti) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .setId(jti) // JWT id
                .claim("deviceId", deviceId) // we include the device's id in the payload, along with JTI
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 14)) // 14 Days
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateRefreshJwt(String token) {
        try {
            // parsing automatically checks for expiration and signature
            Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new IllegalStateException("REFRESH_TOKEN_EXPIRED");
        } catch (Exception e) {
            throw new IllegalStateException("INVALID_REFRESH_TOKEN_SIGNATURE");
        }
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Integer extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", Integer.class));
    }

    // Mutual/helper methods

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String extractedId = extractUserId(token);
        User user = (User) userDetails;
        return (extractedId.equals(String.valueOf(user.getUserId()))) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public long getExpirationTime(String token) {
        return extractExpiration(token).getTime();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}