package com.bego.backend.user.mapper;

import com.bego.backend.user.entity.UserEntity;
import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (
                email,
                normalized_email,
                password_hash,
                display_name,
                avatar_url,
                status
            ) VALUES (
                #{email},
                #{normalizedEmail},
                #{passwordHash},
                #{displayName},
                #{avatarUrl},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity user);

    @Select("""
            SELECT *
            FROM users
            WHERE id = #{id}
              AND deleted_at IS NULL
            """)
    UserEntity findActiveById(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM users
            WHERE normalized_email = #{normalizedEmail}
              AND deleted_at IS NULL
            """)
    UserEntity findActiveByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);

    @Update("""
            UPDATE users
            SET last_login_at = #{lastLoginAt},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND deleted_at IS NULL
            """)
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") Instant lastLoginAt);

    @Update("""
            UPDATE users
            SET email = #{email},
                normalized_email = #{normalizedEmail},
                display_name = #{displayName},
                avatar_url = NULL,
                status = 'DELETED',
                deletion_requested_at = CURRENT_TIMESTAMP(3),
                anonymized_at = CURRENT_TIMESTAMP(3),
                deleted_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND deleted_at IS NULL
            """)
    int anonymizeAndSoftDelete(
            @Param("id") Long id,
            @Param("email") String email,
            @Param("normalizedEmail") String normalizedEmail,
            @Param("displayName") String displayName
    );
}
