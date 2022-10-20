package pbouda.github.lang.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

public class HttpGithubClient implements GithubClient {

    public record Retry(int attempts, Duration delay) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int DEFAULT_PAGE_SIZE = 30;

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final Retry DEFAULT_RETRY = new Retry(5, Duration.ofSeconds(2));

    private final Supplier<UriBuilder> reposUriBuilderSupplier;
    private final HttpClient httpClient;
    private final String apiToken;
    private final Duration requestTimeout;
    private final int pageSize;
    private final Retry retry;

    public HttpGithubClient(URI githubUri, String organization, String apiToken) {
        this(githubUri, organization, apiToken,
                DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_PAGE_SIZE, DEFAULT_RETRY);
    }

    public HttpGithubClient(
            URI githubUri,
            String organization,
            String apiToken,
            Duration connectTimeout,
            Duration requestTimeout,
            int pageSize,
            Retry retry) {

        this.apiToken = apiToken;
        this.requestTimeout = requestTimeout;
        this.pageSize = pageSize;
        this.retry = retry;
        this.reposUriBuilderSupplier = () -> UriComponentsBuilder.fromUri(githubUri)
                .path("/orgs/" + organization + "/repos");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public List<GithubRepository> repositories() {
        List<GithubRepository> repositories = new ArrayList<>();

        /*
         * Unfortunately, it looks like Github API needs to be paged
         * serially. There is no convenient way to get a number of
         * repositories and trigger all requests in parallel.
         */
        int nextPage = 1;
        int currentPageSize;
        do {
            HttpRequest request = createRepoPagingRequest(nextPage++);
            ArrayNode response = invokeBlockingGet(ArrayNode.class, request);

            for (JsonNode node : response) {
                GithubRepository repo = new GithubRepository(
                        node.get("name").asText(),
                        node.get("languages_url").asText());

                repositories.add(repo);
            }

            currentPageSize = response.size();
        } while (currentPageSize >= pageSize);

        return List.copyOf(repositories);
    }

    @Override
    public List<LanguageBytes> languageDetails(GithubRepository repository) {
        ObjectNode response = invokeBlockingGet(ObjectNode.class, createGenericRequest(repository.languageUri()));

        return StreamSupport.stream(spliteratorUnknownSize(response.fields(), Spliterator.ORDERED), false)
                .map(entry -> new LanguageBytes(entry.getKey(), entry.getValue().longValue()))
                .toList();
    }

    private <T extends JsonNode> T invokeBlockingGet(Class<T> clazz, HttpRequest request) {
        try {
            HttpResponse<T> response = HttpInvocation.builder(request, ofJsonNode(clazz))
                    .withHttpClient(httpClient)
                    .withMaxAttempts(retry.attempts)
                    .withRetryDelay(retry.delay)
                    .build()
                    .invoke()
                    .get();

            if (success(response)) {
                return response.body();
            } else {
                throw new RuntimeException("HTTP invocation failed: status_code=" + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed while invoking a request to Github: request=" + request, e);
        }
    }

    private HttpRequest createRepoPagingRequest(int page) {
        URI uri = reposUriBuilderSupplier.get()
                .queryParam("page", page)
                .queryParam("per_page", pageSize)
                .build();

        return createGenericRequest(uri);
    }

    private HttpRequest createGenericRequest(URI uri) {
        return HttpRequest.newBuilder()
                .header("Content-Type", "application/vnd.github+json")
                .header("Authorization", "token " + apiToken)
                .timeout(requestTimeout)
                .uri(uri)
                .GET()
                .build();
    }

    private static <T extends JsonNode> HttpResponse.BodyHandler<T> ofJsonNode(Class<T> ignored) {
        return (responseInfo) -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofByteArray(),
                HttpGithubClient::toJsonNode);
    }

    @SuppressWarnings("unchecked")
    private static <T extends JsonNode> T toJsonNode(byte[] content) {
        try {
            return (T) MAPPER.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse a response to JSON: response: " + new String(content), e);
        }
    }

    private static boolean success(HttpResponse<?> response) {
        int statusCode = response.statusCode();
        return statusCode >= 200 && statusCode <= 299;
    }
}
