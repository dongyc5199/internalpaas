package com.waveterm.demo.terminal.completion;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Validated
public class CompletionController {

    private final CommandCompletionService completionService;

    public CompletionController(CommandCompletionService completionService) {
        this.completionService = completionService;
    }

    @PostMapping("/complete")
    public CompletionResponse complete(@Valid @RequestBody CompletionRequest request) {
        List<CompletionSuggestion> suggestions = completionService.complete(request.prompt(), request.recentCommands());
        return new CompletionResponse(suggestions);
    }
}
