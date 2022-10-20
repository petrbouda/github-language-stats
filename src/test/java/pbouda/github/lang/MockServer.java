package pbouda.github.lang;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class MockServer {

    public static final DockerImageName DOCKER_IMAGE = DockerImageName.parse("mockserver/mockserver").withTag("5.14.0");

    public static MockServerClient cleanClient(MockServerContainer container) {
        MockServerClient client = new MockServerClient(container.getHost(), container.getFirstMappedPort());
        client.reset();
        return client;
    }

    public static JsonBody json(String resourceName) {
        try {
            Path mockedResponse = Path.of(Resources.getResource("mockserver/" + resourceName).getPath());
            return JsonBody.json(Files.readString(mockedResponse));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find MockServer's resource: " + resourceName, e);
        }
    }
}
