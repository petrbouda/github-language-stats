package pbouda.github.lang.storage;

import java.util.List;

public interface Storage<T> {

    /**
     * Sets the list of data into the storage.
     *
     * @param data provided data.
     */
    void set(List<T> data);

    /**
     * Retrieves the list of data from the storage.
     *
     * @return stored data from the storage.
     */
    List<T> get();

}
