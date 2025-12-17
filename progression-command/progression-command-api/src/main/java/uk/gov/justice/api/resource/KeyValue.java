package uk.gov.justice.api.resource;

public class KeyValue<K, V> {

    private final K key;

    private final V value;

    public KeyValue(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }


}
