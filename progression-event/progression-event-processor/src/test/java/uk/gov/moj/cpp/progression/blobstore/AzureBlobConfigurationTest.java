package uk.gov.moj.cpp.progression.blobstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

public class AzureBlobConfigurationTest {

    @Test
    public void shouldReturnTrueWhenConnectionStringIsPresent() throws Exception {
        final AzureBlobConfiguration config = new AzureBlobConfiguration();
        setConnectionString(config, "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1");

        assertThat(config.hasConnectionString(), is(true));
    }

    @Test
    public void shouldReturnFalseWhenConnectionStringIsNull() {
        final AzureBlobConfiguration config = new AzureBlobConfiguration();

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenConnectionStringIsBlank() throws Exception {
        final AzureBlobConfiguration config = new AzureBlobConfiguration();
        setConnectionString(config, "   ");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenConnectionStringIsDefaultAzureCredentialSentinel() throws Exception {
        final AzureBlobConfiguration config = new AzureBlobConfiguration();
        setConnectionString(config, "DefaultAzureCredential");

        assertThat(config.hasConnectionString(), is(false));
    }

    private void setConnectionString(final AzureBlobConfiguration config, final String value) throws Exception {
        final Field field = AzureBlobConfiguration.class.getDeclaredField("connectionString");
        field.setAccessible(true);
        field.set(config, value);
    }
}
