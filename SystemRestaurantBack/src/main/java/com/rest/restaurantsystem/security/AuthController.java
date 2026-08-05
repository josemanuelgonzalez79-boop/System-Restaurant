package com.rest.restaurantsystem.security;

import com.rest.restaurantsystem.user.InitialSetupRequest;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @GetMapping("/setup-status")
    public SetupStatusResponse setupStatus() {
        return new SetupStatusResponse(userService.setupRequired());
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse setup(@Valid @RequestBody InitialSetupRequest request) {
        return userService.createInitialOwner(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.currentUser(authentication.getName());
    }
}
