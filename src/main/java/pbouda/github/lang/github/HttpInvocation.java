package pbouda.github.lang.github;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

public class HttpInvocation<T> {

    private static final System.Logger LOG = System.getLogger(HttpInvocation.class.getName());

    /**
     * Retry on all exceptions that inherits from IOException:
     * <ul>
     *     <li>{@link java.net.http.HttpTimeoutException}</li>
     *     <li>{@link java.net.http.HttpConnectTimeoutException}</li>
     *     <li>{@link java.nio.channels.ClosedChannelException}</li>
     *     <li>{@code sun.net.ConnectionResetException}</li>
     * </ul>
     */
    private static final Predicate<Throwable> DEFAULT_RETRY_ON_THROWABLE =
            ex -> ex instanceof IOException;

    /**
     * A default number of maximum retries on both types <b>on-response</b> and <b>on-throwable</b>
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * When a retry on-response exceeded then throw an exception by default.
     */
    private static final boolean DEFAULT_THROW_WHEN_RETRY_ON_RESPONSE_EXCEEDED = true;

    /**
     * By default, it waits 5 seconds between two retries.
     */
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(5);

    /**
     * If there is no {@link BodyHandler} specified, then discard entire incoming entity in a response.
     */
    private static final BodyHandler<Void> DEFAULT_BODY_HANDLER = HttpResponse.BodyHandlers.discarding();

    private final HttpClient client;
    private final HttpRequest request;
    private final BodyHandler<T> handler;
    private final AtomicInteger attempts = new AtomicInteger();
    private final Predicate<HttpResponse<?>> retryOnResponse;
    private final Predicate<Throwable> retryOnThrowable;
    private final int maxAttempts;
    private final boolean throwWhenRetryOnResponseExceeded;
    private final Executor delayedExecutor;

    private HttpInvocation(Builder<T> builder) {
        this.client = builder.client != null
                ? builder.client : HttpClient.newHttpClient();
        this.request = builder.request;
        this.handler = builder.bodyHandler;
        this.maxAttempts = builder.maxAttempts != null
                ? builder.maxAttempts : DEFAULT_MAX_ATTEMPTS;
        this.retryOnResponse = builder.retryOnResponse != null
                ? builder.retryOnResponse : defaultRetryOnResponse();
        this.retryOnThrowable = builder.retryOnThrowable != null
                ? builder.retryOnThrowable : DEFAULT_RETRY_ON_THROWABLE;
        this.throwWhenRetryOnResponseExceeded = builder.throwWhenRetryOnResponseExceeded != null
                ? builder.throwWhenRetryOnResponseExceeded : DEFAULT_THROW_WHEN_RETRY_ON_RESPONSE_EXCEEDED;

        Duration delay = builder.retryDelay != null
                ? builder.retryDelay
                : DEFAULT_RETRY_DELAY;
        this.delayedExecutor = CompletableFuture.delayedExecutor(
                delay.toMillis(), TimeUnit.MILLISECONDS);
    }
   
    /**
     * Retry on all Server Response (status code >= 500).
     *
     * @return default implementation of retry-on-response based on a status code.
     */
    private static Predicate<HttpResponse<?>> defaultRetryOnResponse() {
        return resp -> resp.statusCode() >= 500;
    }

    /**
     * Invokes a configured {@link HttpInvocation} using {@link Builder} and
     * handle exceptions, incorrect responses and retries with a configured
     * delay.
     *
     * @return a completable future with a completed response or failed in
     * case of any exception.
     */
    public CompletableFuture<HttpResponse<T>> invoke() {
        attempts.incrementAndGet();
        return client.sendAsync(request, handler)
                .thenApply(resp -> {
                    if (retryOnResponse.test(resp)) {
                        LOG.log(ERROR, "HTTP Invocation failed status_code: {}", resp.statusCode());
                        return attemptRetry(resp, null);
                    } else {
                        return CompletableFuture.completedFuture(resp);
                    }
                })
                .exceptionally(ex -> {
                    LOG.log(ERROR, "HTTP Invocation failed on exception: " + ex.getMessage(), ex);
                    // All internal exceptions are wrapped by `CompletionException`
                    if (retryOnThrowable.test(ex.getCause())) {
                        return attemptRetry(null, ex);
                    } else {
                        return CompletableFuture.failedFuture(ex);
                    }
                })
                .thenCompose(Function.identity());
    }

