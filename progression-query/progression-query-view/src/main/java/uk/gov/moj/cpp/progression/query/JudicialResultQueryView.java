package uk.gov.moj.cpp.progression.query;


import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class JudicialResultQueryView {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.query.judicial-child-results")
    public JsonEnvelope getJudicialChildResults(final JsonEnvelope envelope) {
        final List<JudicialResult> judicialChildResult = new ArrayList<>();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final UUID hearingId = getUUID(envelope.payloadAsJsonObject(), "hearingId").orElseThrow(() -> new IllegalArgumentException("No hearingId Supplied"));
        final UUID masterDefendantId = getUUID(envelope.payloadAsJsonObject(), "masterDefendantId").orElseThrow(() -> new IllegalArgumentException("No masterDefendantId Supplied"));
        final UUID judicialResultTypeId = getUUID(envelope.payloadAsJsonObject(), "judicialResultTypeId").orElseThrow(() -> new IllegalArgumentException("No judicialResultTypeId Supplied"));

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        final Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        findJudicialResultFromProsecutionCase(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);

        judicialChildResult.forEach(childResult ->
                jsonArrayBuilder.add(createObjectBuilder()
                        .add("judicialResultId", childResult.getJudicialResultId().toString())
                        .add("judicialResultTypeId", childResult.getJudicialResultTypeId().toString())
                        .add("label", childResult.getLabel())
                        .build()));
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                createObjectBuilder().add("judicialChildResults",jsonArrayBuilder.build()).build());


    }

    private void findJudicialResultFromProsecutionCase(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getProsecutionCases())) {
            final Optional<UUID> judicialResultId = hearing.getProsecutionCases().stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                    .map(Defendant::getOffences)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getJudicialResultId)
                    .findFirst();

            if (judicialResultId.isPresent()) {
                final List<JudicialResult> judicialResults = hearing.getProsecutionCases().stream()
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                        .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getJudicialResults)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId.get());
            }
        }
    }


    private void findChildResults(final List<JudicialResult> judicialResults, final List<JudicialResult> childJudicialResults, final UUID judicialResultId) {
        judicialResults.forEach(judicialResult -> {
            if (judicialResult.getRootJudicialResultId().equals(judicialResultId) && !judicialResult.getJudicialResultId().equals(judicialResultId)) {
                childJudicialResults.add(judicialResult);
                findChildResults(judicialResults, childJudicialResults, judicialResult.getJudicialResultId());
            }
        });
    }
}

