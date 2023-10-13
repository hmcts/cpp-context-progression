package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.inject.Inject;
import javax.json.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;

@ServiceComponent(EVENT_LISTENER)
public class DefendantsAddedToCourtProceedingsListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private HearingRepository hearingRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantsAddedToCourtProceedingsListener.class);

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void processDefendantsAddedToCourtProceedings(final JsonEnvelope event) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.defendants-added-to-court-proceedings {} ", event.toObfuscatedDebugString());
        }
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class);

        for (final uk.gov.justice.core.courts.Defendant defendant: defendantsAddedToCourtProceedings.getDefendants()) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendant.getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

            filterDuplicateOffencesById(defendant.getOffences());
            prosecutionCase.getDefendants().add(dedupAllReportingRestrictions(defendant));
            prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
            repository.save(prosecutionCaseEntity);
        }
    }

    @Handles("progression.event.new-defendant-added-to-hearing")
    public void addNewDefendantToHearing(final JsonEnvelope event) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("received in event listener progression.event.new-defendant-added-to-hearing {} ", event.toObfuscatedDebugString());
        }
        final NewDefendantAddedToHearing newDefendantAddedToHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), NewDefendantAddedToHearing.class);
        final HearingEntity hearingEntity = hearingRepository.findBy(newDefendantAddedToHearing.getHearingId());
        final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
        final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
        hearing.getProsecutionCases().stream().filter(prosecutionCase -> prosecutionCase.getId().equals(newDefendantAddedToHearing.getProsecutionCaseId()))
                .forEach(prosecutionCase -> prosecutionCase.getDefendants().addAll(newDefendantAddedToHearing.getDefendants()));
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        hearingRepository.save(hearingEntity);
    }

    private void filterDuplicateOffencesById(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }
}
