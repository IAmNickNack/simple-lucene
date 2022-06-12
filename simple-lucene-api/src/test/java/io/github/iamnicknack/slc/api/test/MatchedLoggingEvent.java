package io.github.iamnicknack.slc.api.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;

/**
 * Mockito {@link ArgumentMatcher} for Logback {@link ILoggingEvent}
 * @param level level to match as a string
 * @param message the message / string template
 * @param arguments the arguments supplied to the message
 */
public record MatchedLoggingEvent(String level,
                                  String message,
                                  Object... arguments) implements ArgumentMatcher<ILoggingEvent> {

    @Override
    public boolean matches(ILoggingEvent loggingEvent) {
        return this.level.equals(loggingEvent.getLevel().levelStr)
                && this.message.equals(loggingEvent.getMessage())
                && Arrays.stream(this.arguments).allMatch(arg ->
                Arrays.asList(loggingEvent.getArgumentArray())
                        .contains(arg)
        );
    }
}
