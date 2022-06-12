package com.github.iamnicknack.slc.api.lease;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.iamnicknack.slc.api.test.MatchedLoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import com.github.iamnicknack.slc.api.lease.LeaseFactory.LeaseSupplier;
import com.github.iamnicknack.slc.api.lease.LeaseFactory.ReleaseConsumer;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LeaseFactoryTest {

    @SuppressWarnings("unchecked")
    static Appender<ILoggingEvent> mockAppender = mock(Appender.class);

    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(LeaseFactory.class.getName());
        logger.addAppender(mockAppender);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        reset(mockAppender);
    }

    @Nested
    class FactoryTests {
        @Test
        void supplierExceptionThrowsLeaseException() {
            LeaseSupplier<Object> supplier = () -> {
                throw new RuntimeException("EX");
            };
            ReleaseConsumer<Object> consumer = value -> {};

            var factory = LeaseFactory.create(supplier, consumer);

            var thrown  = assertThrows(Lease.LeaseException.class, factory::lease);
            assertEquals("Failed to acquire lease", thrown.getMessage());
            assertEquals("EX", thrown.getCause().getMessage());
        }

        @Test
        void consumerExceptionThrowsLeaseException() {
            LeaseSupplier<Object> supplier = Object::new;
            ReleaseConsumer<Object> consumer = value -> {
                throw new RuntimeException("EX");
            };

            LeaseFactory.create(supplier, consumer)
                    .lease()
                    .close();

            var expectedEvent = new MatchedLoggingEvent(
                    "WARN", "Release failed: {}", "EX"
            );
            verify(mockAppender).doAppend(argThat(expectedEvent));
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class LeaseTests {
        private final LeaseSupplier<String> supplierCheck = mock(LeaseSupplier.class);
        private final ReleaseConsumer<String> releaseCheck = mock(ReleaseConsumer.class);

        private final LeaseFactory<String> factory = LeaseFactory.create(supplierCheck, releaseCheck);

        @BeforeEach
        void beforeEach() {
            reset(supplierCheck, releaseCheck);
        }

        /**
         * Verify that close is called by try-with-resources
         */
        @Test
        void tryInvokesClose() throws Exception {
            final String testValue = "TEST";
            when(supplierCheck.lease()).thenReturn(testValue);

            try(var lease = factory.lease()) {
                assertEquals(testValue, lease.execute(Function.identity()::apply));
            }
            verify(releaseCheck).release(testValue);
        }

        /**
         * Verify that lease is released when an exception is thrown from a {@link Lease.LeaseFunction}
         */
        @Test
        void exceptionInvokesClose() throws Exception {
            final String testValue = "TEST";
            when(supplierCheck.lease()).thenReturn(testValue);

            try(var lease = factory.lease()) {
                Exception exception = assertThrows(Lease.LeaseException.class, () -> lease.execute(ignored -> {
                    throw new RuntimeException("RUNTIME EXCEPTION");
                }));
                assertEquals("RUNTIME EXCEPTION", exception.getCause().getMessage());
            }
            verify(releaseCheck).release(testValue);
        }

        @Test
        void executeReleasesLease() throws Exception {
            factory.execute(Function.identity()::apply);
            verify(releaseCheck).release(any());
        }

        @Test
        void executeReleasesLeaseAndForwardsException() throws Exception {
            assertThrows(Lease.LeaseException.class, () -> factory.execute(leasedValue -> {
                throw new RuntimeException("EX");
            }));
            verify(releaseCheck).release(any());
        }
    }

    /**
     * Tests to validation that assumptions made about try-with-resource are correct
     */
    @Nested
    class TryWithResourcesValidation {

        private final Runnable invocationCheck = mock(Runnable.class);
        private final LeaseFactory<String> factory = () -> new Lease<>() {
            @Override
            public <R> R execute(LeaseFunction<String, R> function) throws LeaseException {
                throw new RuntimeException("THROWN");
            }

            @Override
            public void close() {
                invocationCheck.run();
            }
        };

        @BeforeEach
        void beforeEach() {
            reset(invocationCheck);
        }

        @Test
        @SuppressWarnings("resource")
        void doesNotInvokeClose() {
            var lease = factory.lease();
            assertThrows(RuntimeException.class,
                    () -> lease.execute(ignored -> ignored)
            );

            verifyNoInteractions(invocationCheck);
        }

        @Test
        void callsCloseOnException() {
            try(var lease = factory.lease()) {
                assertThrows(RuntimeException.class,
                        () -> lease.execute(ignored -> ignored)
                );
            }

            verify(invocationCheck).run();
        }
    }


}