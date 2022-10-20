package pbouda.github.lang.github;

import java.net.URI;

public record GithubRepository(String name, URI languageUri) {

    public GithubRepository(String name, String languageUri) {
        this(name, URI.create(languageUri));
    }
}
