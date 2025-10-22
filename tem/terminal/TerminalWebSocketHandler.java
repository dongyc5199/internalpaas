package com.waveterm.demo.terminal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waveterm.demo.terminal.gateway.SshTerminalGatewayAware;
import com.waveterm.demo.terminal.pty.PtySignal;
import com.waveterm.demo.terminal.ssh.model.SshErrorCode;
import com.waveterm.demo.terminal.ssh.model.SshTerminalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final TerminalGateway terminalGateway;
    private final Map<WebSocketSession, SessionBinding> bindings = new ConcurrentHashMap<>();

    public TerminalWebSocketHandler(TerminalGateway terminalGateway) {
        this.terminalGateway = terminalGateway;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String requestedSessionId = extractQueryParam(uri, "sessionId");
        String targetId = extractQueryParam(uri, "targetId");
        ManagedTerminalSession terminalSession;
        try {
            if (terminalGateway instanceof SshTerminalGatewayAware sshAware) {
                terminalSession = sshAware.getOrCreate(requestedSessionId, targetId);
            } else {
                terminalSession = terminalGateway.getOrCreate(requestedSessionId);
            }
        } catch (SshTerminalException sshEx) {
            log.warn("SSH session initialization failed: {}", sshEx.getMessage());
            sendError(session, sshEx.getCode(), sshEx.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        AtomicBoolean closing = new AtomicBoolean(false);

        Consumer<String> outputListener = chunk -> {
            if (!session.isOpen() || closing.get()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                        "type", "output",
                        "data", chunk
                ))));
            } catch (IOException e) {
                log.warn("Failed to send terminal chunk", e);
            }
        };

        Consumer<Integer> exitListener = code -> {
            if (!closing.compareAndSet(false, true)) {
                return;
            }
            if (!session.isOpen()) {
                terminalGateway.remove(terminalSession.getSessionId());
                return;
            }
            try {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                        "type", "exit",
                        "code", code
                ))));
            } catch (IOException e) {
                log.warn("Failed to send terminal exit event", e);
            } finally {
                terminalGateway.remove(terminalSession.getSessionId());
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException ignored) {
                    if (ignored.getCause() instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                } catch (RuntimeException runtimeEx) {
                    if (runtimeEx.getCause() instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    log.debug("Failed to close websocket session cleanly", runtimeEx);
                }
            }
        };

        terminalSession.addOutputListener(outputListener);
        terminalSession.addExitListener(exitListener);
        bindings.put(session, new SessionBinding(terminalSession, outputListener, exitListener));

        session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                "type", "session",
                "sessionId", terminalSession.getSessionId()
        ))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionBinding binding = bindings.get(session);
        if (binding == null) {
            log.debug("No binding for session {}", session.getId());
            return;
        }
        String payload = message.getPayload();
        if (payload == null || payload.isBlank()) {
            return;
        }
        Map<String, Object> parsed = tryParseJson(payload);
        if (!parsed.isEmpty()) {
            String type = stringValue(parsed.get("type"));
            switch (type == null ? "" : type.toLowerCase()) {
                case "input" -> handleInput(binding, stringValue(parsed.get("data")));
                case "resize" -> handleResize(binding, parsed);
                case "ping" -> handlePing(session, binding);
                case "signal" -> handleSignal(binding, stringValue(parsed.get("signal")));
                default -> log.debug("Ignoring unsupported websocket message type: {}", type);
            }
            return;
        }
        binding.session.sendCommand(payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionBinding binding = bindings.remove(session);
        if (binding != null) {
            binding.session.removeOutputListener(binding.outputListener);
            binding.session.removeExitListener(binding.exitListener);
        }
        log.debug("WebSocket closed: {} status={} reason={}", session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error on session {}", session.getId(), exception);
    }

    private void handleInput(SessionBinding binding, String data) throws IOException {
        if (data == null) {
            return;
        }
        binding.session.sendInput(data);
    }

    private void handleResize(SessionBinding binding, Map<String, Object> parsed) throws IOException {
        int cols = numberValue(parsed.get("cols"), 0);
        int rows = numberValue(parsed.get("rows"), 0);
        if (cols > 0 && rows > 0) {
            binding.session.resize(cols, rows);
        }
    }

    private void handlePing(WebSocketSession session, SessionBinding binding) {
        binding.session.touch();
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                    "type", "pong",
                    "ts", System.currentTimeMillis()
            ))));
        } catch (IOException ex) {
            log.debug("Failed to send pong", ex);
        }
    }

    private void handleSignal(SessionBinding binding, String signalName) throws IOException {
        if (signalName == null || signalName.isBlank()) {
            return;
        }
        try {
            PtySignal signal = PtySignal.valueOf(signalName.trim().toUpperCase());
            binding.session.sendSignal(signal);
        } catch (IllegalArgumentException ex) {
            log.debug("Unsupported PTY signal {}", signalName);
        }
    }

    private static Map<String, Object> tryParseJson(String payload) {
        try {
            return mapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ignored) {
            return Collections.emptyMap();
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static int numberValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String extractQueryParam(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            if (!key.equals(k)) {
                continue;
            }
            return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private record SessionBinding(ManagedTerminalSession session,
                                  Consumer<String> outputListener,
                                  Consumer<Integer> exitListener) {
    }

    private void sendError(WebSocketSession session, SshErrorCode code, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                    "type", "error",
                    "code", code.name().toLowerCase(),
                    "message", message
            ))));
        } catch (IOException sendEx) {
            log.debug("Failed to send SSH error message", sendEx);
        }
    }
}

