package pbouda.github.lang.github;

import java.util.List;

public interface GithubClient {

    /**
     * Fetches all repositories from the organization using this
     * dedicated client instance.
     *
     * @return all repositories in the organization.
     */
    List<GithubRepository> repositories();

    /**
     * Fetches all languages used inside the given repository
     * and a number of bytes belonging to the particular language.
     *
     * @param repository a repository that is scanned for languages.
     * @return list of all used languages inside the repository.
     */
    List<LanguageBytes> languageDetails(GithubRepository repository);
}
