package com.waveterm.demo.chat.model;

import java.util.List;

public record ChatContext(String terminalTail, List<String> uploads) {
}