    /**
     * It tries to invoke the request again if there is any remaining attempt, or handle the situation
     * when a threshold of maximum attempts was exceeded.
     *
     * @param response  a failed response or <b>NULL</b>.
     * @param throwable a thrown exception or <b>NULL</b>.
     * @return a new completable future with a next attempt, or a failed response/exception in a case
     * of exceeded attempts.
     */
    private CompletableFuture<HttpResponse<T>> attemptRetry(HttpResponse<T> response, Throwable throwable) {
        if (attemptsRemains()) {
            LOG.log(WARNING, "Retrying: attempt={0} uri={1}", attempts.get() + 1, request.uri());
            return CompletableFuture.supplyAsync(this::invoke, delayedExecutor)
                    .thenCompose(Function.identity());
        } else {
            return handleRetryExceeded(response, throwable);
        }
    }

    /**
     * Defines the handler for an exceeded retry attempts. If the last attempt failed because of
     * an exception then throw it immediately. However, if the attempt failed on a regular response and
     * status code, them there are two possible behaviors based on the property {@link #throwWhenRetryOnResponseExceeded}.
     * <ul>
     *     <li><b>TRUE</b> when {@link #maxAttempts} is exceeded then an exception is thrown</li>
     *     <li><b>FALSE</b> when {@link #maxAttempts} is exceeded then the latest {@link HttpResponse}
     *     is returned</li>
     * </ul>
     *
     * @param response the very latest response object
     * @return a new completable future with a completed or failed state
     * depending on {@link #throwWhenRetryOnResponseExceeded}
     */
    private CompletableFuture<HttpResponse<T>> handleRetryExceeded(
            HttpResponse<T> response, Throwable throwable) {

        if (throwable != null || throwWhenRetryOnResponseExceeded) {
            Throwable ex = throwable == null
                    ? new RuntimeException("Retries exceeded: status-code=" + response.statusCode())
                    : throwable;

            return CompletableFuture.failedFuture(ex);
        } else {
            return CompletableFuture.completedFuture(response);
        }
    }

    /**
     * Returns <b>TRUE</b> if the number of retries has not exceeded the predefined
     * {@link #maxAttempts} value.
     *
     * @return <b>TRUE</b> if some attempts still remaining.
     */
    private boolean attemptsRemains() {
        return attempts.get() < maxAttempts;
    }

    /**
     * Creates a builder without an explicit {@link BodyHandler} which means that the default
     * {@link #DEFAULT_BODY_HANDLER} (discarding) with a return type {@link Void}.
     *
     * @param request an http request to invoke.
     * @return a builder with predefined <b>request</b> and a body-handler {@link #DEFAULT_BODY_HANDLER}.
     */
    public static Builder<Void> builder(HttpRequest request) {
        return new Builder<>(request, DEFAULT_BODY_HANDLER);
    }

    /**
     * Creates a builder along with a {@link BodyHandler} that determines the return type
     * defined by a generic <b>T</b>.
     *
     * @param request     a http request to invoke.
     * @param bodyHandler a handler to process an incoming entity in a response.
     * @param <T>         a type of body of incoming entity.
     * @return a builder with predefined <b>request</b> and <b>bodyHandler</b>.
     */
    public static <T> Builder<T> builder(HttpRequest request, BodyHandler<T> bodyHandler) {
        return new Builder<>(request, bodyHandler);
    }

    public static final class Builder<T> {
        private final HttpRequest request;
        private final BodyHandler<T> bodyHandler;
        private HttpClient client;
        private Integer maxAttempts;
        private Duration retryDelay;
        private Predicate<HttpResponse<?>> retryOnResponse;
        private Predicate<Throwable> retryOnThrowable;
        private Boolean throwWhenRetryOnResponseExceeded;

        public Builder(HttpRequest request, BodyHandler<T> bodyHandler) {
            this.request = request;
            this.bodyHandler = bodyHandler;
        }

        public Builder<T> withHttpClient(HttpClient client) {
            this.client = client;
            return this;
        }

        public Builder<T> withMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder<T> withRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder<T> withRetryOnResponse(Predicate<HttpResponse<?>> retryOnResponse) {
            this.retryOnResponse = retryOnResponse;
            return this;
        }

        public Builder<T> withRetryOnThrowable(Predicate<Throwable> retryOnThrowable) {
            this.retryOnThrowable = retryOnThrowable;
            return this;
        }

        public Builder<T> withThrowWhenRetryOnResponseExceeded(boolean throwWhenRetryOnResponseExceeded) {
            this.throwWhenRetryOnResponseExceeded = throwWhenRetryOnResponseExceeded;
            return this;
        }

        public HttpInvocation<T> build() {
            return new HttpInvocation<>(this);
        }
    }
}