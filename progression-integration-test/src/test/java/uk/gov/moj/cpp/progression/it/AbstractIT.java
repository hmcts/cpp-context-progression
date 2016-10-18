package uk.gov.moj.cpp.progression.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;

public class AbstractIT {

    protected Properties prop;
    protected RequestSpecification reqSpec;
    protected String baseUri;
    protected String HOST = "localhost";
    protected int PORT = 8080;

    public AbstractIT() {
        readConfig();
        setRequestSpecification();
    }

    private void readConfig() {
        prop = new Properties();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final InputStream stream = loader.getResourceAsStream("endpoint.properties");
        try {
            prop.load(stream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        final String configuredHost = System.getProperty("INTEGRATION_HOST_KEY");
        if (StringUtils.isNotBlank(configuredHost)) {
            HOST = configuredHost;
        }
        baseUri = (StringUtils.isNotEmpty(HOST) ? "http://" + HOST + ":" + PORT
                        : prop.getProperty("base-uri"));
    }

    private void setRequestSpecification() {
        reqSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
    }

    protected void waitForResponse(final int i) throws InterruptedException {
        TimeUnit.SECONDS.sleep(i);
    }
}
