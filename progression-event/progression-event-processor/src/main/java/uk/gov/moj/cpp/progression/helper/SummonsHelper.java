package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SummonsHelper {

    private static final String PROGRESSION_COMMAND_APPROVE_APPLICATION_SUMMONS = "progression.command.approve-application-summons";
    private static final String PROGRESSION_COMMAND_REJECT_APPLICATION_SUMMONS = "progression.command.reject-application-summons";
    private static final UUID SUMMONS_APPROVED = UUID.fromString("0f44eeb9-2c81-430d-9a60-bbdaf8c4a093");
    private static final UUID SUMMONS_REJECTED = UUID.fromString("d8837a45-8281-49b3-8349-49b423193148");
    private static final String PROSECUTOR_COST = "prosecutorCost";
    private static final String SUMMONS_SUPPRESSED = "summonsSuppressed";
    private static final String PERSONAL_SERVICE = "personalService";
    private static final String PROSECUTOR_EMAIL_ADDRESS = "prosecutorEmailAddress";
    private static final String REASONS = "reasons";
    private static final String APPLICATION_ID = "applicationId";
    private static final String REASONS_FOR_REJECTION = "reasonsForRejection";
    private static final String PROSECUTION_COSTS = "prosecutionCosts";
    private static final String THIS_SUMMONS_WILL_BE_SERVED_BY_A_PROSECUTOR = "thisSummonsWillBeServedByAProsecutor";
    private static final String THIS_SUMMONS_IS_FOR_PERSONAL_SERVICE = "tHISSUMMONSISFORPERSONALSERVICE";
    private static final String PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION = "prosecutorsEmailAddressUsedSummonsNotification";
    private static final String TRUE = "true";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void initiateSummonsProcess(final JsonEnvelope event, final Hearing hearing) {
        final boolean boxWorkHearing = nonNull(hearing.getIsBoxHearing()) && hearing.getIsBoxHearing();

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());

        if (boxWorkHearing) {
            final List<JudicialResult> summonsApprovedJudicialResults = getJudicialResults(hearing, SUMMONS_APPROVED);
            final List<JudicialResult> summonsRejectedJudicialResults = getJudicialResults(hearing, SUMMONS_REJECTED);

            if (isNotEmpty(courtApplications)) {
                courtApplications.forEach(courtApplication -> {
                    if (isNotEmpty(summonsApprovedJudicialResults)) {
                        final JsonObject summonsApprovedPayload = createSummonsApprovedJsonObject(courtApplication, summonsApprovedJudicialResults);
                        sendSummonsCommand(event, summonsApprovedPayload, PROGRESSION_COMMAND_APPROVE_APPLICATION_SUMMONS);
                    }
                    if (isEmpty(summonsApprovedJudicialResults) && isNotEmpty(summonsRejectedJudicialResults)) {
                        final JsonObject summonsRejectedPayload = createSummonsRejectedJsonObject(courtApplication, summonsRejectedJudicialResults);
                        sendSummonsCommand(event, summonsRejectedPayload, PROGRESSION_COMMAND_REJECT_APPLICATION_SUMMONS);
                    }
                });
            }
        }
    }

    private void sendSummonsCommand(final JsonEnvelope jsonEnvelope, final JsonObject payload, final String eventName) {
        sender.send(Enveloper.envelop(payload).withName(eventName).withMetadataFrom(jsonEnvelope));
    }

    private JsonObject createSummonsRejectedJsonObject(final CourtApplication courtApplication, final List<JudicialResult> summonsRejectedJudicialResults) {
        final List<JudicialResultPrompt> judicialResultPrompts = summonsRejectedJudicialResults.get(0).getJudicialResultPrompts();
        return createObjectBuilder()
                .add(APPLICATION_ID, courtApplication.getId().toString())
                .add("summonsRejectedOutcome", createObjectBuilder()
                        .add(REASONS, createArrayBuilder().add(getPromptValue(judicialResultPrompts, REASONS_FOR_REJECTION)))
                        .add(PROSECUTOR_EMAIL_ADDRESS, getPromptValue(judicialResultPrompts, PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION))
                ).build();
    }

    private JsonObject createSummonsApprovedJsonObject(final CourtApplication courtApplication, final List<JudicialResult> summonsApprovedJudicialResults) {
        final List<JudicialResultPrompt> judicialResultPrompts = summonsApprovedJudicialResults.get(0).getJudicialResultPrompts();
        return createObjectBuilder()
                .add(APPLICATION_ID, courtApplication.getId().toString())
                .add("summonsApprovedOutcome", createObjectBuilder()
                        .add(PROSECUTOR_COST, getPromptValue(judicialResultPrompts, PROSECUTION_COSTS))
                        .add(SUMMONS_SUPPRESSED, getPromptValue(judicialResultPrompts, THIS_SUMMONS_WILL_BE_SERVED_BY_A_PROSECUTOR).equalsIgnoreCase(TRUE))
                        .add(PERSONAL_SERVICE, getPromptValue(judicialResultPrompts, THIS_SUMMONS_IS_FOR_PERSONAL_SERVICE).equalsIgnoreCase(TRUE))
                        .add(PROSECUTOR_EMAIL_ADDRESS, getPromptValue(judicialResultPrompts, PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION))
                ).build();
    }

    private List<JudicialResult> getJudicialResults(CourtApplication courtApplication, final UUID resultDefinitionId) {

        final List<JudicialResult> judicialResults1 = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(courtApplicationCase -> ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty))
                .flatMap(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(toList());

        final List<JudicialResult> judicialResults2 = ofNullable(courtApplication.getCourtOrder()).map(courtOrder -> courtOrder.getCourtOrderOffences().stream()).orElseGet(Stream::empty)
                .flatMap(courtOrderOffence -> ofNullable(courtOrderOffence.getOffence()).map(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty))
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(toList());

        final List<JudicialResult> applicationJudicialResults = ofNullable(courtApplication.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(Collectors.toList());

        return Stream.of(applicationJudicialResults, judicialResults1, judicialResults2).flatMap(Collection::stream).collect(Collectors.toList());

    }

    private List<JudicialResult> getJudicialResults(Hearing hearing, final UUID resultDefinitionId) {

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());

        final List<JudicialResult> applicationJudicialResults = courtApplications.stream().map(courtApplication -> getJudicialResults(courtApplication, resultDefinitionId)).flatMap(Collection::stream).collect(Collectors.toList());

        final List<JudicialResult> defendantJudicialResults = ofNullable(hearing.getDefendantJudicialResults())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        final List<JudicialResult> defendantCaseJudicialResults = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .map(defendant -> ofNullable(defendant.getDefendantCaseJudicialResults()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        final List<JudicialResult> judicialResults = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        return Stream.of(applicationJudicialResults, defendantJudicialResults, defendantCaseJudicialResults, judicialResults).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private String getPromptValue(final List<JudicialResultPrompt> judicialResultPrompts, final String promptReference) {
        final Optional<String> value = judicialResultPrompts.stream().
                filter(judicialResultPrompt -> judicialResultPrompt.getPromptReference().equals(promptReference))
                .map(JudicialResultPrompt::getValue)
                .findFirst();

        return value.orElse("");
    }
}
