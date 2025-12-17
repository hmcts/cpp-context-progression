package uk.gov.moj.cpp.progression.domain.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("squid:S2384")
public class Subscription {

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("channelProperties")
    private Map<String, String> channelProperties = new HashMap<>();

    @JsonProperty("userGroups")
    private List<String> userGroups = new ArrayList<>();

    @JsonProperty("courtCentreIds")
    private List<UUID> courtCentreIds = new ArrayList<>();

    @JsonProperty("nowTypeIds")
    private List<UUID> nowTypeIds = new ArrayList<>();

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Map<String, String> getChannelProperties() {
        return channelProperties;
    }

    public void setChannelProperties(Map<String, String> channelProperties) {
        this.channelProperties = channelProperties;
    }

    public List<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<String> userGroups) {
        this.userGroups = userGroups;
    }

    public List<UUID> getCourtCentreIds() {
        return courtCentreIds;
    }

    public void setCourtCentreIds(List<UUID> courtCentreIds) {
        this.courtCentreIds = courtCentreIds;
    }

    public List<UUID> getNowTypeIds() {
        return nowTypeIds;
    }

    public void setNowTypeIds(List<UUID> nowTypeIds) {
        this.nowTypeIds = nowTypeIds;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "channel='" + channel + '\'' +
                ", destination='" + destination + '\'' +
                ", channelProperties=" + channelProperties +
                ", userGroups=" + userGroups +
                ", courtCentreIds=" + courtCentreIds +
                ", nowTypeIds=" + nowTypeIds +
                '}';
    }
}
