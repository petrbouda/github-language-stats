package pbouda.github.lang.storage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic implementation of {@link Storage} interface.
 * It atomically stores the list of data with the memory-access of <b>volatile</b>.
 * <p>
 * <b>Warning</b>
 * The implementation does the immutable copy of the provided collection, otherwise,
 * we could end up with a modified collection under the hood because the volatile field
 * inside {@link AtomicReference} ensures the visibility the list itself, and not the
 * modified fields inside the list.
 *
 * @param <T> type of data stored inside the collection.
 */
public class AtomicStorage<T> implements Storage<T> {

    private final AtomicReference<List<T>> holder = new AtomicReference<>(List.of());

    @Override
    public void set(List<T> data) {
        // immutability checking is for free if the collection is already
        // created as an immutable list: List.of(..)
        List<T> immutableCopy = List.copyOf(data);
        holder.set(immutableCopy);
    }

    @Override
    public List<T> get() {
        return holder.get();
    }
}
