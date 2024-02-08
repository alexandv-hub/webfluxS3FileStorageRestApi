package com.example.webfluxS3FileStorageRestApi.security;

import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Mono<Boolean> isAdminOrModerator(Mono<Authentication> authMono) {
        return authMono.map(authentication -> authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals("ROLE_" + UserRole.ADMIN.name()) ||
                        grantedAuthority.getAuthority().equals("ROLE_" + UserRole.MODERATOR.name())));
    }
}
