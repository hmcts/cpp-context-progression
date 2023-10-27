package uk.gov.moj.cpp.progression.service.dto;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HearingNotificationInputData {

    private UUID hearingId;

    private String hearingType;

    private List<UUID> caseIds;
    private List<UUID> defendantIds;
    private UUID courtCenterId;
    private UUID courtRoomId;
    private String ljaCode;
    private ZonedDateTime hearingDateTime;
    private UUID emailNotificationTemplateId;
    private Map<UUID, List<UUID>> defendantOffenceListMap;

    private String templateName;

    public Map<UUID, List<UUID>> getDefendantOffenceListMap() {
        return defendantOffenceListMap;
    }

    public void setDefendantOffenceListMap(final Map<UUID, List<UUID>> defendantOffenceListMap) {
        this.defendantOffenceListMap = defendantOffenceListMap;
    }

    public UUID getEmailNotificationTemplateId() {
        return emailNotificationTemplateId;
    }

    public void setEmailNotificationTemplateId(final UUID emailNotificationTemplateId) {
        this.emailNotificationTemplateId = emailNotificationTemplateId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public List<UUID> getCaseIds() {
      return Collections.unmodifiableList(caseIds);
    }

    public void setCaseIds(final List<UUID> caseIds) {
        this.caseIds =Collections.unmodifiableList(caseIds);
    }
    public List<UUID> getDefendantIds() {
        return Collections.unmodifiableList(defendantIds);
    }

    public void setDefendantIds(final List<UUID> defendantIds) {
        this.defendantIds=Collections.unmodifiableList(defendantIds);
    }

    public UUID getCourtCenterId() {
        return courtCenterId;
    }

    public void setCourtCenterId(final UUID courtCenterId) {
        this.courtCenterId = courtCenterId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public void setCourtRoomId(final UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public void setLjaCode(final String ljaCode) {
        this.ljaCode = ljaCode;
    }

    public ZonedDateTime getHearingDateTime() {
        return hearingDateTime;
    }

    public void setHearingDateTime(final ZonedDateTime hearingDateTime) {
        this.hearingDateTime = hearingDateTime;
    }

    public String getHearingType() {
        return hearingType;
    }

    public void setHearingType(final String hearingType) {
        this.hearingType = hearingType;
    }

    public String getTemplateName() {
        return templateName;
    }
    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

}
