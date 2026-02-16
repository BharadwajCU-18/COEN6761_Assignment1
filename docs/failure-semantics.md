# Failure Semantics in Concurrent Systems

## Introduction

In a concurrent system, multiple tasks run at the same time. While this improves performance, it also raises an important question:

What should happen if one of the tasks fails?

This decision is called *failure semantics*. In this project, we implemented three different failure-handling strategies using Java’s `CompletableFuture` in a fan-out / fan-in model.

The three policies are:

1. Fail-Fast
2. Fail-Partial
3. Fail-Soft

Each one behaves differently when a microservice fails.

---

## 1. Fail-Fast

If any microservice fails, the entire operation fails.

No partial result is returned. The exception is propagated to the caller.

### When it is useful

Fail-Fast is useful in systems where correctness is very important, such as:

- Banking transactions
- Payment systems
- Critical business logic

In these systems, partial results are not acceptable.

### Advantages

- Ensures strong correctness
- Prevents inconsistent data

### Disadvantages

- Low availability
- One small failure stops everything

---

## 2. Fail-Partial

If some microservices fail, the system still returns the successful results.

The overall computation does not fail. Failed services are simply ignored.

### When it is useful

Fail-Partial is useful in cases like:

- Dashboards
- Reports
- Analytics systems

In these cases, partial data is better than no data.

### Advantages

- Higher availability than Fail-Fast
- Useful results even if some services fail

### Disadvantages

- Some data may be missing
- Failures might not be immediately obvious

---

## 3. Fail-Soft

If a microservice fails, its result is replaced with a predefined fallback value.

The overall computation always completes successfully.

### When it is useful

Fail-Soft is useful for:

- High-availability systems
- User-facing APIs
- Systems where degraded output is acceptable

### Advantages

- Very high availability
- Never crashes due to a service failure

### Disadvantages

- Can hide serious errors
- Makes debugging harder if failures are not logged properly

---

## Why Failure Semantics Matter in Concurrency

Concurrency itself is not the main difficulty. The real challenge is deciding how to handle failures when multiple tasks are running at the same time.

Because tasks complete in a nondeterministic order, the system must clearly define what happens if one or more tasks fail.

By explicitly defining failure semantics, the system becomes more predictable and easier to reason about.

In this project, microservices are executed concurrently using CompletableFuture. Since tasks run in parallel, their completion order is nondeterministic. The CompletableFuture.allOf mechanism aggregates results, and the chosen failure policy determines whether exceptions are propagated, ignored, or replaced with fallback values. In testing, timeouts are used to ensure liveness and prevent indefinite blocking.

---


## Conclusion

Each failure policy represents a different trade-off:

- If correctness is most important → use Fail-Fast.
- If partial results are acceptable → use Fail-Partial.
- If availability is most important → use Fail-Soft.

There is no single best policy. The right choice depends on system requirements.

Understanding and defining failure semantics is essential when designing concurrent systems.
