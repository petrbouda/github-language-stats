package pbouda.github.lang.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

class AtomicStorageTest {

    @Test
    public void initialValue() {
        AtomicStorage<String> storage = new AtomicStorage<>();
        assertEquals(List.of(), storage.get());
    }

    @Test
    public void withValue() {
        List<String> value = List.of("1", "2");

        AtomicStorage<String> storage = new AtomicStorage<>();

        Thread.ofVirtual().start(() -> storage.set(value));

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(value, storage.get()));
    }
}