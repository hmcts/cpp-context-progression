package uk.gov.moj.cpp.progression.listing.common.service;

import static java.lang.Math.toIntExact;

import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.config.RequestConfig;

/**
 * Time-outs for every call progression makes to courtscheduler. The calls run inside event
 * processors, so without them a courtscheduler that accepts the connection and never answers
 * would hold the thread indefinitely.
 */
@ApplicationScoped
public class CourtSchedulerRequestConfig {

    @Inject
    @Value(key = "courtscheduler.timeout.connect.ms", defaultValue = "5000")
    long connectTimeoutMs = 5000;

    @Inject
    @Value(key = "courtscheduler.timeout.ms", defaultValue = "15000")
    long socketTimeoutMs = 15000;

    private RequestConfig requestConfig;

    public CourtSchedulerRequestConfig() {
    }

    public CourtSchedulerRequestConfig(final long connectTimeoutMs, final long socketTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
        init();
    }

    @PostConstruct
    void init() {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(toIntExact(connectTimeoutMs))
                .setSocketTimeout(toIntExact(socketTimeoutMs))
                .build();
    }

    public RequestConfig get() {
        return requestConfig;
    }
}
