package com.bego.backend.tag.mapper;

import com.bego.backend.tag.entity.TagEntity;
import com.bego.backend.tag.vo.TagResponse;
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

    @Select("""
            SELECT
                tg.id AS id,
                tg.name AS name,
                tg.color AS color,
                tg.sort_order AS sort_order,
                COUNT(t.id) AS open_todo_count,
                tg.created_at AS created_at,
                tg.updated_at AS updated_at
            FROM tags tg
            LEFT JOIN todo_tags tt
                ON tt.tag_id = tg.id
               AND tt.user_id = tg.user_id
            LEFT JOIN todos t
                ON t.id = tt.todo_id
               AND t.user_id = tg.user_id
               AND t.deleted_at IS NULL
               AND t.status = 'TODO'
            WHERE tg.user_id = #{userId}
              AND tg.deleted_at IS NULL
            GROUP BY tg.id, tg.name, tg.color, tg.sort_order, tg.created_at, tg.updated_at
            ORDER BY tg.sort_order ASC, tg.created_at ASC
            """)
    List<TagResponse> findActiveResponsesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(t.id)
            FROM tags tg
            LEFT JOIN todo_tags tt
                ON tt.tag_id = tg.id
               AND tt.user_id = tg.user_id
            LEFT JOIN todos t
                ON t.id = tt.todo_id
               AND t.user_id = tg.user_id
               AND t.deleted_at IS NULL
               AND t.status = 'TODO'
            WHERE tg.id = #{id}
              AND tg.user_id = #{userId}
              AND tg.deleted_at IS NULL
            """)
    long countOpenTodosByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM tags
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
              AND id IN
              <foreach item="id" collection="ids" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    int countActiveByUserIdAndIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

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
            SET sort_order = #{sortOrder},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int updateSortOrder(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("sortOrder") Integer sortOrder
    );

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
