package uk.gov.moj.cpp.progression.query;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
        if (isNull(hearingEntity)) {
            return JsonEnvelope.envelopeFrom(
                    envelope.metadata(),
                    createObjectBuilder().add("judicialChildResults",jsonArrayBuilder.build()).build());
        }
        final Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        findJudicialResultFromProsecutionCase(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplication(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplicationOffence(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplicationCourtOrderOffence(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);

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

    private final static DateTimeFormatter END_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Handles("progression.query.judicial-child-results-v2")
    public JsonEnvelope getJudicialChildResultsV2(final JsonEnvelope envelope) {
        final List<JudicialResult> judicialChildResult = new ArrayList<>();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final UUID hearingId = getUUID(envelope.payloadAsJsonObject(), "hearingId").orElseThrow(() -> new IllegalArgumentException("No hearingId Supplied"));
        final UUID masterDefendantId = getUUID(envelope.payloadAsJsonObject(), "masterDefendantId").orElseThrow(() -> new IllegalArgumentException("No masterDefendantId Supplied"));
        final UUID judicialResultTypeId = getUUID(envelope.payloadAsJsonObject(), "judicialResultTypeId").orElseThrow(() -> new IllegalArgumentException("No judicialResultTypeId Supplied"));

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        if (isNull(hearingEntity)) {
            return JsonEnvelope.envelopeFrom(
                    envelope.metadata(),
                    createObjectBuilder().add("judicialChildResults",jsonArrayBuilder.build()).build());
        }
        final Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        findJudicialResultFromProsecutionCaseV2(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplicationV2(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplicationOffenceV2(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        findJudicialResultFromApplicationCourtOrderOffenceV2(hearing, masterDefendantId, judicialResultTypeId, judicialChildResult);
        final LocalDate latestEndDate = getLatestEndDate(hearing, masterDefendantId, judicialResultTypeId);

        judicialChildResult.forEach(childResult ->
                jsonArrayBuilder.add(createObjectBuilder()
                        .add("judicialResultId", childResult.getJudicialResultId().toString())
                        .add("judicialResultTypeId", childResult.getJudicialResultTypeId().toString())
                        .add("label", childResult.getLabel())
                        .build()));

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                createObjectBuilder()
                        .add("judicialChildResults",jsonArrayBuilder.build())
                        .add("latestEndDate", latestEndDate.toString())
                        .build());

    }

    private LocalDate getLatestEndDate(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId) {
        final AtomicReference<LocalDate> latestEndDate = new AtomicReference<>();
        findLatestEndDateFromProsecutionCase(hearing, masterDefendantId, judicialResultTypeId).ifPresent(date -> latestEndDate.set(date));
        findLatestEndDateFromApplication(hearing, masterDefendantId, judicialResultTypeId).ifPresent(date -> {
            if (isNull(latestEndDate.get()) || date.isAfter(latestEndDate.get())) {
                latestEndDate.set(date);
            }
        });
        findLatestEndDateFromApplicationOffence(hearing, masterDefendantId, judicialResultTypeId).ifPresent(date -> {
            if (isNull(latestEndDate.get()) || date.isAfter(latestEndDate.get())) {
                latestEndDate.set(date);
            }
        });
        findLatestEndDateFromApplicationCourtOrderOffence(hearing, masterDefendantId, judicialResultTypeId).ifPresent(date -> {
            if (isNull(latestEndDate.get()) || date.isAfter(latestEndDate.get())) {
                latestEndDate.set(date);
            }
        });

        return latestEndDate.get();
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
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getJudicialResultId)
                    .findFirst();

            if (judicialResultId.isPresent()) {
                final List<JudicialResult> judicialResults = hearing.getProsecutionCases().stream()
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId.get());
            }
        }
    }

    private void findJudicialResultFromProsecutionCaseV2(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getProsecutionCases())) {
            final List<UUID> judicialResultIds = hearing.getProsecutionCases().stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                    .map(Defendant::getOffences)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getJudicialResultId)
                    .toList();

            judicialResultIds.forEach(judicialResultId->{
                final List<JudicialResult> judicialResults = hearing.getProsecutionCases().stream()
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId);
            });
        }
    }

    private Optional<LocalDate> findLatestEndDateFromProsecutionCase(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId) {
        if (nonNull(hearing.getProsecutionCases())) {
           return hearing.getProsecutionCases().stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId))
                    .map(Defendant::getOffences)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getDurationElement)
                    .filter(Objects::nonNull)
                    .map(JudicialResultPromptDurationElement::getDurationEndDate)
                    .map(d-> LocalDate.parse(d, END_DATE_FORMATTER))
                    .max(Comparator.naturalOrder());
        }

        return Optional.empty();
    }

    private void findJudicialResultFromApplication(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final Optional<UUID> judicialResultId = hearing.getCourtApplications().stream()
                    .filter(c-> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getJudicialResultId)
                    .findFirst();

            if (judicialResultId.isPresent()) {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId.get());
            }
        }
    }

    private void findJudicialResultFromApplicationV2(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final List<UUID> judicialResultIds = hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getJudicialResultId)
                    .toList();

            judicialResultIds.forEach(judicialResultId -> {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId);
            });
        }
    }

    private Optional<LocalDate> findLatestEndDateFromApplication(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId) {
        if (nonNull(hearing.getCourtApplications())) {
            return hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getDurationElement)
                    .filter(Objects::nonNull)
                    .map(JudicialResultPromptDurationElement::getDurationEndDate)
                    .map(d->LocalDate.parse(d, END_DATE_FORMATTER))
                    .max(Comparator.naturalOrder());
        }
        return Optional.empty();

    }

    private void findJudicialResultFromApplicationOffence(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final Optional<UUID> judicialResultId = hearing.getCourtApplications().stream()
                    .filter(c-> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtApplicationCases)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtApplicationCase::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .filter(Objects::nonNull)
                    .map(JudicialResult::getJudicialResultId)
                    .findFirst();

            if (judicialResultId.isPresent()) {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getCourtApplicationCases)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(CourtApplicationCase::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId.get());
            }
        }
    }

    private void findJudicialResultFromApplicationOffenceV2(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final List<UUID> judicialResultIds = hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtApplicationCases)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtApplicationCase::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .filter(Objects::nonNull)
                    .map(JudicialResult::getJudicialResultId)
                    .toList();

            judicialResultIds.forEach(judicialResultId -> {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getCourtApplicationCases)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(CourtApplicationCase::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId);
            });
        }
    }

    private Optional<LocalDate> findLatestEndDateFromApplicationOffence(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId) {
        if (nonNull(hearing.getCourtApplications())) {
            return hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtApplicationCases)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtApplicationCase::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getDurationElement)
                    .filter(Objects::nonNull)
                    .map(JudicialResultPromptDurationElement::getDurationEndDate)
                    .map(d->LocalDate.parse(d, END_DATE_FORMATTER))
                    .max(Comparator.naturalOrder());
        }
        return Optional.empty();
    }


    private void findJudicialResultFromApplicationCourtOrderOffence(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final Optional<UUID> judicialResultId = hearing.getCourtApplications().stream()
                    .filter(c-> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtOrder)
                    .filter(Objects::nonNull)
                    .map(CourtOrder::getCourtOrderOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtOrderOffence::getOffence)
                    .filter(Objects::nonNull)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .filter(Objects::nonNull)
                    .map(JudicialResult::getJudicialResultId)
                    .findFirst();

            if (judicialResultId.isPresent()) {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getCourtOrder)
                        .filter(Objects::nonNull)
                        .map(CourtOrder::getCourtOrderOffences)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(CourtOrderOffence::getOffence)
                        .filter(Objects::nonNull)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId.get());
            }
        }
    }

    private void findJudicialResultFromApplicationCourtOrderOffenceV2(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId, final List<JudicialResult> judicialChildResult) {
        if (nonNull(hearing.getCourtApplications())) {
            final List<UUID> judicialResultIds = hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtOrder)
                    .filter(Objects::nonNull)
                    .map(CourtOrder::getCourtOrderOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtOrderOffence::getOffence)
                    .filter(Objects::nonNull)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .filter(Objects::nonNull)
                    .map(JudicialResult::getJudicialResultId)
                    .toList();

            judicialResultIds.forEach(judicialResultId -> {
                final List<JudicialResult> judicialResults = hearing.getCourtApplications().stream()
                        .map(CourtApplication::getCourtOrder)
                        .filter(Objects::nonNull)
                        .map(CourtOrder::getCourtOrderOffences)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(CourtOrderOffence::getOffence)
                        .filter(Objects::nonNull)
                        .map(Offence::getJudicialResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .toList();

                findChildResults(judicialResults, judicialChildResult, judicialResultId);
            });
        }
    }

    private Optional<LocalDate> findLatestEndDateFromApplicationCourtOrderOffence(final Hearing hearing, final UUID masterDefendantId, final UUID judicialResultTypeId) {
        if (nonNull(hearing.getCourtApplications())) {
            return hearing.getCourtApplications().stream()
                    .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant())
                            && masterDefendantId.equals(c.getSubject().getMasterDefendant().getMasterDefendantId()))
                    .map(CourtApplication::getCourtOrder)
                    .filter(Objects::nonNull)
                    .map(CourtOrder::getCourtOrderOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(CourtOrderOffence::getOffence)
                    .filter(Objects::nonNull)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(judicialResultTypeId))
                    .map(JudicialResult::getDurationElement)
                    .filter(Objects::nonNull)
                    .map(JudicialResultPromptDurationElement::getDurationEndDate)
                    .map(d->LocalDate.parse(d, END_DATE_FORMATTER))
                    .max(Comparator.naturalOrder());
        }

        return Optional.empty();
    }

    private void findChildResults(final List<JudicialResult> judicialResults, final List<JudicialResult> childJudicialResults, final UUID judicialResultId) {
        judicialResults.forEach(judicialResult -> {
            if (judicialResultId.equals(judicialResult.getRootJudicialResultId()) && !judicialResultId.equals(judicialResult.getJudicialResultId())) {
                childJudicialResults.add(judicialResult);
                findChildResults(judicialResults, childJudicialResults, judicialResult.getJudicialResultId());
            }
        });
    }
}

