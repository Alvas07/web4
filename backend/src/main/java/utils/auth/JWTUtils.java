package utils.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JWTUtils {

    public static final long ACCESS_EXP_MS = 1000 * 60 * 15;
    public static final long REFRESH_EXP_MS = 1000L * 60 * 60 * 24 * 7;
    private static final Key KEY;

    static {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET is not set or too short (min 32 bytes)");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    public static String generateAccessToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ACCESS_EXP_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateRefreshToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + REFRESH_EXP_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public static boolean isRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token);
            String tokenType = claims.getBody().get(TOKEN_TYPE_CLAIM, String.class);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token);
            return claims.getBody().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}