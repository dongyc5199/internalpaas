package com.waveterm.demo.terminal.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class TerminalHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TerminalHistoryService.class);

    private final TerminalCommandRepository repository;
    private final TerminalHistoryProperties properties;

    public TerminalHistoryService(TerminalCommandRepository repository,
                                  TerminalHistoryProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public TerminalCommandEntity append(String sessionId, String commandId, String input, long executedAt) {
        TerminalCommandEntity entity = new TerminalCommandEntity(sessionId, commandId, input, executedAt);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public HistoryPage list(String sessionId, HistoryQuery query) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(query, "query");

        int limit = Math.min(Math.max(query.limit(), 1), 200);
        Cursor cursor = Cursor.parse(query.cursor());
        String keyword = StringUtils.hasText(query.keyword()) ? query.keyword().trim() : null;

        List<TerminalCommandEntity> entities = repository.findPage(
                sessionId,
                keyword,
                cursor == null ? null : cursor.executedAt(),
                cursor == null ? null : cursor.id(),
                PageRequest.of(0, limit + 1)
        );

        String nextCursor = null;
        if (entities.size() > limit) {
            TerminalCommandEntity last = entities.remove(entities.size() - 1);
            nextCursor = Cursor.of(last).asString();
        }
        return new HistoryPage(entities, nextCursor);
    }

    @Scheduled(initialDelayString = "${terminal.history.cleanup-interval:PT30M}",
            fixedDelayString = "${terminal.history.cleanup-interval:PT30M}")
    @Transactional
    public void cleanupHistoricalCommands() {
        if (properties.getRetentionTtl().isZero() || properties.getRetentionTtl().isNegative()) {
            return;
        }
        long cutoff = Instant.now().toEpochMilli() - properties.getRetentionTtl().toMillis();
        int removed = repository.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.debug("Removed {} expired terminal commands (cutoff={})", removed, cutoff);
        }
        int retain = properties.getRetentionPerSession();
        if (retain <= 0) {
            return;
        }
        List<String> sessionIds = repository.findDistinctSessionIds();
        for (String sessionId : sessionIds) {
            if (sessionId == null) {
                continue;
            }
            int trimmed = repository.trimSessionHistory(sessionId, retain);
            if (trimmed > 0) {
                log.debug("Trimmed {} terminal commands for session {}", trimmed, sessionId);
            }
        }
    }

    public record HistoryQuery(int limit, String cursor, String keyword) {
        public HistoryQuery {
            if (limit <= 0) {
                limit = 50;
            }
        }
    }

    public record HistoryPage(List<TerminalCommandEntity> items, String nextCursor) {
    }

    private record Cursor(long executedAt, long id) {
        static Cursor parse(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String[] parts = value.split(":");
            if (parts.length != 2) {
                return null;
            }
            try {
                long ts = Long.parseLong(parts[0]);
                long id = Long.parseLong(parts[1]);
                return new Cursor(ts, id);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        static Cursor of(TerminalCommandEntity entity) {
            return new Cursor(entity.getExecutedAt(), entity.getId() == null ? 0L : entity.getId());
        }

        String asString() {
            return executedAt + ":" + id;
        }
    }
}
