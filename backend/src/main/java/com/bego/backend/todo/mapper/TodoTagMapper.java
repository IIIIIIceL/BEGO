package com.bego.backend.todo.mapper;

import com.bego.backend.todo.entity.TodoTagEntity;
import com.bego.backend.todo.vo.TodoTagResponse;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TodoTagMapper {

    @Insert("""
            INSERT INTO todo_tags (
                todo_id,
                tag_id,
                user_id
            ) VALUES (
                #{todoId},
                #{tagId},
                #{userId}
            )
            """)
    int insert(TodoTagEntity todoTag);

    @Select("""
            SELECT tag_id
            FROM todo_tags
            WHERE todo_id = #{todoId}
              AND user_id = #{userId}
            ORDER BY created_at ASC
            """)
    List<Long> findTagIdsByTodoIdAndUserId(@Param("todoId") Long todoId, @Param("userId") Long userId);

    @Select("""
            SELECT
                tg.id AS id,
                tg.name AS name,
                tg.color AS color
            FROM todo_tags tt
            JOIN tags tg
              ON tg.id = tt.tag_id
             AND tg.user_id = tt.user_id
             AND tg.deleted_at IS NULL
            WHERE tt.todo_id = #{todoId}
              AND tt.user_id = #{userId}
            ORDER BY tg.sort_order ASC, tg.created_at ASC
            """)
    List<TodoTagResponse> findTagResponsesByTodoIdAndUserId(
            @Param("todoId") Long todoId,
            @Param("userId") Long userId
    );

    @Delete("""
            DELETE FROM todo_tags
            WHERE todo_id = #{todoId}
              AND user_id = #{userId}
            """)
    int deleteByTodoIdAndUserId(@Param("todoId") Long todoId, @Param("userId") Long userId);

    @Delete("""
            DELETE FROM todo_tags
            WHERE tag_id = #{tagId}
              AND user_id = #{userId}
            """)
    int deleteByTagIdAndUserId(@Param("tagId") Long tagId, @Param("userId") Long userId);
}
