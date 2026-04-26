package com.skipq.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Maps Logback's WARN level to WARNING to match GCP Cloud Logging's severity enum.
 * All other levels (DEBUG, INFO, ERROR) already match Cloud Logging names.
 */
public class CloudSeverityConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return event.getLevel() == Level.WARN ? "WARNING" : event.getLevel().toString();
    }
}
