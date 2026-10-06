package com.example.auth;

import com.example.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private final ObjectMapper mapper;
    private final byte[] secret;
    private final long tokenSeconds;

    public JwtService(ObjectMapper mapper, @Value("${auth.jwt-secret}") String secret,
                      @Value("${auth.token-hours:8}") long tokenHours) {
        this.mapper = mapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenSeconds = tokenHours * 3600;
    }

    public String issue(User user) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getUsername());
            payload.put("uid", user.getUserId());
            payload.put("role", user.getRole());
            payload.put("sid", user.getStudentId());
            payload.put("mid", user.getMentorId());
            payload.put("iat", now);
            payload.put("exp", now + tokenSeconds);
            String content = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT"))) + "."
                    + encode(mapper.writeValueAsBytes(payload));
            return content + "." + encode(sign(content));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成登录令牌", e);
        }
    }

    public AuthUser verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("令牌格式错误");
            String content = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(sign(content), Base64.getUrlDecoder().decode(parts[2]))) {
                throw new IllegalArgumentException("令牌签名无效");
            }
            Map<String, Object> payload = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {});
            if (((Number) payload.get("exp")).longValue() < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("登录已过期");
            }
            return new AuthUser(number(payload.get("uid")), (String) payload.get("sub"),
                    (String) payload.get("role"), number(payload.get("sid")), number(payload.get("mid")));
        } catch (Exception e) {
            throw new IllegalArgumentException("登录令牌无效或已过期", e);
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
    private String encode(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private Integer number(Object value) { return value == null ? null : ((Number) value).intValue(); }
}
