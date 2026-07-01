package com.bego.backend.user.controller;

import com.bego.backend.common.security.CurrentUserContext;
import com.bego.backend.user.dto.DeleteMeRequest;
import com.bego.backend.user.service.UserService;
import com.bego.backend.user.vo.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse me() {
        return UserProfileResponse.from(userService.getActiveUser(CurrentUserContext.requireUserId()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe(@Valid @RequestBody DeleteMeRequest request) {
        userService.deleteMe(CurrentUserContext.requireUserId(), request.password());
        return ResponseEntity.noContent().build();
    }
}
