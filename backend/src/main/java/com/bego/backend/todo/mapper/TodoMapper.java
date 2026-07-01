package com.bego.backend.todo.mapper;

import com.bego.backend.todo.entity.TodoEntity;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TodoMapper {

    @Insert("""
            INSERT INTO todos (
                user_id,
                title,
                description,
                status,
                priority,
                client_temp_id,
                sync_status,
                last_synced_at,
                due_at,
                reminder_at,
                completed_at,
                sort_order
            ) VALUES (
                #{userId},
                #{title},
                #{description},
                #{status},
                #{priority},
                #{clientTempId},
                #{syncStatus},
                #{lastSyncedAt},
                #{dueAt},
                #{reminderAt},
                #{completedAt},
                #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TodoEntity todo);

    @Select("""
            SELECT *
            FROM todos
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    TodoEntity findActiveByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM todos
            WHERE user_id = #{userId}
              AND client_temp_id = #{clientTempId}
              AND deleted_at IS NULL
            """)
    TodoEntity findActiveByUserIdAndClientTempId(
            @Param("userId") Long userId,
            @Param("clientTempId") String clientTempId
    );

    @Select("""
            SELECT *
            FROM todos
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            ORDER BY
              CASE WHEN status = 'TODO' THEN 0 ELSE 1 END,
              CASE WHEN due_at IS NULL THEN 1 ELSE 0 END,
              due_at ASC,
              updated_at DESC
            LIMIT #{limit}
            OFFSET #{offset}
            """)
    List<TodoEntity> findActivePageByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT DISTINCT t.*
            FROM todos t
            <if test="tagId != null">
            JOIN todo_tags tt
              ON tt.todo_id = t.id
             AND tt.user_id = t.user_id
             AND tt.tag_id = #{tagId}
            </if>
            WHERE t.user_id = #{userId}
              AND t.deleted_at IS NULL
              <if test="status != null and status != ''">
                AND t.status = #{status}
              </if>
              <if test="priority != null and priority != ''">
                AND t.priority = #{priority}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (t.title LIKE #{keywordLike} OR t.description LIKE #{keywordLike})
              </if>
              <if test="dueFrom != null">
                AND t.due_at &gt;= #{dueFrom}
              </if>
              <if test="dueTo != null">
                AND t.due_at &lt;= #{dueTo}
              </if>
            <choose>
              <when test="sort == 'UPDATED_DESC'">
                ORDER BY t.updated_at DESC
              </when>
              <when test="sort == 'CREATED_DESC'">
                ORDER BY t.created_at DESC
              </when>
              <when test="sort == 'DUE_ASC'">
                ORDER BY CASE WHEN t.due_at IS NULL THEN 1 ELSE 0 END, t.due_at ASC, t.updated_at DESC
              </when>
              <otherwise>
                ORDER BY
                  CASE WHEN t.status = 'TODO' THEN 0 ELSE 1 END,
                  CASE WHEN t.due_at IS NULL THEN 1 ELSE 0 END,
                  t.due_at ASC,
                  t.updated_at DESC
              </otherwise>
            </choose>
            LIMIT #{limit}
            OFFSET #{offset}
            </script>
            """)
    List<TodoEntity> searchActivePage(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("tagId") Long tagId,
            @Param("keywordLike") String keywordLike,
            @Param("dueFrom") Instant dueFrom,
            @Param("dueTo") Instant dueTo,
            @Param("sort") String sort,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(DISTINCT t.id)
            FROM todos t
            <if test="tagId != null">
            JOIN todo_tags tt
              ON tt.todo_id = t.id
             AND tt.user_id = t.user_id
             AND tt.tag_id = #{tagId}
            </if>
            WHERE t.user_id = #{userId}
              AND t.deleted_at IS NULL
              <if test="status != null and status != ''">
                AND t.status = #{status}
              </if>
              <if test="priority != null and priority != ''">
                AND t.priority = #{priority}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (t.title LIKE #{keywordLike} OR t.description LIKE #{keywordLike})
              </if>
              <if test="dueFrom != null">
                AND t.due_at &gt;= #{dueFrom}
              </if>
              <if test="dueTo != null">
                AND t.due_at &lt;= #{dueTo}
              </if>
            </script>
            """)
    long countSearchActive(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("tagId") Long tagId,
            @Param("keywordLike") String keywordLike,
            @Param("dueFrom") Instant dueFrom,
            @Param("dueTo") Instant dueTo
    );

    @Update("""
            UPDATE todos
            SET title = #{title},
                description = #{description},
                priority = #{priority},
                due_at = #{dueAt},
                reminder_at = #{reminderAt},
                sort_order = #{sortOrder},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int update(TodoEntity todo);

    @Update("""
            UPDATE todos
            SET status = #{status},
                completed_at = #{completedAt},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int updateStatus(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("completedAt") Instant completedAt
    );

    @Update("""
            UPDATE todos
            SET deleted_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE todos
            SET deleted_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            """)
    int softDeleteAllByUserId(@Param("userId") Long userId);
}
