package com.cmict.internalpaas.ai.model;

import java.util.List;

public record ChatContext(String terminalTail, List<String> uploads) {
}
