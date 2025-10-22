package com.waveterm.demo.terminal.completion;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CompletionRequest(@NotBlank String prompt,
                                String cwd,
                                List<String> recentCommands,
                                String terminalTail) {
}
