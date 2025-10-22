package com.waveterm.demo.terminal.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerminalCommandRepository extends JpaRepository<TerminalCommandEntity, Long> {

    @Query("""
            SELECT e FROM TerminalCommandEntity e
            WHERE e.sessionId = :sessionId
              AND (:keyword IS NULL OR LOWER(e.input) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:cursorTs IS NULL OR e.executedAt < :cursorTs
                   OR (e.executedAt = :cursorTs AND e.id < :cursorId))
            ORDER BY e.executedAt DESC, e.id DESC
            """)
    List<TerminalCommandEntity> findPage(@Param("sessionId") String sessionId,
                                         @Param("keyword") String keyword,
                                         @Param("cursorTs") Long cursorTs,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    @Modifying
    @Query("DELETE FROM TerminalCommandEntity e WHERE e.executedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") long cutoffMillis);

    @Modifying
    @Query(value = """
            DELETE FROM terminal_command_history
            WHERE id IN (
                SELECT id FROM terminal_command_history
                WHERE sessionId = :sessionId
                ORDER BY executedAt DESC, id DESC
                LIMIT -1 OFFSET :retain
            )
            """, nativeQuery = true)
    int trimSessionHistory(@Param("sessionId") String sessionId, @Param("retain") int retain);

    @Query("SELECT DISTINCT e.sessionId FROM TerminalCommandEntity e")
    List<String> findDistinctSessionIds();
}
