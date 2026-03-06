When I use a `suspend` function in a controller method, MDC context propagation works correctly. I can see MDC values inside the coroutine.

When I use a controller method that returns a `Flow`, MDC context propagation doesn't work and I can't see MDC values inside the flow.

The repository contains two Kotlin files:

- In `src/main`, one file with the controllers and app configuration
- In `src/test`, a pair of test cases that demonstrate context working for `suspend` functions and not working for `Flow`.
