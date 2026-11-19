# 18. Virtual threads enabled

- **Status:** Accepted
- **Type:** Technical
- **Requirements:** [FR-5](../requirements.md#fr-5-safe-under-concurrent-requests), and the brief's "could realistically run in production"

## Context

[Decision 16](16-virtual-threads-not-enabled.md) kept Tomcat's platform threads, and said to
enable virtual threads together with an explicit limit on work in flight. Its background still
holds: how JDBC runs on virtual threads, why our request path does not pin, and the thread names
measured with the setting on.

## Decision

**Run every request on a virtual thread, and fail fast when the database is busy.**

```yaml
spring:
  threads:
    virtual:
      enabled: true
  datasource:
    hikari:
      connection-timeout: 5000 # milliseconds, default 30000
```

## Why

- **Waiting is cheap.** A request waiting for a connection, a row lock, or a reply parks its
  virtual thread and frees the OS thread. No thread pool to size.
- **Ready for waits outside the database.** Calls to another service, such as a payout provider,
  will not need a thread pool of their own.
- **The limit stays explicit.** Without Tomcat's 200 threads, the 10 database connections are the
  only limit. A 5-second `connectionTimeout` turns overload into a quick `503` with
  `Retry-After: 1` ([decision 08](08-error-responses.md)), instead of thousands of requests
  queueing for 30 seconds.

With 500 transfers at once:

```
Before (platform threads)             Now (virtual threads)
200 get a thread, 300 wait in Tomcat   500 get a virtual thread
 └─ 10 get a DB connection              └─ 10 get a DB connection, 490 park
                                        └─ any still waiting after 5 s get 503
```

## Consequences

- Throughput is the same. The 10 connections and per-account row locks still set it.
- `server.tomcat.threads.max` no longer applies. In-flight work is capped only by
  `server.tomcat.max-connections` (8192) and the 5-second connection timeout.
- A request that waits more than 5 seconds for a connection fails with `503`, even if it would have
  succeeded later. Clients retry, which is safe ([decision 04](04-idempotent-retries.md)).
- Pinning would stall carriers. Watch the JFR event `jdk.VirtualThreadPinned` when adding a
  library that blocks in native code.
- Spring `@Async` and `@Scheduled` would also run on virtual threads. This service uses neither.

## Alternatives considered

- **Keep platform threads** ([decision 16](16-virtual-threads-not-enabled.md)): same throughput,
  with Tomcat's thread pool as the natural limit.
- **Virtual threads with the default 30-second timeout:** overload queues thousands of requests
  until they time out together.
- **A semaphore or filter that caps concurrent requests:** a hard limit, but one more component to
  size. The connection timeout is enough while every request uses the database.

## Evidence

Thread dump of the running Docker stack after `scripts/smoke.sh`, with the setting on:

| Threads | Count | Meaning |
|---|---|---|
| `http-nio-8080-exec-N`, `http-nio-9090-exec-N` | 0 | Tomcat's platform worker pools are gone |
| `http-nio-8080-Acceptor`, `-Poller` (and 9090) | 1 each | Still platform threads, as in decision 16 |
| `ForkJoinPool-1-worker-N` | 5 | Carrier threads that run the virtual threads |
| `MasterPoller` | 1 | The JDK's thread that watches sockets for parked virtual threads |

The smoke test passed, and all 114 tests pass.
