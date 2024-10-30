package uk.gov.moj.cpp.progression.domain.transformation.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.service.EventPayloadTransformer.DATE_TIME_FORMATTER;
import static uk.gov.moj.cpp.progression.domain.transformation.service.EventPayloadTransformer.JUDICIAL_RESULTS_ATTRIBUTE;
import static uk.gov.moj.cpp.progression.domain.transformation.service.EventPayloadTransformer.JUDICIAL_RESULT_PROMPTS_ATTRIBUTE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.transformation.domain.Prompt;
import uk.gov.moj.cpp.progression.domain.transformation.domain.ResultDefinition;

import java.time.LocalDate;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventPayloadTransformerTest {

    public static final String OFFENCE_JUDICIAL_RESULT_LABEL_VALUE = "offenceJudicialResultLabel";
    public static final String DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE = "defendantJudicialResultLabel";
    public static final String OFFENCE_JUDICIAL_RESULT_PROMPT_LABEL_VALUE = "offenceJudicialResultPromptLabel";
    public static final String DEFENDANT_JUDICIAL_RESULT_PROMPT_LABEL_VALUE = "defendantJudicialResultPromptLabel";
    public static final LocalDate EVENT_DATE = LocalDate.now().minusDays(2);

    @Mock
    private ResultDefinitionService resultDefinitionService;

    @Mock
    private HearingService hearingService;

    @Test
    public void testTransform() {

        final JsonArrayBuilder offenceJudicialResultPrompts = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("isAvailableForCourtExtract", true)
                        .add("label", OFFENCE_JUDICIAL_RESULT_PROMPT_LABEL_VALUE)
                );

        final JsonArrayBuilder defendantJudicialResultPrompts = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("isAvailableForCourtExtract", true)
                        .add("label", DEFENDANT_JUDICIAL_RESULT_PROMPT_LABEL_VALUE)
                );

        final JsonArrayBuilder offenceJudicialResults = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("", OFFENCE_JUDICIAL_RESULT_LABEL_VALUE) // deliberately left out attribute
                        .add(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE, offenceJudicialResultPrompts)
                );

        final JsonArrayBuilder defendantJudicialResults = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("label", DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE)
                        .add(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE, defendantJudicialResultPrompts)
                );

        final JsonArrayBuilder offences = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(JUDICIAL_RESULTS_ATTRIBUTE, offenceJudicialResults));

        final JsonArrayBuilder defendants = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(JUDICIAL_RESULTS_ATTRIBUTE, defendantJudicialResults)
                        .add("offences", offences)
                );

        final JsonArrayBuilder prosecutionCases = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("defendants", defendants)
                );

        final String hearingId = randomUUID().toString();
        final JsonObject payload = createObjectBuilder()
                .add("hearing", createObjectBuilder().add("id", hearingId))
                .add("prosecutionCases", prosecutionCases)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("progression.event.prosecutionCase-defendant-listing-status-changed"), payload);

        final ResultDefinition defendantResultDefinition = getResultDefinition(DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE, DEFENDANT_JUDICIAL_RESULT_PROMPT_LABEL_VALUE);
        final ResultDefinition offenceResultDefinition = getResultDefinition(OFFENCE_JUDICIAL_RESULT_LABEL_VALUE, OFFENCE_JUDICIAL_RESULT_PROMPT_LABEL_VALUE);
        final String formattedDate = EVENT_DATE.format(DATE_TIME_FORMATTER);

        when(resultDefinitionService.getResultDefinition(formattedDate, DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE)).thenReturn(defendantResultDefinition);
        when(resultDefinitionService.getResultDefinition(formattedDate, OFFENCE_JUDICIAL_RESULT_LABEL_VALUE)).thenReturn(offenceResultDefinition);
        when(hearingService.getHearingSittingDate(hearingId)).thenReturn(EVENT_DATE);

        final EventPayloadTransformer eventPayloadTransformer = new EventPayloadTransformer();
        eventPayloadTransformer.setResultDefinitionService(resultDefinitionService);
        eventPayloadTransformer.setHearingService(hearingService);
        final JsonObject transformedPayload = eventPayloadTransformer.transform(jsonEnvelope);

        final JsonObject firstDefendant = transformedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0);
        final JsonObject defendantJudicialResultObject = firstDefendant.getJsonArray(JUDICIAL_RESULTS_ATTRIBUTE).getJsonObject(0);
        assertThat(defendantJudicialResultObject.getString("judicialResultTypeId"), is(defendantResultDefinition.getId()));
        assertThat(defendantJudicialResultObject.getString("resultText"), is("NA"));
        assertThat(defendantJudicialResultObject.getBoolean("terminatesOffenceProceedings"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("lifeDuration"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("publishedAsAPrompt"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("excludedFromResults"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("alwaysPublished"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("urgent"), is(true));
        assertThat(defendantJudicialResultObject.getBoolean("d20"), is(false));

        final JsonObject judicialResultPromptObject = defendantJudicialResultObject.getJsonArray(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE).getJsonObject(0);
        assertThat(judicialResultPromptObject.getString("judicialResultPromptTypeId"), is(defendantResultDefinition.getPrompts().get(0).getId()));
        assertThat(judicialResultPromptObject.getString("courtExtract"), is("Y"));
        assertThat(judicialResultPromptObject.containsKey("isAvailableForCourtExtract"), is(false));

        final JsonObject offenceJudicialResultObject = firstDefendant.getJsonArray("offences").getJsonObject(0).getJsonArray(JUDICIAL_RESULTS_ATTRIBUTE).getJsonObject(0);
        assertThat(offenceJudicialResultObject.getString("judicialResultTypeId"), is(offenceResultDefinition.getId()));
        assertThat(offenceJudicialResultObject.getString("resultText"), is("NA"));
        assertThat(offenceJudicialResultObject.getBoolean("terminatesOffenceProceedings"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("lifeDuration"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("publishedAsAPrompt"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("excludedFromResults"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("alwaysPublished"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("urgent"), is(true));
        assertThat(offenceJudicialResultObject.getBoolean("d20"), is(false));
        assertThat(offenceJudicialResultObject.getString("label"), is(OFFENCE_JUDICIAL_RESULT_LABEL_VALUE));
        assertThat(offenceJudicialResultObject.get(""), nullValue());

        final JsonObject offenceJudicialResultPromptObject = offenceJudicialResultObject.getJsonArray(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE).getJsonObject(0);
        assertThat(offenceJudicialResultPromptObject.getString("judicialResultPromptTypeId"), is(offenceResultDefinition.getPrompts().get(0).getId()));
        assertThat(offenceJudicialResultPromptObject.getString("courtExtract"), is("Y"));
        assertThat(offenceJudicialResultPromptObject.containsKey("isAvailableForCourtExtract"), is(false));

        verify(resultDefinitionService).getResultDefinition(formattedDate, OFFENCE_JUDICIAL_RESULT_LABEL_VALUE);
        verify(resultDefinitionService).getResultDefinition(formattedDate, DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE);

    }

    @Test
    public void testTransformV2() {

        final JsonArrayBuilder offenceJudicialResultPrompts = createArrayBuilder()
            .add(createObjectBuilder()
                .add("isAvailableForCourtExtract", true)
                .add("label", OFFENCE_JUDICIAL_RESULT_PROMPT_LABEL_VALUE)
            );

        final JsonArrayBuilder defendantJudicialResultPrompts = createArrayBuilder()
            .add(createObjectBuilder()
                .add("isAvailableForCourtExtract", true)
                .add("label", DEFENDANT_JUDICIAL_RESULT_PROMPT_LABEL_VALUE)
            );

        final JsonArrayBuilder offenceJudicialResults = createArrayBuilder()
            .add(createObjectBuilder()
                .add("", OFFENCE_JUDICIAL_RESULT_LABEL_VALUE) // deliberately left out attribute
                .add(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE, offenceJudicialResultPrompts)
            );

        final JsonArrayBuilder defendantJudicialResults = createArrayBuilder()
            .add(createObjectBuilder()
                .add("label", DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE)
                .add(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE, defendantJudicialResultPrompts)
            );

        final JsonArrayBuilder offences = createArrayBuilder()
            .add(createObjectBuilder()
                .add(JUDICIAL_RESULTS_ATTRIBUTE, offenceJudicialResults));

        final JsonArrayBuilder defendants = createArrayBuilder()
            .add(createObjectBuilder()
                .add(JUDICIAL_RESULTS_ATTRIBUTE, defendantJudicialResults)
                .add("offences", offences)
            );

        final JsonArrayBuilder prosecutionCases = createArrayBuilder()
            .add(createObjectBuilder()
                .add("defendants", defendants)
            );

        final String hearingId = randomUUID().toString();
        final JsonObject payload = createObjectBuilder()
            .add("hearing", createObjectBuilder().add("id", hearingId))
            .add("prosecutionCases", prosecutionCases)
            .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("progression.event.prosecutionCase-defendant-listing-status-changed-v2"), payload);

        final ResultDefinition defendantResultDefinition = getResultDefinition(DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE, DEFENDANT_JUDICIAL_RESULT_PROMPT_LABEL_VALUE);
        final ResultDefinition offenceResultDefinition = getResultDefinition(OFFENCE_JUDICIAL_RESULT_LABEL_VALUE, OFFENCE_JUDICIAL_RESULT_PROMPT_LABEL_VALUE);
        final String formattedDate = EVENT_DATE.format(DATE_TIME_FORMATTER);

        when(resultDefinitionService.getResultDefinition(formattedDate, DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE)).thenReturn(defendantResultDefinition);
        when(resultDefinitionService.getResultDefinition(formattedDate, OFFENCE_JUDICIAL_RESULT_LABEL_VALUE)).thenReturn(offenceResultDefinition);
        when(hearingService.getHearingSittingDate(hearingId)).thenReturn(EVENT_DATE);

        final EventPayloadTransformer eventPayloadTransformer = new EventPayloadTransformer();
        eventPayloadTransformer.setResultDefinitionService(resultDefinitionService);
        eventPayloadTransformer.setHearingService(hearingService);
        final JsonObject transformedPayload = eventPayloadTransformer.transform(jsonEnvelope);

        final JsonObject firstDefendant = transformedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0);
        final JsonObject defendantJudicialResultObject = firstDefendant.getJsonArray(JUDICIAL_RESULTS_ATTRIBUTE).getJsonObject(0);
        assertThat(defendantJudicialResultObject.getString("judicialResultTypeId"), is(defendantResultDefinition.getId()));
        assertThat(defendantJudicialResultObject.getString("resultText"), is("NA"));
        assertThat(defendantJudicialResultObject.getBoolean("terminatesOffenceProceedings"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("lifeDuration"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("publishedAsAPrompt"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("excludedFromResults"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("alwaysPublished"), is(false));
        assertThat(defendantJudicialResultObject.getBoolean("urgent"), is(true));
        assertThat(defendantJudicialResultObject.getBoolean("d20"), is(false));

        final JsonObject judicialResultPromptObject = defendantJudicialResultObject.getJsonArray(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE).getJsonObject(0);
        assertThat(judicialResultPromptObject.getString("judicialResultPromptTypeId"), is(defendantResultDefinition.getPrompts().get(0).getId()));
        assertThat(judicialResultPromptObject.getString("courtExtract"), is("Y"));
        assertThat(judicialResultPromptObject.containsKey("isAvailableForCourtExtract"), is(false));

        final JsonObject offenceJudicialResultObject = firstDefendant.getJsonArray("offences").getJsonObject(0).getJsonArray(JUDICIAL_RESULTS_ATTRIBUTE).getJsonObject(0);
        assertThat(offenceJudicialResultObject.getString("judicialResultTypeId"), is(offenceResultDefinition.getId()));
        assertThat(offenceJudicialResultObject.getString("resultText"), is("NA"));
        assertThat(offenceJudicialResultObject.getBoolean("terminatesOffenceProceedings"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("lifeDuration"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("publishedAsAPrompt"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("excludedFromResults"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("alwaysPublished"), is(false));
        assertThat(offenceJudicialResultObject.getBoolean("urgent"), is(true));
        assertThat(offenceJudicialResultObject.getBoolean("d20"), is(false));
        assertThat(offenceJudicialResultObject.getString("label"), is(OFFENCE_JUDICIAL_RESULT_LABEL_VALUE));
        assertThat(offenceJudicialResultObject.get(""), nullValue());

        final JsonObject offenceJudicialResultPromptObject = offenceJudicialResultObject.getJsonArray(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE).getJsonObject(0);
        assertThat(offenceJudicialResultPromptObject.getString("judicialResultPromptTypeId"), is(offenceResultDefinition.getPrompts().get(0).getId()));
        assertThat(offenceJudicialResultPromptObject.getString("courtExtract"), is("Y"));
        assertThat(offenceJudicialResultPromptObject.containsKey("isAvailableForCourtExtract"), is(false));

        verify(resultDefinitionService).getResultDefinition(formattedDate, OFFENCE_JUDICIAL_RESULT_LABEL_VALUE);
        verify(resultDefinitionService).getResultDefinition(formattedDate, DEFENDANT_JUDICIAL_RESULT_LABEL_VALUE);

    }

    private ResultDefinition getResultDefinition(final String resultDefinitionLabel, final String promptLabel) {
        ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(randomUUID().toString());
        resultDefinition.setLabel(resultDefinitionLabel);
        resultDefinition.setUrgent("Y");
        resultDefinition.setD20("N");
        Prompt prompt = new Prompt();
        prompt.setId(randomUUID().toString());
        prompt.setLabel(promptLabel);
        resultDefinition.setPrompts(newArrayList(prompt));
        return resultDefinition;
    }
}