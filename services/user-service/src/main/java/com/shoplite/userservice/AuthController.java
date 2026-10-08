package com.shoplite.userservice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public record RegisterRequest(@Email @NotBlank String email, @NotBlank String name, @Size(min = 6) String password) {}
    public record LoginRequest(String email, String password) {}
    public record AuthResponse(String token, String email, String name) {}

    private final UserRepository users;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Counter loginFailures;

    public AuthController(UserRepository users, JwtService jwt, MeterRegistry registry) {
        this.users = users;
        this.jwt = jwt;
        this.loginFailures = Counter.builder("auth.login.failures").description("Failed logins").register(registry);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (users.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User u = new User();
        u.setEmail(req.email());
        u.setName(req.name());
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        log.info("User registered id={} email={}", u.getId(), u.getEmail());
        return new AuthResponse(jwt.create(u), u.getEmail(), u.getName());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        User u = users.findByEmail(req.email() == null ? "" : req.email()).orElse(null);
        if (u == null || req.password() == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            loginFailures.increment();
            log.warn("Login failed for email={}", req.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        log.info("Login ok id={}", u.getId());
        return new AuthResponse(jwt.create(u), u.getEmail(), u.getName());
    }
}
