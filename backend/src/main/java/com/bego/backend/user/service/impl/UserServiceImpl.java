package com.bego.backend.user.service.impl;

import com.bego.backend.auth.mapper.AuthTokenMapper;
import com.bego.backend.common.error.ApiException;
import com.bego.backend.common.error.ErrorCode;
import com.bego.backend.tag.mapper.TagMapper;
import com.bego.backend.todo.mapper.TodoMapper;
import com.bego.backend.user.entity.UserEntity;
import com.bego.backend.user.mapper.UserMapper;
import com.bego.backend.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final TagMapper tagMapper;
    private final AuthTokenMapper authTokenMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserMapper userMapper,
            TodoMapper todoMapper,
            TagMapper tagMapper,
            AuthTokenMapper authTokenMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.todoMapper = todoMapper;
        this.tagMapper = tagMapper;
        this.authTokenMapper = authTokenMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity getActiveUser(Long userId) {
        UserEntity user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new ApiException(ErrorCode.ACCOUNT_DELETED);
        }
        return user;
    }

    @Override
    @Transactional
    public void deleteMe(Long userId, String password) {
        UserEntity user = getActiveUser(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid password");
        }

        String deletedEmail = "deleted+" + userId + "@deleted.local";
        userMapper.anonymizeAndSoftDelete(userId, deletedEmail, deletedEmail, "已注销用户");
        todoMapper.softDeleteAllByUserId(userId);
        tagMapper.softDeleteAllByUserId(userId);
        authTokenMapper.revokeAllByUserId(userId);
    }
}
