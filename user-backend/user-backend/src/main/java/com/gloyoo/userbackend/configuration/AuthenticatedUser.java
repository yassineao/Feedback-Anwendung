package com.gloyoo.userbackend.configuration;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String name,
        String role
) {
}
