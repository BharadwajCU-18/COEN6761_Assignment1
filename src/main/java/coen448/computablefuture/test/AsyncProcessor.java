package coen448.computablefuture.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AsyncProcessor {

    public CompletableFuture<String> processAsync(List<Microservice> microservices, String message) {

        List<CompletableFuture<String>> futures = microservices.stream()
                .map(client -> client.retrieveAsync(message))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining(" ")));

    }

    public CompletableFuture<List<String>> processAsyncCompletionOrder(
            List<Microservice> microservices, String message) {

        List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = microservices.stream()
                .map(ms -> ms.retrieveAsync(message)
                        .thenAccept(completionOrder::add))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> completionOrder);

    }

    // Task 2 A
    public CompletableFuture<String> processAsyncFailFast(
            List<Microservice> services,
            List<String> messages) {
        if (services == null || messages == null) {
            throw new IllegalArgumentException("services/messages must not be null");
        }
        if (services.size() != messages.size()) {
            throw new IllegalArgumentException("services and messages must have same size");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < services.size(); i++) {
            futures.add(services.get(i).retrieveAsync(messages.get(i)));
        }

        // Fail-fast: if any future completes exceptionally, allOf completes
        // exceptionally
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // join preserves list order
                        .collect(Collectors.joining(" ")));
    }

    // Task 2 B
    public CompletableFuture<List<String>> processAsyncFailPartial(
            List<Microservice> services,
            List<String> messages) {
        if (services == null || messages == null) {
            throw new IllegalArgumentException("services/messages must not be null");
        }
        if (services.size() != messages.size()) {
            throw new IllegalArgumentException("services and messages must have same size");
        }

        // Convert each call into a "safe" future that never throws:
        // success -> CompletableFuture.completedFuture(result)
        // failure -> CompletableFuture.completedFuture(null)
        List<CompletableFuture<String>> safeFutures = new ArrayList<>();

        for (int i = 0; i < services.size(); i++) {
            CompletableFuture<String> f = services.get(i).retrieveAsync(messages.get(i))
                    .handle((val, ex) -> ex == null ? val : null);
            safeFutures.add(f);
        }

        // allOf now cannot fail (because each inner future completes normally)
        return CompletableFuture.allOf(safeFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> safeFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(x -> x != null) // keep only successes
                        .collect(Collectors.toList()));
    }

}