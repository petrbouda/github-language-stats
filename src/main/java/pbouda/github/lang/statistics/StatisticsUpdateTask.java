package pbouda.github.lang.statistics;

import jdk.incubator.concurrent.StructuredTaskScope;
import pbouda.github.lang.github.GithubClient;
import pbouda.github.lang.github.GithubRepository;
import pbouda.github.lang.github.HttpInvocation;
import pbouda.github.lang.github.LanguageBytes;
import pbouda.github.lang.storage.Storage;

import java.util.List;
import java.util.function.Function;

import static java.lang.System.Logger.Level.INFO;

public class StatisticsUpdateTask implements Runnable {

    private static final System.Logger LOG = System.getLogger(HttpInvocation.class.getName());

    private final GithubClient githubClient;
    private final Storage<LanguageRatio> storage;
    private final Function<List<LanguageBytes>, List<LanguageRatio>> calculateRatio;

    public StatisticsUpdateTask(GithubClient githubClient, Storage<LanguageRatio> storage) {
        this(githubClient, storage, new LanguageRatioAlgorithm());
    }

    public StatisticsUpdateTask(
            GithubClient githubClient,
            Storage<LanguageRatio> storage,
            Function<List<LanguageBytes>, List<LanguageRatio>> calculateRatio) {

        this.githubClient = githubClient;
        this.storage = storage;
        this.calculateRatio = calculateRatio;
    }

    @Override
    public void run() {
        List<GithubRepository> repositories = githubClient.repositories();

        List<LanguageBytes> languages;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var futures = repositories.stream()
                    .map(repo -> scope.fork(() -> githubClient.languageDetails(repo)))
                    .toList();

            scope.join();
            scope.throwIfFailed();

            languages = futures.stream()
                    .flatMap(future -> future.resultNow().stream())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Cannot download the data about languages", e);
        }

        List<LanguageRatio> ratios = calculateRatio.apply(languages);
        storage.set(ratios);

        LOG.log(INFO, "Statistics Updated: {0}", ratios);
    }
}
