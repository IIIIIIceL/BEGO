package com.bego.backend.todo.mapper;

import com.bego.backend.todo.entity.TodoTagEntity;
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
