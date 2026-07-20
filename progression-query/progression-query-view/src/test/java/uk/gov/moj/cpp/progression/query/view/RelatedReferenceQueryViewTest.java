package uk.gov.moj.cpp.progression.query.view;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelatedReferenceQueryViewTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private RelatedReferenceRepository relatedReferenceRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private SearchProsecutionCaseRepository searchProsecutionCaseRepository;

    @InjectMocks
    private RelatedReferenceQueryView relatedReferenceQueryView;

    private final UUID primaryCaseId = randomUUID();
    private final UUID relatedCaseId = randomUUID();
    private final UUID relatedReferenceId = randomUUID();
    private final String relatedReferenceUrn = "cn12345";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnRootIsCivilMatchingPrimaryCase(final boolean isCivil) {
        givenRelatedReference();
        givenPrimaryCase(isCivil);
        givenRelatedCaseUrnNotFound();

        assertThat(invoke().getBoolean("isCivil"), is(isCivil));
    }

    @Test
    void shouldOmitRootIsCivilWhenPrimaryCaseNotFound() {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseUrnNotFound();

        assertThat(invoke().containsKey("isCivil"), is(false));
    }

    private static Stream<Arguments> relatedCaseIsCivilScenarios() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(false, false),
                Arguments.of(null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("relatedCaseIsCivilScenarios")
    void shouldResolveRelatedCaseIsCivilOnEntry(final Boolean isCivil, final boolean expected) {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseResolvable(isCivil);

        assertThat(firstRelatedReference().getBoolean("isCivil"), is(expected));
    }

    @Test
    void shouldOmitEntryIsCivilWhenRelatedCaseUrnNotFound() {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseUrnNotFound();

        assertThat(firstRelatedReference().containsKey("isCivil"), is(false));
    }

    @Test
    void shouldOmitEntryIsCivilWhenRelatedCaseMissingFromProsecutionCase() {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseMissingFromProsecutionCase();

        assertThat(firstRelatedReference().containsKey("isCivil"), is(false));
    }

    @Test
    void shouldResolveRelatedCaseIsCivilIndependentlyOfPrimaryCase() {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseResolvable(true);

        final JsonObject response = invoke();

        assertThat(response.containsKey("isCivil"), is(false));
        assertThat(response.getJsonArray("relatedReferenceList").getJsonObject(0).getBoolean("isCivil"), is(true));
    }

    @Test
    void shouldUppercaseRelatedReferenceBeforeLookup() {
        givenRelatedReference();
        givenPrimaryCaseNotFound();
        givenRelatedCaseResolvable(false);

        invoke();

        verify(searchProsecutionCaseRepository).findByCaseUrn(relatedReferenceUrn.toUpperCase());
    }

    @Test
    void shouldReturnEmptyListWhenNoRelatedReferences() {
        when(relatedReferenceRepository.findByProsecutionCaseId(primaryCaseId)).thenReturn(emptyList());
        givenPrimaryCase(false);

        final JsonArray list = invoke().getJsonArray("relatedReferenceList");

        assertThat(list.isEmpty(), is(true));
    }

    private JsonObject invoke() {
        final JsonObject payload = createObjectBuilder().add("caseId", primaryCaseId.toString()).build();
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("progression.query.related-references"), payload);
        return relatedReferenceQueryView.getProsecutionCaseWithRelatedUrn(envelope).payloadAsJsonObject();
    }

    private JsonObject firstRelatedReference() {
        return invoke().getJsonArray("relatedReferenceList").getJsonObject(0);
    }

    private void givenRelatedReference() {
        final RelatedReference relatedReference = new RelatedReference();
        relatedReference.setId(relatedReferenceId);
        relatedReference.setReference(relatedReferenceUrn);
        relatedReference.setProsecutionCaseId(primaryCaseId);
        when(relatedReferenceRepository.findByProsecutionCaseId(primaryCaseId)).thenReturn(List.of(relatedReference));
    }

    private void givenPrimaryCase(final boolean isCivil) {
        final ProsecutionCaseEntity entity = new ProsecutionCaseEntity();
        entity.setCaseId(primaryCaseId);
        entity.setPayload("primaryPayload");
        when(prosecutionCaseRepository.findByCaseId(primaryCaseId)).thenReturn(entity);

        final JsonObject primaryJson = createObjectBuilder().add("marker", "primary").build();
        when(stringToJsonObjectConverter.convert("primaryPayload")).thenReturn(primaryJson);
        when(jsonObjectToObjectConverter.convert(primaryJson, ProsecutionCase.class))
                .thenReturn(ProsecutionCase.prosecutionCase().withIsCivil(isCivil).build());
    }

    private void givenPrimaryCaseNotFound() {
        when(prosecutionCaseRepository.findByCaseId(primaryCaseId)).thenThrow(new NoResultException());
    }

    private void givenRelatedCaseResolvable(final Boolean isCivil) {
        final SearchProsecutionCaseEntity searchEntity = new SearchProsecutionCaseEntity();
        searchEntity.setCaseId(relatedCaseId.toString());
        when(searchProsecutionCaseRepository.findByCaseUrn(relatedReferenceUrn.toUpperCase())).thenReturn(List.of(searchEntity));

        final ProsecutionCaseEntity entity = new ProsecutionCaseEntity();
        entity.setCaseId(relatedCaseId);
        entity.setPayload("relatedPayload");
        when(prosecutionCaseRepository.findByCaseId(relatedCaseId)).thenReturn(entity);

        final JsonObject relatedJson = createObjectBuilder().add("marker", "related").build();
        when(stringToJsonObjectConverter.convert("relatedPayload")).thenReturn(relatedJson);
        when(jsonObjectToObjectConverter.convert(relatedJson, ProsecutionCase.class))
                .thenReturn(ProsecutionCase.prosecutionCase().withIsCivil(isCivil).build());
    }

    private void givenRelatedCaseUrnNotFound() {
        when(searchProsecutionCaseRepository.findByCaseUrn(relatedReferenceUrn.toUpperCase())).thenReturn(emptyList());
    }

    private void givenRelatedCaseMissingFromProsecutionCase() {
        final SearchProsecutionCaseEntity searchEntity = new SearchProsecutionCaseEntity();
        searchEntity.setCaseId(relatedCaseId.toString());
        when(searchProsecutionCaseRepository.findByCaseUrn(relatedReferenceUrn.toUpperCase())).thenReturn(List.of(searchEntity));
        when(prosecutionCaseRepository.findByCaseId(relatedCaseId)).thenThrow(new NoResultException());
    }
}
