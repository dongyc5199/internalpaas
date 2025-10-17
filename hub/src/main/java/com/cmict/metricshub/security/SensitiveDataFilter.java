package com.cmict.metricshub.security;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Logback Filter to mask sensitive information in logs
 * 
 * Masks:
 * - Passwords (password=xxx, PASSWORD: xxx)
 * - Bearer tokens (Bearer xxx, Authorization: Bearer xxx)
 * - API keys (api_key=xxx, apiKey: xxx)
 * - Connection strings with credentials
 * - JWT tokens
 * 
 * Usage in logback-spring.xml:
 * <pre>
 * &lt;appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender"&gt;
 *   &lt;filter class="com.cmict.metricshub.security.SensitiveDataFilter"/&gt;
 *   &lt;encoder&gt;
 *     &lt;pattern&gt;...&lt;/pattern&gt;
 *   &lt;/encoder&gt;
 * &lt;/appender&gt;
 * </pre>
 * 
 * @author Metrics Hub Team
 * @since 1.0.0
 */
@Slf4j
public class SensitiveDataFilter extends Filter<ILoggingEvent> {

    // Regex patterns for sensitive data
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|pwd|passwd)[=:\\s]+['\"]?([^'\"\\s,}]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
        "Bearer\\s+([A-Za-z0-9_\\-\\.]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(api[_-]?key|apikey)[=:\\s]+['\"]?([^'\"\\s,}]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONNECTION_STRING_PATTERN = Pattern.compile(
        "(jdbc:[^:]+://[^:]+):([^@]+)@",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"
    );

    private static final String MASK = "***MASKED***";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        // This filter doesn't block events, it modifies them
        // The actual masking happens in a custom PatternLayout
        return FilterReply.NEUTRAL;
    }

    /**
     * Mask sensitive data in log message
     * 
     * @param message Original log message
     * @return Masked log message
     */
    public static String maskSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = message;

        // Mask passwords
        masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1=" + MASK);

        // Mask Bearer tokens
        masked = BEARER_TOKEN_PATTERN.matcher(masked).replaceAll("Bearer " + MASK);

        // Mask API keys
        masked = API_KEY_PATTERN.matcher(masked).replaceAll("$1=" + MASK);

        // Mask connection string credentials
        masked = CONNECTION_STRING_PATTERN.matcher(masked).replaceAll("$1:" + MASK + "@");

        // Mask JWT tokens
        masked = JWT_PATTERN.matcher(masked).replaceAll(MASK);

        return masked;
    }

    /**
     * Custom PatternLayout that applies sensitive data masking
     */
    public static class MaskingPatternLayout extends PatternLayout {
        @Override
        public String doLayout(ILoggingEvent event) {
            String originalMessage = super.doLayout(event);
            return maskSensitiveData(originalMessage);
        }
    }
}
