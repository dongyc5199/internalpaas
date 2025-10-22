package com.waveterm.demo.terminal.completion;

import java.util.List;

public record CompletionResponse(List<CompletionSuggestion> suggestions) {
}
