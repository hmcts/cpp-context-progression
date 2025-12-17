package uk.gov.moj.cpp.progression.query;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.core.courts.FormType.PTPH;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormQueryViewTest {

    @InjectMocks
    private FormQueryView formQueryView;

    @Mock
    private CaseDefendantOffenceRepository caseDefendantOffenceRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final UUID CASE_ID = randomUUID();

    private static final UUID ID_1 = randomUUID();
    private static final UUID ID_2 = randomUUID();
    private static final UUID ID_3 = randomUUID();
    private static final UUID ID_4 = randomUUID();
    private static final UUID ID_5 = randomUUID();
    private static final UUID ID_6 = randomUUID();

    private static final UUID COURT_FORM_ID_1 = randomUUID();
    private static final UUID COURT_FORM_ID_2 = randomUUID();
    private static final UUID COURT_FORM_ID_3 = randomUUID();
    private static final UUID COURT_FORM_ID_4 = randomUUID();

    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_3 = randomUUID();
    private static final UUID DEFENDANT_ID_4 = randomUUID();
    private static final UUID DEFENDANT_ID_5 = randomUUID();
    private static final UUID DEFENDANT_ID_6 = randomUUID();

    private static final UUID OFFENCE_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID_3 = randomUUID();

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldReturnFormsForCase_whenPassingInCaseId() {

        when(caseDefendantOffenceRepository.findByCaseId(CASE_ID)).thenReturn(asList(
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_2, BCM)
        ));

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.forms-for-case"), createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        //when
        final JsonEnvelope formsResult = formQueryView.getFormsForCase(envelope);

        final JsonObject formResultAsJson = formsResult.payloadAsJsonObject();
        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        final JsonArray formsArray = formResultAsJson.getJsonArray("forms");
        assertThat(formsArray, hasSize(2));

        final ImmutableMap<UUID, Matcher> offencesMatchersForForm1 = of(DEFENDANT_ID_1, contains(OFFENCE_ID_1));
        final ImmutableMap<UUID, Matcher> offencesMatchersForForm2 = of(DEFENDANT_ID_2, contains(OFFENCE_ID_2));

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_1, BCM, contains(DEFENDANT_ID_1), offencesMatchersForForm1));
        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_2, BCM, contains(DEFENDANT_ID_2), offencesMatchersForForm2));
    }

    @Test
    public void shouldReturnFormsForCase_whenPassingInCaseId_And_FormType() {

        when(caseDefendantOffenceRepository.findByCaseIdAndFormType(CASE_ID, BCM)).thenReturn(asList(
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_3, CASE_ID, COURT_FORM_ID_1, BCM)
        ));

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.forms-for-case"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("formType", BCM.name())
                        .build());

        //when
        final JsonEnvelope formsResult = formQueryView.getFormsForCase(envelope);

        final JsonObject formResultAsJson = formsResult.payloadAsJsonObject();
        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        final JsonArray formsArray = formResultAsJson.getJsonArray("forms");
        assertThat(formsArray, hasSize(1));

        final ImmutableMap<UUID, Matcher> offencesMatchers = of(
                DEFENDANT_ID_1, containsInAnyOrder(OFFENCE_ID_1, OFFENCE_ID_2),
                DEFENDANT_ID_2, contains(OFFENCE_ID_1),
                DEFENDANT_ID_3, empty());

        final Matcher defendantMatcher = containsInAnyOrder(DEFENDANT_ID_1, DEFENDANT_ID_2, DEFENDANT_ID_3);

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_1, BCM, defendantMatcher, offencesMatchers));
    }

    @Test
    public void shouldReturnNoFormsForCase_whenNonePresentForCaseAndFormTypePassedIn() {

        when(caseDefendantOffenceRepository.findByCaseIdAndFormType(CASE_ID, BCM)).thenReturn(
                emptyList()
        );

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.forms-for-case"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("formType", BCM.name())
                        .build());

        //when
        final JsonEnvelope formsResult = formQueryView.getFormsForCase(envelope);

        final JsonObject formResultAsJson = formsResult.payloadAsJsonObject();
        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        final JsonArray formsArray = formResultAsJson.getJsonArray("forms");
        assertThat(formsArray, empty());
    }

    @Test
    public void shouldReturnFormsForCase_WithDifferentFormTypes_whenPassingInCaseId() {

        when(caseDefendantOffenceRepository.findByCaseId(CASE_ID)).thenReturn(asList(
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_3, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_4, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_3, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_3, PET),
                new CaseDefendantOffence(ID_3, DEFENDANT_ID_6, CASE_ID, COURT_FORM_ID_3, PET),
                new CaseDefendantOffence(ID_3, DEFENDANT_ID_4, CASE_ID, COURT_FORM_ID_3, PET),
                new CaseDefendantOffence(ID_4, DEFENDANT_ID_5, CASE_ID, COURT_FORM_ID_4, PTPH),
                new CaseDefendantOffence(ID_4, DEFENDANT_ID_6, CASE_ID, COURT_FORM_ID_4, PTPH)
        ));

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.forms-for-case"), createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        // when
        final JsonEnvelope formsResult = formQueryView.getFormsForCase(envelope);

        // There will be 4 forms in the results; 2 for BCM, 1 PET and 1 PTPH
        final JsonObject formResultAsJson = formsResult.payloadAsJsonObject();

        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        final JsonArray formsArray = formResultAsJson.getJsonArray("forms");
        assertThat(formsArray, hasSize(4));

        assertCorrectNumberOfFormsForFormType(formsArray, PTPH, 1);
        assertCorrectNumberOfFormsForFormType(formsArray, BCM, 2);
        assertCorrectNumberOfFormsForFormType(formsArray, PET, 1);

        final ImmutableMap<UUID, Matcher> offencesMatchersForPTPHDefendants = of(
                DEFENDANT_ID_5, contains(OFFENCE_ID_2),
                DEFENDANT_ID_6, contains(OFFENCE_ID_1));

        final ImmutableMap<UUID, Matcher> offencesMatchersForPETDefendants = of(
                DEFENDANT_ID_2, contains(OFFENCE_ID_2),
                DEFENDANT_ID_4, contains(OFFENCE_ID_2),
                DEFENDANT_ID_6, contains(OFFENCE_ID_2));

        final ImmutableMap<UUID, Matcher> offencesMatchersForBCMDefendantsForm1 = of(DEFENDANT_ID_1, contains(OFFENCE_ID_1));

        final ImmutableMap<UUID, Matcher> offencesMatchersForBCMDefendantsForm2 = of(
                DEFENDANT_ID_2, contains(OFFENCE_ID_2),
                DEFENDANT_ID_3, contains(OFFENCE_ID_2),
                DEFENDANT_ID_4, contains(OFFENCE_ID_2));

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_4, PTPH, containsInAnyOrder(DEFENDANT_ID_5, DEFENDANT_ID_6), offencesMatchersForPTPHDefendants));

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_3, PET,
                containsInAnyOrder(DEFENDANT_ID_2, DEFENDANT_ID_4, DEFENDANT_ID_6), offencesMatchersForPETDefendants));

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_1, BCM,
                containsInAnyOrder(DEFENDANT_ID_1), offencesMatchersForBCMDefendantsForm1));

        assertThat(formsArray, containsFormWithCorrectDefendantsAndOffences(COURT_FORM_ID_2, BCM,
                containsInAnyOrder(DEFENDANT_ID_2, DEFENDANT_ID_3, DEFENDANT_ID_4), offencesMatchersForBCMDefendantsForm2));
    }

    @Test
    public void shouldReturnFormWithMultipleDefendantsWithOffence_whenCourtFormIdIsPassedIn_ForFormQuery() {
        final List<CaseDefendantOffence> caseDefendantOffences = asList(
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_3, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_4, DEFENDANT_ID_3, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_5, DEFENDANT_ID_4, CASE_ID, COURT_FORM_ID_2, BCM),
                new CaseDefendantOffence(ID_6, DEFENDANT_ID_5, CASE_ID, COURT_FORM_ID_2, BCM)
        );

        when(caseDefendantOffenceRepository.findByCourtFormId(COURT_FORM_ID_2)).thenReturn(caseDefendantOffences);

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.form"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("courtFormId", COURT_FORM_ID_2.toString())
                        .build());

        // when
        final JsonEnvelope formResult = formQueryView.getForm(envelope);

        final JsonObject formResultAsJson = formResult.payloadAsJsonObject();
        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        assertThat(formResultAsJson.getString("formType"), is(BCM.name()));
        assertThat(formResultAsJson.getString("courtFormId"), is(COURT_FORM_ID_2.toString()));

        final JsonArray defendantsArray = formResultAsJson.getJsonArray("defendants");
        assertThat(defendantsArray, hasSize(5));

        final List<String> defendantIds = defendantsArray.stream().map(defendantOffences -> ((JsonObject) defendantOffences).getString("defendantId")).collect(toList());
        assertThat(defendantIds, containsInAnyOrder(
                DEFENDANT_ID_1.toString(), DEFENDANT_ID_2.toString(), DEFENDANT_ID_3.toString(),
                DEFENDANT_ID_4.toString(), DEFENDANT_ID_5.toString()));
    }

    @Test
    public void shouldNotReturnAnyOffencesForDefendantsWithNullOffence_whenCourtFormIdIsPassedIn_ForFormQuery() {
        final List<CaseDefendantOffence> caseDefendantOffences = asList(
                new CaseDefendantOffence(ID_1, DEFENDANT_ID_1, CASE_ID, COURT_FORM_ID_1, BCM),
                new CaseDefendantOffence(ID_2, DEFENDANT_ID_2, CASE_ID, COURT_FORM_ID_1, BCM)
        );

        when(caseDefendantOffenceRepository.findByCourtFormId(COURT_FORM_ID_1)).thenReturn(caseDefendantOffences);

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.form"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("courtFormId", COURT_FORM_ID_1.toString())
                        .build());

        // when
        final JsonEnvelope formResult = formQueryView.getForm(envelope);

        final JsonObject formResultAsJson = formResult.payloadAsJsonObject();
        assertThat(formResultAsJson.getString("caseId"), is(CASE_ID.toString()));
        assertThat(formResultAsJson.getString("formType"), is(BCM.name()));
        assertThat(formResultAsJson.getString("courtFormId"), is(COURT_FORM_ID_1.toString()));
        final JsonArray defendantsArray = formResultAsJson.getJsonArray("defendants");
        assertThat(defendantsArray, hasSize(2));

        final List<String> defendantIds = defendantsArray.stream().map(defendantOffences -> ((JsonObject) defendantOffences).getString("defendantId")).collect(toList());
        assertThat(defendantIds, containsInAnyOrder(DEFENDANT_ID_1.toString(), DEFENDANT_ID_2.toString()));
    }

    @Test
    public void shouldNotReturnAnyForms_whenNonePresentForCourtFormIdPassedIn_ForFormQuery() {
        when(caseDefendantOffenceRepository.findByCourtFormId(COURT_FORM_ID_1)).thenReturn(emptyList());

        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.form"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("courtFormId", COURT_FORM_ID_1.toString())
                        .build());

        // when
        final JsonEnvelope formResult = formQueryView.getForm(envelope);

        final JsonObject formResultAsJson = formResult.payloadAsJsonObject();

        assertThat(formResultAsJson.size(), is(0));
    }

    private Matcher<JsonArray> containsFormWithCorrectDefendantsAndOffences(final UUID courtFormId, final FormType expectedFormType, final Matcher defendantMatcher, final Map<UUID, Matcher> offenceMatchers) {
        return new TypeSafeMatcher<JsonArray>() {

            @Override
            protected boolean matchesSafely(final JsonArray formsArray) {
                final List<JsonValue> actualForms = formsArray.stream()
                        .filter(forms ->
                                ((JsonObject) forms).getString("courtFormId").equals(courtFormId.toString()))
                        .collect(toList());

                assertThat(actualForms, hasSize(1));
                final JsonObject formAsJson = (JsonObject) actualForms.get(0);
                assertThat((formAsJson.getString("formType")), is(expectedFormType.name()));

                final JsonArray actualDefendants = formAsJson.getJsonArray("defendants");
                final List<UUID> actualDefendantIds = actualDefendants.stream()
                        .map(defendant -> ((JsonObject) defendant).getString("defendantId"))
                        .map(UUID::fromString)
                        .collect(toList());

                assertThat(actualDefendantIds, defendantMatcher);
                return true;
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }


    private void assertCorrectNumberOfFormsForFormType(final JsonArray formsArray, final FormType expectedFormType, final int expectedNumberOfForms) {
        final List<JsonValue> ptphForms = formsArray.stream()
                .map(form -> (JsonObject) form)
                .filter(formType -> formType.getString("formType").equals(expectedFormType.name()))
                .collect(toList());

        assertThat(ptphForms, hasSize(expectedNumberOfForms));

    }
}