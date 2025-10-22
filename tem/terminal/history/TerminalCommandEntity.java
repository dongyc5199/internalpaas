package com.waveterm.demo.terminal.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "terminal_command_history", indexes = {
        @Index(name = "idx_history_session_ts", columnList = "sessionId, executedAt DESC"),
        @Index(name = "idx_history_executed_at", columnList = "executedAt"),
        @Index(name = "idx_history_exit_code", columnList = "exitCode")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_history_session_command", columnNames = {"sessionId", "commandId"})
})
public class TerminalCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String commandId;

    @Column(nullable = false, length = 4096)
    private String input;

    @Column(nullable = false)
    private long executedAt;

    @Column
    private Integer exitCode;

    protected TerminalCommandEntity() {
    }

    public TerminalCommandEntity(String sessionId, String commandId, String input, long executedAt) {
        this.sessionId = sessionId;
        this.commandId = commandId;
        this.input = input;
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getInput() {
        return input;
    }

    public long getExecutedAt() {
        return executedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }
}
