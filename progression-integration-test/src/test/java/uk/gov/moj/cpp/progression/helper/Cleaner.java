package uk.gov.moj.cpp.progression.helper;

import java.io.Closeable;

public class Cleaner {

    public static void closeSilently(final AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                //suppress exception
            }
        }
    }

    public static void closeSilently(final Closeable closeable){
        closeSilently((AutoCloseable)closeable);
    }
}
