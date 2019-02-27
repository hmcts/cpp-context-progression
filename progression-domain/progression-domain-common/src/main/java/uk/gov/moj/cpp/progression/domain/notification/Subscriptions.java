package uk.gov.moj.cpp.progression.domain.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@SuppressWarnings({"squid:S1700","squid:S2384"})
public class Subscriptions {

    @JsonProperty("subscriptions")
    private List<Subscription> subscriptions;

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
