package pbouda.github.lang.statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pbouda.github.lang.github.GithubClient;
import pbouda.github.lang.github.GithubRepository;
import pbouda.github.lang.github.LanguageBytes;
import pbouda.github.lang.storage.Storage;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
class StatisticsUpdateTaskTest {

    @Mock
    GithubClient githubClient;

    @Mock
    Storage<LanguageRatio> storage;

    @Test
    public void noRepository() {
        when(githubClient.repositories())
                .thenReturn(List.of());

        verifyNoMoreInteractions(githubClient);

        StatisticsUpdateTask task = new StatisticsUpdateTask(githubClient, storage);
        task.run();

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(storage).set(List.of()));
    }

    @Test
    public void repositoriesWithoutLanguages() {
        List<GithubRepository> repositories = List.of(
                new GithubRepository("repository-1", URI.create("https://api.github.com/repos/MyOrganization/repository-1/languages")),
                new GithubRepository("repository-2", URI.create("https://api.github.com/repos/MyOrganization/repository-2/languages")));

        when(githubClient.repositories())
                .thenReturn(repositories);
        when(githubClient.languageDetails(repositories.get(0)))
                .thenReturn(List.of());
        when(githubClient.languageDetails(repositories.get(1)))
                .thenReturn(List.of());

        StatisticsUpdateTask task = new StatisticsUpdateTask(githubClient, storage);
        task.run();

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(storage).set(List.of()));
    }

    @Test
    public void repositoriesWithLanguages() {
        List<GithubRepository> repositories = List.of(
                new GithubRepository("repository-1", URI.create("https://api.github.com/repos/MyOrganization/repository-1/languages")),
                new GithubRepository("repository-2", URI.create("https://api.github.com/repos/MyOrganization/repository-2/languages")));

        when(githubClient.repositories())
                .thenReturn(repositories);
        when(githubClient.languageDetails(repositories.get(0)))
                .thenReturn(List.of(new LanguageBytes("Java", 3), new LanguageBytes("Ruby", 2)));
        when(githubClient.languageDetails(repositories.get(1)))
                .thenReturn(List.of(new LanguageBytes("Java", 1), new LanguageBytes("Kotlin", 4)));

        StatisticsUpdateTask task = new StatisticsUpdateTask(githubClient, storage);
        task.run();

        List<LanguageRatio> expected = List.of(
                new LanguageRatio("Java", new BigDecimal("0.40")),
                new LanguageRatio("Ruby", new BigDecimal("0.20")),
                new LanguageRatio("Kotlin", new BigDecimal("0.40")));

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(storage).set(expected));
    }
}