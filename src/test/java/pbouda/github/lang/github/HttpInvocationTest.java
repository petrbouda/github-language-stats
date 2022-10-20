package pbouda.github.lang.github;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pbouda.github.lang.MockServer;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

@Testcontainers
@DisabledIfEnvironmentVariable(named = "TESTCONTAINERS_DISABLED", matches = "true")
class HttpInvocationTest {

    private static final String PATH = "/rest/api/latest";

    @Container
    private static final MockServerContainer CONTAINER = new MockServerContainer(MockServer.DOCKER_IMAGE);

    private static HttpRequest REQUEST;

    @BeforeAll
    public static void setup() {
        REQUEST = HttpRequest.newBuilder()
                .uri(URI.create(CONTAINER.getEndpoint()).resolve(PATH))
                .GET()
                .build();
    }

    @Test
    public void singleSuccessInvocationWithoutBody() throws Exception {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withPath(PATH)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        HttpResponse<Void> response = HttpInvocation.builder(REQUEST)
                .build()
                .invoke()
                .get(1, TimeUnit.SECONDS);

        assertEquals(200, response.statusCode());
    }

    @Test
    public void singleSuccessInvocationWithBody() throws Exception {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withPath(PATH)
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody("Body")
                );

        HttpResponse<String> response =
                HttpInvocation.builder(REQUEST, HttpResponse.BodyHandlers.ofString())
                        .build()
                        .invoke()
                        .get(1, TimeUnit.SECONDS);

        assertEquals(200, response.statusCode());
        assertEquals("Body", response.body());
    }

    @Test
    public void successfulRetry() throws Exception {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        org.mockserver.model.HttpRequest getRequest = request()
                .withMethod("GET")
                .withPath(PATH);

        mockClient
                .when(getRequest, Times.exactly(2))
                .respond(response()
                        .withStatusCode(500)
                );

        mockClient
                .when(getRequest)
                .respond(response()
                        .withStatusCode(200)
                        .withBody("Body")
                );

        HttpResponse<String> response =
                HttpInvocation.builder(REQUEST, HttpResponse.BodyHandlers.ofString())
                        .withMaxAttempts(3)
                        .withRetryDelay(Duration.ofMillis(100))
                        .build()
                        .invoke()
                        .get(1, TimeUnit.SECONDS);

        assertEquals(200, response.statusCode());
        assertEquals("Body", response.body());

        mockClient.verify(getRequest, exactly(3));
    }

    @Test
    public void attemptsExceededOnResponseThrowException() {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withPath(PATH)
                )
                .respond(response()
                        .withStatusCode(500)
                );

        Executable executable =
                () -> HttpInvocation.builder(REQUEST, HttpResponse.BodyHandlers.ofString())
                        .withMaxAttempts(3)
                        .withRetryDelay(Duration.ofMillis(100))
                        .withThrowWhenRetryOnResponseExceeded(true)
                        .build()
                        .invoke()
                        .get(1, TimeUnit.SECONDS);

        ExecutionException ex = assertThrows(ExecutionException.class, executable);
        assertEquals("java.lang.RuntimeException: Retries exceeded: status-code=500", ex.getMessage());
    }

    @Test
    public void attemptsExceededOnResponseReturnResponse() throws Exception {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withPath(PATH)
                )
                .respond(response()
                        .withStatusCode(500)
                );

        HttpResponse<String> response = HttpInvocation.builder(REQUEST, HttpResponse.BodyHandlers.ofString())
                .withMaxAttempts(3)
                .withRetryDelay(Duration.ofMillis(100))
                .withThrowWhenRetryOnResponseExceeded(false)
                .build()
                .invoke()
                .get(1, TimeUnit.SECONDS);

        assertEquals(500, response.statusCode());
    }
}