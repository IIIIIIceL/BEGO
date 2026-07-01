package com.bego.backend.tag.mapper;

import com.bego.backend.tag.entity.TagEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TagMapper {

    @Insert("""
            INSERT INTO tags (
                user_id,
                name,
                normalized_name,
                color,
                sort_order,
                active_key
            ) VALUES (
                #{userId},
                #{name},
                #{normalizedName},
                #{color},
                #{sortOrder},
                #{activeKey}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TagEntity tag);

    @Select("""
            SELECT *
            FROM tags
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    TagEntity findActiveByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM tags
            WHERE user_id = #{userId}
              AND normalized_name = #{normalizedName}
              AND active_key = 0
              AND deleted_at IS NULL
            """)
    TagEntity findActiveByUserIdAndNormalizedName(
            @Param("userId") Long userId,
            @Param("normalizedName") String normalizedName
    );

    @Select("""
            SELECT *
            FROM tags
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            ORDER BY sort_order ASC, created_at ASC
            """)
    List<TagEntity> findActiveByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE tags
            SET name = #{name},
                normalized_name = #{normalizedName},
                color = #{color},
                sort_order = #{sortOrder},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int update(TagEntity tag);

    @Update("""
            UPDATE tags
            SET deleted_at = CURRENT_TIMESTAMP(3),
                active_key = id,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE tags
            SET deleted_at = CURRENT_TIMESTAMP(3),
                active_key = id,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int softDeleteAllByUserId(@Param("userId") Long userId);
}
