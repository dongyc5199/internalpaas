package com.waveterm.demo.terminal;

import com.waveterm.demo.terminal.history.TerminalCommandEntity;
import com.waveterm.demo.terminal.history.TerminalHistoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/terminal")
@Validated
public class TerminalController {

    private final TerminalGateway sessionGateway;
    private final TerminalHistoryService historyService;

    public TerminalController(TerminalGateway sessionGateway,
                              TerminalHistoryService historyService) {
        this.sessionGateway = sessionGateway;
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public CommandHistoryResponse history(@RequestParam("sessionId") @NotBlank String sessionId,
                                          @RequestParam(value = "limit", required = false) Integer limit,
                                          @RequestParam(value = "cursor", required = false) String cursor,
                                          @RequestParam(value = "q", required = false) String keyword) {
        TerminalHistoryService.HistoryQuery query = new TerminalHistoryService.HistoryQuery(
                limit != null ? limit : 50,
                cursor,
                keyword
        );
        TerminalHistoryService.HistoryPage page = historyService.list(sessionId, query);
        List<TerminalHistoryItem> items = page.items().stream()
                .map(entity -> new TerminalHistoryItem(entity.getCommandId(), entity.getInput(), entity.getExecutedAt()))
                .toList();
        return new CommandHistoryResponse(items, page.nextCursor());
    }

    @PostMapping("/write")
    public ResponseEntity<Void> write(@Valid @RequestBody TerminalWriteRequest request) throws IOException {
        ManagedTerminalSession session = sessionGateway.get(request.sessionId())
                .filter(ManagedTerminalSession::isAlive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or closed"));
        session.sendCommand(request.data());
        return ResponseEntity.noContent().build();
    }

    public record TerminalWriteRequest(@NotBlank String sessionId, @NotBlank String data) {
    }

    public record TerminalHistoryItem(String id, String input, long ts) {
    }

    public record CommandHistoryResponse(List<TerminalHistoryItem> items, String nextCursor) {
    }
}
