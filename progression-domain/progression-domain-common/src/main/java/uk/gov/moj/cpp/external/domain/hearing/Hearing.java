package uk.gov.moj.cpp.external.domain.hearing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(NON_NULL)
public class Hearing implements Serializable {

    private static final long serialVersionUID = 2886748203741860886L;
    private final UUID id;
    private final String type;
    private final UUID courtCentreId;
    private String courtCentreName;
    private final UUID courtRoomId;
    private String courtRoomName;
    private final Judge judge;
    private final List<String> hearingDays;
    private final List<Defendant> defendants;


    @JsonCreator
    public Hearing(@JsonProperty("id") final UUID id,
                   @JsonProperty("courtCentreId") final UUID courtCentreId,
                   @JsonProperty("courtRoomId") final UUID courtRoomId,
                   @JsonProperty("judge") final Judge judge,
                   @JsonProperty("type") final String type,
                   @JsonProperty("hearingDays") final List<String> hearingDays,
                   @JsonProperty("defendants") final List<Defendant> defendants) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.judge = judge;
        this.type = type;
        this.hearingDays = hearingDays;
        this.defendants = (null == defendants) ? new ArrayList<>() : new ArrayList<>(defendants);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getType() {
        return type;
    }


    public List<String> getHearingDays() {
        return hearingDays;
    }

    public List<Defendant> getDefendants() {
        return new ArrayList<>(defendants);
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public Judge getJudge() {
        return judge;
    }

    public void setCourtCentreName(final String courtCentreName) {
        this.courtCentreName = courtCentreName;
    }

    public void setCourtRoomName(final String courtRoomName) {
        this.courtRoomName = courtRoomName;
    }

}
