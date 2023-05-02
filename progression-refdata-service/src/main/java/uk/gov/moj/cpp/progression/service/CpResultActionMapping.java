package uk.gov.moj.cpp.progression.service;

import java.io.Serializable;
import java.util.UUID;

public class CpResultActionMapping implements Serializable {


    private String resultActionCode;
    private UUID resultRefId;

    public CpResultActionMapping(final String resultActionCode, final UUID resultRefId) {
        this.resultActionCode = resultActionCode;
        this.resultRefId = resultRefId;
    }

    public String getResultActionCode() {
        return resultActionCode;
    }

    public UUID getResultRefId() {
        return resultRefId;
    }

}
