package com.waveterm.demo.terminal.upload;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UploadValidationException extends ResponseStatusException {

    public UploadValidationException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
