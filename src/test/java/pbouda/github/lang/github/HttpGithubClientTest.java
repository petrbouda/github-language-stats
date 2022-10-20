package pbouda.github.lang.github;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pbouda.github.lang.MockServer;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
@DisabledIfEnvironmentVariable(named = "TESTCONTAINERS_DISABLED", matches = "true")
class HttpGithubClientTest {

    private static final String AUTH_HEADER_VALUE = "token MyToken";
    private static final String ORGANIZATION = "MyOrganization";
    private static final int PAGE_SIZE = 2;

    @Container
    private static final MockServerContainer CONTAINER = new MockServerContainer(MockServer.DOCKER_IMAGE);

    private static GithubClient GITHUB_CLIENT;

    @BeforeAll
    public static void setup() {
        GITHUB_CLIENT = new HttpGithubClient(
                URI.create(CONTAINER.getEndpoint()),
                ORGANIZATION,
                "MyToken",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                PAGE_SIZE,
                new HttpGithubClient.Retry(1, Duration.ofSeconds(1))
        );
    }

    @Test
    public void repositoriesPaging() {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);
        mockRepoRequest(mockClient, "1");
        mockRepoRequest(mockClient, "2");
        mockRepoRequest(mockClient, "3");

        List<GithubRepository> actual = GITHUB_CLIENT.repositories();

        List<GithubRepository> expected = List.of(
                new GithubRepository("repository-1", URI.create("https://api.github.com/repos/MyOrganization/repository-1/languages")),
                new GithubRepository("repository-2", URI.create("https://api.github.com/repos/MyOrganization/repository-2/languages")),
                new GithubRepository("repository-3", URI.create("https://api.github.com/repos/MyOrganization/repository-3/languages")),
                new GithubRepository("repository-4", URI.create("https://api.github.com/repos/MyOrganization/repository-4/languages")),
                new GithubRepository("repository-5", URI.create("https://api.github.com/repos/MyOrganization/repository-5/languages"))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void repositoriesEmpty() {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);
        mockClient
                .when(request()
                        .withMethod("GET")
                        .withHeader("Authorization", AUTH_HEADER_VALUE)
                        .withPath("/orgs/MyOrganization/repos")
                        .withQueryStringParameter("page", "1")
                        .withQueryStringParameter("per_page", String.valueOf(PAGE_SIZE))
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonBody.json("[]"))
                );

        List<GithubRepository> actual = GITHUB_CLIENT.repositories();
        assertEquals(List.of(), actual);
    }

    @Test
    public void languageDetails() {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        GithubRepository repo = new GithubRepository(
                "repository-1", URI.create(CONTAINER.getEndpoint() + "/repos/MyOrganization/repository-1/languages"));

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withHeader("Authorization", AUTH_HEADER_VALUE)
                        .withPath("/repos/MyOrganization/repository-1/languages")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(MockServer.json("laguages.json"))
                );

        List<LanguageBytes> actual = GITHUB_CLIENT.languageDetails(repo);

        List<LanguageBytes> languages = List.of(
                new LanguageBytes("Ruby", 1),
                new LanguageBytes("Java", 2));

        assertEquals(languages, actual);
    }

    @Test
    public void languageDetailsEmpty() {
        MockServerClient mockClient = MockServer.cleanClient(CONTAINER);

        GithubRepository repo = new GithubRepository(
                "repository-1", URI.create(CONTAINER.getEndpoint() + "/repos/MyOrganization/repository-1/languages"));

        mockClient
                .when(request()
                        .withMethod("GET")
                        .withHeader("Authorization", AUTH_HEADER_VALUE)
                        .withPath("/repos/MyOrganization/repository-1/languages")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonBody.json("{}"))
                );

        List<LanguageBytes> actual = GITHUB_CLIENT.languageDetails(repo);

        assertEquals(List.of(), actual);
    }

    private static void mockRepoRequest(MockServerClient mockClient, String page) {
        mockClient
                .when(request()
                        .withMethod("GET")
                        .withHeader("Authorization", AUTH_HEADER_VALUE)
                        .withPath("/orgs/MyOrganization/repos")
                        .withQueryStringParameter("page", page)
                        .withQueryStringParameter("per_page", String.valueOf(PAGE_SIZE))
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(MockServer.json("repositories-page" + page + ".json"))
                );
    }
}