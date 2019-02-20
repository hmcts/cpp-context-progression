package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SharedHearing;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class HearingResultEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.hearing-resulted")
    public void updateHearingResult(final JsonEnvelope event) {
        final HearingResulted hearingResulted = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingResulted.getHearing().getId());
        final Set<HearingResultLineEntity> hearingResultLineEntities = hearingEntity.getResultLines();
        addHearingResultLines(hearingResultLineEntities, hearingEntity, hearingResulted.getHearing().getSharedResultLines());
        final Hearing hearing = tranformSharedHearing(hearingResulted.getHearing());
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        hearingRepository.save(hearingEntity);
    }

    private Hearing tranformSharedHearing(final SharedHearing sharedHearing) {
        return Hearing.hearing()
                .withId(sharedHearing.getId())
                .withJurisdictionType(JurisdictionType.valueOf(sharedHearing.getJurisdictionType().toString()))
                .withDefendantAttendance(sharedHearing.getDefendantAttendance())
                .withProsecutionCounsels(sharedHearing.getProsecutionCounsels())
                .withCourtCentre(sharedHearing.getCourtCentre())
                .withHearingDays(sharedHearing.getHearingDays())
                .withDefenceCounsels(sharedHearing.getDefenceCounsels())
                .withReportingRestrictionReason(sharedHearing.getReportingRestrictionReason())
                .withType(sharedHearing.getType())
                .withProsecutionCases(sharedHearing.getProsecutionCases())
                .withHearingLanguage(HearingLanguage.valueOf(sharedHearing.getHearingLanguage().toString()))
                .withJudiciary(sharedHearing.getJudiciary())
                .withDefendantReferralReasons(sharedHearing.getDefendantReferralReasons())
                .withHasSharedResults(sharedHearing.getHasSharedResults())
                .withHearingCaseNotes(sharedHearing.getHearingCaseNotes())
                .build();
    }

    private void addHearingResultLines(Set<HearingResultLineEntity> hearingResultLineEntities, HearingEntity hearingEntity, List<SharedResultLine> sharedResultLines) {
        hearingEntity.getResultLines().clear();
        for (final SharedResultLine sharedResultLine : sharedResultLines) {
            final JsonObject sharedResultLineJson = objectToJsonObjectConverter.convert(sharedResultLine);
            hearingResultLineEntities.add(new HearingResultLineEntity(UUID.randomUUID(), sharedResultLineJson.toString(), hearingEntity));
        }
    }
}
