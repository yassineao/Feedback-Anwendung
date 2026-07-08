package com.gloyoo.backend.user.client;

import com.gloyoo.backend.user.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/user/{id}")
    UserResponseDto getUserById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authorization
    );
}
