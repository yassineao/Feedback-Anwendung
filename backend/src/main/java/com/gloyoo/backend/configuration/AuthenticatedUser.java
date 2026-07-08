package com.gloyoo.backend.configuration;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String name,
        String role
) {
}
