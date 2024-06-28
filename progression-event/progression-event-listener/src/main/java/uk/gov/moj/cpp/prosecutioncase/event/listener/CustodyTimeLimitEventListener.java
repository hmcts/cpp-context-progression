package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.CustodyTimeLimitClockStopped;
import uk.gov.justice.progression.courts.CustodyTimeLimitExtended;
import uk.gov.justice.progression.courts.ExtendCustodyTimeLimitResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;


@ServiceComponent(EVENT_LISTENER)
public class CustodyTimeLimitEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustodyTimeLimitEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("progression.events.custody-time-limit-clock-stopped")
    public void processCustodyTimeLimitClockStopped(final Envelope<CustodyTimeLimitClockStopped> event) {
        final CustodyTimeLimitClockStopped custodyTimeLimitClockStopped = event.payload();
        final List<UUID> offenceIds = custodyTimeLimitClockStopped.getOffenceIds();
        final List<UUID> caseIds = custodyTimeLimitClockStopped.getCaseIds();

        stopCTLClockForHearing(custodyTimeLimitClockStopped, offenceIds);
        stopCTLClockForProsecutionCases(offenceIds, caseIds);
    }

    @Handles("progression.events.extend-custody-time-limit-resulted")
    public void processExtendCustodyTimeLimitResulted(final Envelope<ExtendCustodyTimeLimitResulted> event) {
        final ExtendCustodyTimeLimitResulted extendCustodyTimeLimitResulted = event.payload();

        final UUID hearingId = extendCustodyTimeLimitResulted.getHearingId();
        final UUID caseId = extendCustodyTimeLimitResulted.getCaseId();
        final UUID offenceId = extendCustodyTimeLimitResulted.getOffenceId();
        final LocalDate extendedTimeLimit = extendCustodyTimeLimitResulted.getExtendedTimeLimit();

        updateCustodyTimeLimitForProsecutionCase(caseId, extendedTimeLimit, offenceId);
        updateCustodyTimeLimitForHearing(hearingId, extendedTimeLimit, offenceId);

    }

    @Handles("progression.events.custody-time-limit-extended")
    public void processCustodyTimeLimitExtended(final Envelope<CustodyTimeLimitExtended> event) {
        final CustodyTimeLimitExtended eventCustodyTimeLimitExtended = event.payload();
        final UUID offenceId = eventCustodyTimeLimitExtended.getOffenceId();
        final LocalDate extendedTimeLimit = eventCustodyTimeLimitExtended.getExtendedTimeLimit();

        eventCustodyTimeLimitExtended.getHearingIds().
                forEach(hearingId -> updateCustodyTimeLimitForHearing(hearingId, extendedTimeLimit, offenceId));

    }


    private void updateCustodyTimeLimitForHearing(final UUID hearingId, final LocalDate extendedTimeLimit, final UUID offenceId) {
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        if(hearingEntity == null){
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hearing can't be found in view store. hearingId : {}", hearingId);
            }
            return;
        }
        final JsonObject dbHearingJsonObject = jsonFromString(hearingEntity.getPayload());

        final Hearing dbHearing = jsonObjectConverter.convert(dbHearingJsonObject, Hearing.class);
        dbHearing.getProsecutionCases().stream().flatMap(pc -> pc.getDefendants().stream())
                .filter(defendant -> defendant.getOffences().stream()
                        .anyMatch(o -> o.getId().equals(offenceId)))
                .forEach(defendant -> updateCustodyTimeLimit(extendedTimeLimit, offenceId, defendant));
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(dbHearing).toString());
        hearingRepository.save(hearingEntity);
    }

    private void updateCustodyTimeLimitForProsecutionCase(final UUID caseId, final LocalDate extendedTimeLimit, final UUID offenceId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject dbProsecutionCaseJsonObject = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(dbProsecutionCaseJsonObject, ProsecutionCase.class);
        prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getOffences().stream()
                        .anyMatch(o -> o.getId().equals(offenceId)))
                .forEach(defendant -> updateCustodyTimeLimit(extendedTimeLimit, offenceId, defendant));
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);
    }

    private void updateCustodyTimeLimit(final LocalDate extendedTimeLimit, final UUID offenceId, final Defendant defendant) {
        final Optional<Offence> offence = defendant.getOffences().stream()
                .filter(o -> o.getId().equals(offenceId))
                .findFirst();
        if (offence.isPresent()) {
            final int index = defendant.getOffences().indexOf(offence.get());
            defendant.getOffences().remove(index);
            defendant.getOffences().add(index, Offence.offence()
                    .withValuesFrom(offence.get())
                    .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                            .withValuesFrom(nonNull(offence.get().getCustodyTimeLimit()) ? offence.get().getCustodyTimeLimit() : CustodyTimeLimit.custodyTimeLimit().build())
                            .withTimeLimit(extendedTimeLimit)
                            .withIsCtlExtended(true)
                            .build())
                    .build());
        }
    }

    private void stopCTLClockForProsecutionCases(final List<UUID> offenceIds, final List<UUID> caseIds) {
        for (final UUID caseId : caseIds) {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
            final JsonObject dbProsecutionCaseJsonObject = jsonFromString(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(dbProsecutionCaseJsonObject, ProsecutionCase.class);
            prosecutionCase.getDefendants().stream()
                    .forEach(defendant -> stopCTLClock(offenceIds, defendant));
            prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
            prosecutionCaseRepository.save(prosecutionCaseEntity);
        }

    }

    private void stopCTLClockForHearing(final CustodyTimeLimitClockStopped custodyTimeLimitClockStopped, final List<UUID> offenceIds) {
        final HearingEntity hearingEntity = hearingRepository.findBy(custodyTimeLimitClockStopped.getHearingId());
        final JsonObject dbHearingJsonObject = jsonFromString(hearingEntity.getPayload());

        final Hearing dbHearing = jsonObjectConverter.convert(dbHearingJsonObject, Hearing.class);
        dbHearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .forEach(defendant -> stopCTLClock(offenceIds, defendant));
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(dbHearing).toString());
        hearingRepository.save(hearingEntity);
    }

    private void stopCTLClock(final List<UUID> offenceIds, final Defendant defendant) {
        final List<Offence> offences = defendant.getOffences().stream()
                .filter(offence -> offenceIds.contains(offence.getId()))
                .collect(Collectors.toList());
        for (final Offence offence : offences) {
            final int index = defendant.getOffences().indexOf(offence);
            defendant.getOffences().remove(offence);
            defendant.getOffences().add(index, Offence.offence()
                    .withValuesFrom(offence)
                    .withCustodyTimeLimit(null)
                    .withCtlClockStopped(Boolean.TRUE)
                    .build());

        }
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
