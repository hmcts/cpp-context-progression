package uk.gov.justice.services;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;

import java.util.ArrayList;
import java.util.List;

public class HearingMapper {


    private HearingDatesMapper hearingDatesMapper = new HearingDatesMapper();

    private HearingDaysMapper hearingDaysMapper = new HearingDaysMapper();

    public uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearing(final Hearing hearing,
                                                                               final List<String> defendantIds) {
        final CourtCentre courtCentre = hearing.getCourtCentre();
        uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex
                = new uk.gov.justice.services.unifiedsearch.client.domain.Hearing();
        hearingIndex = populateCourtCentre(courtCentre, hearingIndex);
        populateHearingDetails(defendantIds, hearing, hearingIndex);

        return hearingIndex;
    }

    private void populateHearingDetails(final List<String> defendantIds,
                                        final Hearing hearing,
                                        final uk.gov.justice.services.unifiedsearch.client.domain.Hearing hearingIndex) {
        hearingIndex.setDefendantIds(defendantIds);
        hearingIndex.setIsIsBoxHearing(hearing.getIsBoxHearing() != null && hearing.getIsBoxHearing());
        hearingIndex.setIsIsVirtualBoxHearing(hearing.getIsVirtualBoxHearing() != null && hearing.getIsVirtualBoxHearing());
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
            hearingIndex.setHearingTypeLabel(hearing.getType().getDescription());
        }

        if (hearing.getJurisdictionType() != null) {
            hearingIndex.setJurisdictionType(hearing.getJurisdictionType().toString());
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
