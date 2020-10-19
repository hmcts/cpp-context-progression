package uk.gov.justice.services;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.BoxWorkTaskStatus;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HearingMapper {


    private HearingDatesMapper hearingDatesMapper = new HearingDatesMapper();

    private HearingDaysMapper hearingDaysMapper = new HearingDaysMapper();

    public uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearing(final ProsecutionCaseDefendantListingStatusChanged listingStatusChanged,
                                                                                final List<String> defendantIds) {
        final Hearing hearing = listingStatusChanged.getHearing();
        final UUID boxWorkAssignedUserId = listingStatusChanged.getBoxWorkAssignedUserId();
        final BoxWorkTaskStatus boxWorkTaskStatus = listingStatusChanged.getBoxWorkTaskStatus();
        final CourtCentre courtCentre = hearing.getCourtCentre();
        uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex
                = new uk.gov.justice.services.unifiedsearch.client.domain.Hearing();
        hearingIndex = populateCourtCentre(courtCentre, hearingIndex);
        populateBoxWorkDetails(boxWorkAssignedUserId, boxWorkTaskStatus, hearingIndex);
        populateHearingDetails(defendantIds, hearing, hearingIndex);

        return hearingIndex;
    }

    private void populateHearingDetails(final List<String> defendantIds,
                                        final Hearing hearing,
                                        final uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex) {
        hearingIndex.setDefendantIds(defendantIds);
        hearingIndex.setIsIsBoxHearing(hearing.getIsBoxHearing() == null ? false : hearing.getIsBoxHearing());
        hearingIndex.setHearingDays(hearingDaysMapper.extractHearingDays(hearing));
        hearingIndex.setHearingDates(hearingDatesMapper.extractHearingDates(hearing));
        hearingIndex.setJudiciaryTypes(judiciaryTypes(hearing));
        if (hearing.getId() != null) {
            hearingIndex.setHearingId(hearing.getId().toString());
        }

        if (hearing.getType() != null && hearing.getType().getId() != null) {
            hearingIndex.setHearingTypeId(hearing.getType().getId().toString());
        }

        if (hearing.getType() != null && hearing.getType().getDescription() != null) {
            hearingIndex.setHearingTypeLabel(hearing.getType().getDescription().toString());
        }

        if (hearing.getJurisdictionType() != null) {
            hearingIndex.setJurisdictionType(hearing.getJurisdictionType().toString());
        }
    }

    private void populateBoxWorkDetails(final UUID boxWorkAssignedUserId, BoxWorkTaskStatus boxWorkTaskStatus,
                                        final uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex) {
        if (boxWorkAssignedUserId != null) {
            hearingIndex.setBoxWorkAssignedUserId(boxWorkAssignedUserId.toString());
        }
        if (boxWorkTaskStatus != null) {
            hearingIndex.setBoxWorkTaskStatus(boxWorkTaskStatus.toString());
        }
    }

    private List<String> judiciaryTypes(final Hearing hearing) {
        final List<String> judiciaryTypes = new ArrayList<>();
        final List<JudicialRole> judicialRoles = hearing.getJudiciary();
        if (judicialRoles != null) {
            for (final JudicialRole judicialRole : judicialRoles) {
                final JudicialRoleType judicialRoleType = judicialRole.getJudicialRoleType();
                if (judicialRoleType != null) {
                    judiciaryTypes.add(judicialRoleType.getJudiciaryType());
                }
            }
        }
        return judiciaryTypes;
    }

    public uk.gov.justice.services.unifiedsearch.client.domain.Hearing populateCourtCentre(final CourtCentre courtCentre,
                                                                                           final uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex) {
        ofNullable(courtCentre.getName()).ifPresent(hearingIndex::setCourtCentreName);
        ofNullable(courtCentre.getRoomName()).ifPresent(hearingIndex::setCourtCentreRoomName);
        ofNullable(courtCentre.getWelshName()).ifPresent(hearingIndex::setCourtCentreWelshName);
        ofNullable(courtCentre.getWelshRoomName()).ifPresent(hearingIndex::setCourtCentreRoomWelshName);
        ofNullable(courtCentre.getCode()).ifPresent(hearingIndex::setCourtCentreCode);

        if (courtCentre.getId() != null) {
            hearingIndex.setCourtId(courtCentre.getId().toString());
        }
        if (courtCentre.getRoomId() != null) {
            hearingIndex.setCourtCentreRoomId(courtCentre.getRoomId().toString());
        }
        return hearingIndex;
    }
}
