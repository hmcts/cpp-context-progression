package uk.gov.moj.cpp.progression.service;

import uk.gov.moj.cpp.progression.service.exception.SecureConnectionException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientWrapper.class);

    public CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

    public CloseableHttpClient createSecureHttpClient() {
        return HttpClients.custom()
                .setSSLSocketFactory(getSslConnection())
                .build();
    }

    /**
     * Solution obtained from post: https://github.com/Azure/azure-functions-java-library/issues/105
     */
    public static SSLConnectionSocketFactory getSslConnection() {
        try {
            final KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);
            final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(ks, acceptingTrustStrategy).build();
            return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
            LOGGER.error("Unable to set up secure socket connection");
            throw new SecureConnectionException("Unable to set up secure socket connection", e);
        }
    }
}

