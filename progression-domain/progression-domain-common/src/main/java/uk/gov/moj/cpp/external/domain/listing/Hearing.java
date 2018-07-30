package uk.gov.moj.cpp.external.domain.listing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("squid:S1067")
@JsonInclude(NON_NULL)
public final class Hearing implements Serializable {

    private static final long serialVersionUID = -1656294577315327959L;
    private UUID id;
    private UUID courtCentreId;
    private String type;
    private String startDate;
    private Integer estimateMinutes;
    private List<Defendant> defendants;
    private final UUID courtRoomId;
    private final UUID judgeId;
    private final String startTime;

    @JsonCreator
    public Hearing(@JsonProperty("id") final UUID id,
                   @JsonProperty("courtCentreId") final UUID courtCentreId,
                   @JsonProperty("type") final String type,
                   @JsonProperty("startDate") final String startDate,
                   @JsonProperty("estimateMinutes") final Integer estimateMinutes,
                   @JsonProperty("defendants") final List<Defendant> defendants,
                   @JsonProperty(value = "courtRoomId") final UUID courtRoomId,
                   @JsonProperty(value = "judgeId") final UUID judgeId,
                   @JsonProperty(value = "startTime") final String startTime) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.defendants = (null == defendants) ? new ArrayList<>() : new ArrayList<>(defendants);
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.startTime = startTime;

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

    public String getStartDate() {
        return startDate;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public List<Defendant> getDefendants() {
        return new ArrayList<>(defendants);
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    public String getStartTime() {
        return startTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Hearing)) { return false; }
        Hearing hearing = (Hearing) o;
        return Objects.equals(getId(), hearing.getId()) &&
                Objects.equals(getCourtCentreId(), hearing.getCourtCentreId()) &&
                Objects.equals(getType(), hearing.getType()) &&
                Objects.equals(getStartDate(), hearing.getStartDate()) &&
                Objects.equals(getEstimateMinutes(), hearing.getEstimateMinutes()) &&
                Objects.equals(getDefendants(), hearing.getDefendants()) &&
                Objects.equals(getCourtRoomId(), hearing.getCourtRoomId()) &&
                Objects.equals(getJudgeId(), hearing.getJudgeId()) &&
                Objects.equals(getStartTime(), hearing.getStartTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCourtCentreId(), getType(), getStartDate(), getEstimateMinutes(), getDefendants(), getCourtRoomId(), getJudgeId(), getStartTime());
    }
}
