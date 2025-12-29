package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.court.HearingAddMissingResultsBdf;
import uk.gov.moj.cpp.progression.court.HearingResultedBdf;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingResultedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.event.hearing-resulted-bdf")
    public void processHearingInitiatedEnrichedEvent(final Envelope<HearingResultedBdf> resultHearingBdfEnvelope) {
        final UUID hearingId = resultHearingBdfEnvelope.payload().getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.hearing-resulted-by-bdf in listener for hearing id {} ", hearingId);
        }
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingEntity)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("resulting hearing with id: {} ", hearingId);
            }
                hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
                hearingRepository.save(hearingEntity);
        }
    }

    @Handles("progression.event.hearing-add-missing-results-bdf")
    public void processHearingAddMissingResults(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final HearingAddMissingResultsBdf eventPayload = jsonObjectToObjectConverter.convert(payload, HearingAddMissingResultsBdf.class);
        final UUID hearingId = eventPayload.getHearingId();
        final UUID prosecutionCaseId = eventPayload.getHearingId();
        final UUID defendantId = eventPayload.getHearingId();
        final UUID offenceId = eventPayload.getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.hearing-add-missing-results-bdf in listener for Hearing Id {}, Case Id: {}, Defendant Id {}, Offence Id {} ",
                    hearingId, prosecutionCaseId, defendantId, offenceId);
        }

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);
        final JsonObject dbHearingJson = jsonFromString(dbHearingEntity.getPayload());
        final Hearing dbtHearing = jsonObjectToObjectConverter.convert(dbHearingJson, Hearing.class);

        final Hearing updatedHearingWithAddedMissingResults = Hearing.hearing().withValuesFrom(dbtHearing)
                .withProsecutionCases(updatedProsecutionCases(dbtHearing, eventPayload))
                .build();

        final String updatedHearingWithAddedMissingResultsPayload = objectToJsonObjectConverter.convert(updatedHearingWithAddedMissingResults).toString();
        dbHearingEntity.setPayload(updatedHearingWithAddedMissingResultsPayload);
        hearingRepository.save(dbHearingEntity);
    }

    private List<ProsecutionCase> updatedProsecutionCases(final Hearing dbtHearing, final HearingAddMissingResultsBdf eventPayload) {
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>(dbtHearing.getProsecutionCases());
        prosecutionCases.removeIf(dbCase -> dbCase.getId().equals(eventPayload.getProsecutionCaseId()));

        final ProsecutionCase updatedProsecutionCase = getUpdatedProsecutionCase(dbtHearing, eventPayload);
        prosecutionCases.add(updatedProsecutionCase);
        return prosecutionCases;
    }

    private ProsecutionCase getUpdatedProsecutionCase(final Hearing dbtHearing, final HearingAddMissingResultsBdf eventPayload) {
        final Optional<ProsecutionCase> optionalDbProsecutionCase = dbtHearing.getProsecutionCases().stream()
                .filter(prosecutionCase -> prosecutionCase.getId().equals(eventPayload.getProsecutionCaseId()))
                .findFirst();
        if(optionalDbProsecutionCase.isPresent()){
            final List<Defendant> defendants = new ArrayList<>(optionalDbProsecutionCase.get().getDefendants());
            final Optional<Defendant> optionalDefendant = optionalDbProsecutionCase.get().getDefendants().stream()
                    .filter(defendant -> defendant.getId().equals(eventPayload.getDefendantId()))
                    .findFirst();
            if (optionalDefendant.isPresent()){
                defendants.removeIf(defendant -> defendant.getId().equals(eventPayload.getDefendantId()));
                final Defendant defendant = getUpdatedDefendant(optionalDefendant.get(), eventPayload);
                defendants.add(defendant);
                return ProsecutionCase.prosecutionCase().withValuesFrom(optionalDbProsecutionCase.get())
                        .withDefendants(defendants)
                        .build();
            }
        }
        return null;
    }

    private Defendant getUpdatedDefendant(final Defendant dBdefendant, final HearingAddMissingResultsBdf eventPayload) {
        final List<JudicialResult> defendantJudicialResult = new ArrayList<>();
        if(isNotEmpty(eventPayload.getDefendantCaseJudicialResults())){
            defendantJudicialResult.addAll(eventPayload.getDefendantCaseJudicialResults());
        }

        if (isNotEmpty(dBdefendant.getDefendantCaseJudicialResults())) {
            defendantJudicialResult.addAll(dBdefendant.getDefendantCaseJudicialResults());
        }

        return Defendant.defendant().withValuesFrom(dBdefendant)
                .withDefendantCaseJudicialResults(defendantJudicialResult)
                .withOffences(getUpdatedOffences(dBdefendant, eventPayload))
                .build();
    }

    private List<Offence> getUpdatedOffences(final Defendant dBdefendant, final HearingAddMissingResultsBdf eventPayload) {
        final List<Offence> offences = new ArrayList<>(dBdefendant.getOffences());
        final Optional<Offence> optionalOffence = dBdefendant.getOffences().stream().filter(offence -> offence.getId().equals(eventPayload.getOffenceId())).findFirst();
        if (optionalOffence.isPresent()){
            offences.removeIf(offence -> offence.getId().equals(eventPayload.getOffenceId()));
            final Offence offence = getUpdatedOffence(optionalOffence.get(), eventPayload);
            offences.add(offence);
        }
        return offences;
    }

    private Offence getUpdatedOffence(final Offence dBOffence, final HearingAddMissingResultsBdf eventPayload) {
        final List<JudicialResult> offenceJudicialResult = new ArrayList<>();
        if (isNotEmpty(eventPayload.getOffenceJudicialResults())){
            offenceJudicialResult.addAll(eventPayload.getOffenceJudicialResults());
        }
        if (isNotEmpty(dBOffence.getJudicialResults())){
            offenceJudicialResult.addAll(dBOffence.getJudicialResults());
        }

        return Offence.offence().withValuesFrom(dBOffence)
                .withJudicialResults(offenceJudicialResult)
                .build();
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }
}
