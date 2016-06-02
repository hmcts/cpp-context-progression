package uk.gov.moj.cpp.progression.it;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractIT {

    protected Properties prop;
    protected RequestSpecification reqSpec;
    protected String baseUri;

  	public AbstractIT() {
        readConfig();
        setRequestSpecification();
    }

    private void readConfig() {
        prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("endpoint.properties");
        try {
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String baseUriProp = System.getProperty("baseUri");
        baseUri = (StringUtils.isNotEmpty(baseUriProp) ? baseUriProp : prop.getProperty("base-uri"));
    }

    private void setRequestSpecification() {
        reqSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
    }
    
    
}
