package uk.gov.moj.cpp.external.listing.event;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseSentForListing implements Serializable {
    public static final String EVENT_NAME = "public.listing.case-sent-for-listing";
    public static final String CASE_ID = "caseId";
    private static final long serialVersionUID = -8750561493799034049L;
    private final UUID caseId;

    @JsonCreator
    public CaseSentForListing(@JsonProperty(CASE_ID) final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
