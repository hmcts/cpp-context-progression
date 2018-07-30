package uk.gov.moj.cpp.external.domain.hearing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public class Case implements Serializable{

    private static final long serialVersionUID = 1266830072913532647L;

    private UUID caseId;
    private String urn;

    public Case(UUID caseId, String urn) {
        super();
        this.caseId = caseId;
        this.urn = urn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

}
