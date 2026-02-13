package coen448.computablefuture.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.RepeatedTest;

public class AsyncProcessorTest {
    @RepeatedTest(5)
    public void testProcessAsyncSuccess() throws Exception {
        Microservice service1 = new Microservice("Hello");
        Microservice service2 = new Microservice("World");

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<String> resultFuture = processor.processAsync(List.of(service1, service2), "hi");

        String result = resultFuture.get(1, TimeUnit.SECONDS);

        // Order preserved by joining list order in processAsync
        assertEquals("Hello:HI World:HI", result);
    }

    @ParameterizedTest
    @CsvSource({
            "hi, Hello:HI World:HI",
            "cloud, Hello:CLOUD World:CLOUD",
            "async, Hello:ASYNC World:ASYNC"
    })
    public void testProcessAsync_withDifferentMessages(
            String message,
            String expectedResult)
            throws ExecutionException, InterruptedException, TimeoutException {

        Microservice service1 = new Microservice("Hello");
        Microservice service2 = new Microservice("World");

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<String> resultFuture = processor.processAsync(List.of(service1, service2), message);

        String result = resultFuture.get(1, TimeUnit.SECONDS);

        assertEquals(expectedResult, result);

    }

    @RepeatedTest(20)
    void showNondeterminism_completionOrderVaries() throws Exception {

        Microservice s1 = new Microservice("A");
        Microservice s2 = new Microservice("B");
        Microservice s3 = new Microservice("C");

        AsyncProcessor processor = new AsyncProcessor();

        List<String> order = processor
                .processAsyncCompletionOrder(List.of(s1, s2, s3), "msg")
                .get(1, TimeUnit.SECONDS);

        // Not asserting a fixed order (because it is intentionally nondeterministic)
        System.out.println(order);

        // A minimal sanity check: all three must be present
        assertEquals(3, order.size());

        assertTrue(order.stream().anyMatch(x -> x.startsWith("A:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("B:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("C:")));
    }

    @Test
    void failSoft_shouldUseFallback_whenServiceFails_andNeverThrow() throws Exception {
        AsyncProcessor processor = new AsyncProcessor();

        Microservice ok1 = new Microservice("OK1");

        Microservice fail = new Microservice("FAIL") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new RuntimeException("forced failure"));
                return f;
            }
        };

        Microservice ok2 = new Microservice("OK2");

        String fallback = "FALLBACK";

        CompletableFuture<String> result = processor.processAsyncFailSoft(
                List.of(ok1, fail, ok2),
                List.of("m1", "m2", "m3"),
                fallback);

        String out = result.get(1, TimeUnit.SECONDS);

        // Should not throw, and should contain fallback in the middle
        assertTrue(out.startsWith("OK1:"));
        assertTrue(out.contains(" " + fallback + " "));
        assertTrue(out.endsWith("OK2:M3"));
    }

    @Test
    void failSoft_allFail_shouldReturnAllFallbacks() throws Exception {
        AsyncProcessor processor = new AsyncProcessor();

        Microservice fail1 = new Microservice("F1") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new RuntimeException("fail"));
                return f;
            }
        };

        Microservice fail2 = new Microservice("F2") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new RuntimeException("fail"));
                return f;
            }
        };

        String fallback = "X";

        String out = processor.processAsyncFailSoft(
                List.of(fail1, fail2),
                List.of("a", "b"),
                fallback).get(1, TimeUnit.SECONDS);

        assertEquals("X X", out);
    }

}
