package com.gloyoo.userbackend.user.controller;

import com.gloyoo.userbackend.configuration.AuthenticatedUser;
import com.gloyoo.userbackend.configuration.JwtService;
import com.gloyoo.userbackend.user.dto.AuthRequest;
import com.gloyoo.userbackend.user.dto.UserRequest;
import com.gloyoo.userbackend.user.entity.User;
import com.gloyoo.userbackend.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Configuration
@RequestMapping ("/user")
public class UserController {
    private final UserService userService;
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private final JwtService jwt;
    UserController(UserService userService , JwtService jwt) {
        this.userService = userService;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest req) {
        try {
            User u = userService.registerUser(req);
            String access = jwt.generateToken(u.getEmail(),
                    Map.of("uid", u.getId().toString(), "role", u.getRole(), "user", u.getName()),
                    ACCESS_TTL);
            String refresh = jwt.generateToken(u.getEmail(),
                    Map.of("uid", u.getId().toString(), "role", u.getRole(), "user", u.getName(), "type", "refresh"),
                    REFRESH_TTL);


            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(authPayload(u, access, refresh));
        } catch (IllegalArgumentException ex) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
            pd.setTitle("Registration failed");
            pd.setProperty("exception", ex.getClass().getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {

        try {
            User u = userService.findByEmailOrThrow(req.email());

            if (!userService.checkPassword(u, req.password())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }

            String access = jwt.generateToken(u.getEmail(),
                    Map.of("uid", u.getId().toString(), "role", u.getRole(), "user", u.getName()),
                    ACCESS_TTL);
            String refresh = jwt.generateToken(u.getEmail(),
                    Map.of("uid", u.getId().toString(), "role", u.getRole(), "user", u.getName(), "type", "refresh"),
                    REFRESH_TTL);
            return ResponseEntity.ok(authPayload(u, access, refresh));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) TokenResponse body) {
        try {
            String refreshToken = body != null ? body.refreshToken() : null;
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing refresh token"));
            }

            var claims = jwt.parse(refreshToken).getBody();
            if (!"refresh".equals(claims.get("type"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token type"));
            }
            String email = claims.getSubject();
            String access = jwt.generateToken(email,
                    Map.of("uid", claims.get("uid"), "role", claims.get("role"), "user", claims.get("user")),
                    ACCESS_TTL);
            Map<String, Object> payload = authPayload(claims);
            payload.put("accessToken", access);
            payload.put("tokenType", "Bearer");
            return ResponseEntity.ok()
                    .body(payload);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid/expired token"));
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        return ResponseEntity.ok(authPayload(userService.findByIdOrThrow(authenticatedUser.id().toString())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(authPayload(userService.findByIdOrThrow(id.toString())));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok()
                .body(Map.of("message", "Logged out"));
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    private Map<String, Object> authPayload(User user, String accessToken, String refreshToken) {
        Map<String, Object> payload = authPayload(user);
        payload.put("accessToken", accessToken);
        payload.put("refreshToken", refreshToken);
        payload.put("tokenType", "Bearer");
        return payload;
    }

    private Map<String, Object> authPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("name", user.getName());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole());
        return payload;
    }

    private Map<String, Object> authPayload(Claims claims) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", claims.get("uid"));
        payload.put("name", claims.get("user"));
        payload.put("email", claims.getSubject());
        payload.put("role", claims.get("role"));
        return payload;
    }

    private record TokenResponse(String refreshToken) {
    }

}
