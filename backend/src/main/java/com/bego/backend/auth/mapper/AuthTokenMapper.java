package com.bego.backend.auth.mapper;

import com.bego.backend.auth.entity.AuthTokenEntity;
import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthTokenMapper {

    @Insert("""
            INSERT INTO auth_tokens (
                user_id,
                token_hash,
                token_type,
                parent_token_id,
                device_name,
                user_agent,
                ip_address,
                expires_at
            ) VALUES (
                #{userId},
                #{tokenHash},
                #{tokenType},
                #{parentTokenId},
                #{deviceName},
                #{userAgent},
                #{ipAddress},
                #{expiresAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuthTokenEntity token);

    @Select("""
            SELECT *
            FROM auth_tokens
            WHERE token_hash = #{tokenHash}
            """)
    AuthTokenEntity findByTokenHash(@Param("tokenHash") String tokenHash);

    @Select("""
            SELECT *
            FROM auth_tokens
            WHERE token_hash = #{tokenHash}
              AND token_type = 'REFRESH'
              AND revoked_at IS NULL
              AND expires_at > #{now}
            """)
    AuthTokenEntity findUsableRefreshTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Select("""
            SELECT *
            FROM auth_tokens
            WHERE token_hash = #{tokenHash}
              AND token_type = 'ACCESS'
              AND revoked_at IS NULL
              AND expires_at > #{now}
            """)
    AuthTokenEntity findUsableAccessTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Update("""
            UPDATE auth_tokens
            SET revoked_at = CURRENT_TIMESTAMP(3),
                last_used_at = #{lastUsedAt}
            WHERE id = #{id}
              AND revoked_at IS NULL
            """)
    int revokeById(@Param("id") Long id, @Param("lastUsedAt") Instant lastUsedAt);

    @Update("""
            UPDATE auth_tokens
            SET revoked_at = CURRENT_TIMESTAMP(3),
                last_used_at = #{lastUsedAt}
            WHERE id = #{id}
               OR parent_token_id = #{id}
            """)
    int revokeTokenFamilyByRefreshTokenId(@Param("id") Long id, @Param("lastUsedAt") Instant lastUsedAt);

    @Update("""
            UPDATE auth_tokens
            SET revoked_at = CURRENT_TIMESTAMP(3),
                last_used_at = #{lastUsedAt}
            WHERE id = #{id}
               OR parent_token_id = #{parentTokenId}
            """)
    int revokeAccessTokenAndParentFamily(
            @Param("id") Long id,
            @Param("parentTokenId") Long parentTokenId,
            @Param("lastUsedAt") Instant lastUsedAt
    );

    @Update("""
            UPDATE auth_tokens
            SET revoked_at = CURRENT_TIMESTAMP(3)
            WHERE user_id = #{userId}
              AND revoked_at IS NULL
            """)
    int revokeAllByUserId(@Param("userId") Long userId);
}
