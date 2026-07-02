package com.bego.backend.user.service;

import com.bego.backend.user.entity.UserEntity;

public interface UserService {
    UserEntity getActiveUser(Long userId);

    void deleteMe(Long userId, String password);
}
