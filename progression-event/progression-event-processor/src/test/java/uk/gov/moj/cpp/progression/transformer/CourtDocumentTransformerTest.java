package uk.gov.moj.cpp.progression.transformer;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentTransformerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentTransformerTest.class);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private CourtDocumentTransformer transformCourtDocument;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private MaterialService materialService;

    private UUID materialId = randomUUID();
    private UUID prosecutionCaseDocumentId = randomUUID();


    @Test
    public void shouldTransformCourtDocumentMaterialToJsonStringAndEvaluateEventNotification() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");
        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, Optional.empty(), jsonEnvelope, null);
        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectBusinessObjectId", is(prosecutionCaseDocumentId.toString())),
                withJsonPath("$.businessEventType", is("defence-requested-to-notify-cps-of-material")),
                withJsonPath("$.subjectDetails.material", is(materialId.toString())),
                withJsonPath("$.subjectDetails.materialType", is("defence type")),
                withJsonPath("$.subjectDetails.materialContentType", is("application/pdf"))
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenProsecutionJsonObjectIsNotPresent() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.empty();

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectBusinessObjectId", is(prosecutionCaseDocumentId.toString())),
                withJsonPath("$.businessEventType", is("defence-requested-to-notify-cps-of-material")),
                withJsonPath("$.subjectDetails.material", is(materialId.toString())),
                withJsonPath("$.subjectDetails.materialType", is("defence type")),
                withJsonPath("$.subjectDetails.materialContentType", is("application/pdf"))
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenProsecutionJsonObjectIsPresentAndCourtDocumentApplicationsAreNotPresent() {

        final CourtDocument courtDocument = buildCourtDocumentWithoutDocumentCategory(materialId);
        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, Optional.empty(), jsonEnvelope, null);

        assertThat(transformedPayload.isPresent(), is(false));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenCourtDocumentProsecutionIdIsNotMatchedWithProsecutionDefendantId() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObject(randomUUID());

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("URN-123")),
                withoutJsonPath("$.subjectDetails.prosecutionCaseSubject.defendantSubject.prosecutorDefendantId")
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenCourtDocumentProsecutionIdIsMatchedWithProsecutionDefendantId() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObject(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("URN-123")),
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.defendantSubject.prosecutorDefendantId", is("prosecutorAuthorityRef"))
        )));

    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenProsecutionCaseHasNotHavingCaseURNAndCallRefDataToGetOuCode() {

        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("oucode", "OUCODE123");

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutCaseUrn(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("prosecutorAuthorityRefNumber")),
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.prosecutingAuthority", is("OUCODE_123"))
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWhenProsecutionCaseHasOuCodeOnly() {
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("oucode", "OUCODE123");

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutCaseUrn(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.prosecutingAuthority", is("OUCODE_123"))
        )));
        verify(referenceDataService, times(0)).getProsecutor(any(JsonEnvelope.class), any(UUID.class), any(Requester.class));
    }


    @Test
    public void shouldTransformCourtDocumentMaterialWithDefendantSubject() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutCaseUrn(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.prosecutingAuthority", is("OUCODE_123")),
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.defendantSubject.asn", is("ASN-1234")),
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.defendantSubject.cpsDefendantId", is("0c10e736-3387-4044-85ed-e962e78caf0a")),
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.defendantSubject.prosecutorDefendantId", is("ProsecutionAuthorityReference_123"))
        )));
        verify(referenceDataService, times(0)).getProsecutor(any(JsonEnvelope.class), any(UUID.class), any(Requester.class));

    }

    @Test
    public void shouldTransformCourtDocumentMaterialAndCreateWithDefendantSubjectAndCPSPersonDefendantDetails() {

        final CourtDocument courtDocument = buildCourtDocument(materialId, prosecutionCaseDocumentId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutProsecutorAuthorityRef(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("URN-123"))
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWithCourtApplicationSubject() {

        final UUID applicationId = randomUUID();
        final CourtDocument courtDocument = buildCourtDocumentWithApplication(materialId, applicationId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutProsecutorAuthorityRef(prosecutionCaseDocumentId);

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);

        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("URN-123")),
                withJsonPath("$.subjectDetails.fileName", is("fileName")),
                withJsonPath("$.subjectDetails.courtApplicationSubject.courtApplicationId", is(applicationId.toString()))
        )));
    }

    @Test
    public void shouldTransformCourtDocumentMaterialWithCourtApplicationSubjectWhenMaterialServiceReturnFileName() {

        final UUID applicationId = randomUUID();
        final CourtDocument courtDocument = buildCourtDocumentWithApplication(materialId, applicationId);
        final Optional<JsonObject> prosecutionCaseJsonOptional = getProsecutionJsonObjectWithoutProsecutorAuthorityRef(prosecutionCaseDocumentId);

        final JsonObjectBuilder materialBuilder = Json.createObjectBuilder();
        materialBuilder.add("materialId", randomUUID().toString());
        materialBuilder.add("fileName", "fileName.pdf");
        materialBuilder.add("mimeType", "application/octet-stream");
        materialBuilder.add("externalLink", "http://example.com");

        when(materialService.getMaterialMetadataV2(any(JsonEnvelope.class), any(UUID.class))).thenReturn("fileName.pdf");

        final Optional<String> transformedPayload = transformCourtDocument.transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);
        LOGGER.info("transformedPayload: {}", transformedPayload.get());
        assertThat(transformedPayload.get(), isJson(Matchers.allOf(
                withJsonPath("$.subjectDetails.prosecutionCaseSubject.caseUrn", is("URN-123")),
                withJsonPath("$.subjectDetails.courtApplicationSubject.courtApplicationId", is(applicationId.toString())),
                withJsonPath("$.subjectDetails.fileName", is("fileName.pdf"))
        )));
    }

    private CourtDocument buildCourtDocument(final UUID materialId, final UUID prosecutionCaseId) {
        final CourtDocument.Builder courtDocument = getCourtDocumentBuilder(materialId);
        final DocumentCategory.Builder documentCategoryBuilder = DocumentCategory.documentCategory();
        final CaseDocument.Builder caseDocumentBuilder = CaseDocument.caseDocument();
        caseDocumentBuilder.withProsecutionCaseId(prosecutionCaseId);
        documentCategoryBuilder.withCaseDocument(caseDocumentBuilder.build());
        courtDocument.withDocumentCategory(documentCategoryBuilder.build());
        return courtDocument.build();
    }

    private CourtDocument buildCourtDocumentWithApplication(final UUID materialId, final UUID applicationId) {
        final CourtDocument.Builder courtDocument = getCourtDocumentBuilder(materialId);
        final DocumentCategory.Builder documentCategoryBuilder = DocumentCategory.documentCategory();
        final ApplicationDocument.Builder applicationDocument = ApplicationDocument.applicationDocument();
        applicationDocument.withApplicationId(applicationId);
        documentCategoryBuilder.withApplicationDocument(applicationDocument.build());
        courtDocument.withDocumentCategory(documentCategoryBuilder.build());
        return courtDocument.build();
    }

    private CourtDocument buildCourtDocumentWithoutDocumentCategory(final UUID materialId) {
        final CourtDocument.Builder courtDocument = getCourtDocumentBuilder(materialId);
        final DocumentCategory.Builder documentCategoryBuilder = DocumentCategory.documentCategory();
        courtDocument.withDocumentCategory(documentCategoryBuilder.build());
        return courtDocument.build();
    }


    private CourtDocument.Builder getCourtDocumentBuilder(final UUID materialId) {
        final CourtDocument.Builder courtDocument = new CourtDocument.Builder();
        final List<Material> materials = new ArrayList<>();
        final Material.Builder material = new Material.Builder();
        material.withId(materialId);
        materials.add(material.build());
        courtDocument.withMaterials(materials);
        courtDocument.withDocumentTypeDescription("defence type");
        courtDocument.withMimeType("application/pdf");
        return courtDocument;
    }

    private Optional<JsonObject> getProsecutionJsonObject(final UUID prosecutionCaseDocumentId) {
        final ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase();
        prosecutionCaseBuilder.withId(randomUUID());
        List<Defendant> defendants = new ArrayList<>();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        defendantBuilder.withId(prosecutionCaseDocumentId);
        defendantBuilder.withProsecutionAuthorityReference("prosecutorAuthorityRef");
        final LegalEntityDefendant.Builder legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant();
        final Organisation.Builder organisation = Organisation.organisation();
        organisation.withName("Organisation_name");
        legalEntityDefendant.withOrganisation(organisation.build());
        defendantBuilder.withLegalEntityDefendant(legalEntityDefendant.build());
        final PersonDefendant.Builder personDefendant = PersonDefendant.personDefendant();
        final Person.Builder person = Person.person();
        person.withFirstName("firstName");
        person.withLastName("lastName");
        person.withMiddleName("middleName");
        person.withDateOfBirth(LocalDate.of(2021, 10, 10));
        person.withTitle("Mrs");
        personDefendant.withPersonDetails(person.build());
        defendantBuilder.withPersonDefendant(personDefendant.build());
        defendants.add(defendantBuilder.build());
        prosecutionCaseBuilder.withDefendants(defendants);
        final ProsecutionCaseIdentifier.Builder prosecutionCaseIdentifierBuilder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        prosecutionCaseIdentifierBuilder.withCaseURN("URN-123");
        prosecutionCaseBuilder.withProsecutionCaseIdentifier(prosecutionCaseIdentifierBuilder.build());
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCaseBuilder.build()));
        return ofNullable(objectBuilder.build());
    }

    private Optional<JsonObject> getProsecutionJsonObjectWithoutProsecutorAuthorityRef(final UUID prosecutionCaseDocumentId) {
        final ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase();
        prosecutionCaseBuilder.withId(randomUUID());
        final List<Defendant> defendants = new ArrayList<>();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        defendantBuilder.withId(prosecutionCaseDocumentId);
        final LegalEntityDefendant.Builder legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant();
        final Organisation.Builder organisation = Organisation.organisation();
        organisation.withName("Organisation_name");
        legalEntityDefendant.withOrganisation(organisation.build());
        defendantBuilder.withLegalEntityDefendant(legalEntityDefendant.build());
        final PersonDefendant.Builder personDefendant = PersonDefendant.personDefendant();
        final Person.Builder person = Person.person();
        person.withFirstName("firstName");
        person.withLastName("lastName");
        person.withMiddleName("middleName");
        person.withDateOfBirth(LocalDate.of(2021, 10, 10));
        person.withTitle("Mrs");
        personDefendant.withPersonDetails(person.build());
        defendantBuilder.withPersonDefendant(personDefendant.build());
        defendants.add(defendantBuilder.build());
        prosecutionCaseBuilder.withDefendants(defendants);
        final ProsecutionCaseIdentifier.Builder prosecutionCaseIdentifierBuilder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        prosecutionCaseIdentifierBuilder.withCaseURN("URN-123");
        prosecutionCaseBuilder.withProsecutionCaseIdentifier(prosecutionCaseIdentifierBuilder.build());
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCaseBuilder.build()));
        return ofNullable(objectBuilder.build());
    }

    private Optional<JsonObject> getProsecutionJsonObjectWithoutCaseUrn(final UUID prosecutionCaseDocumentId) {
        final ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase();
        prosecutionCaseBuilder.withId(randomUUID());
        final List<Defendant> defendants = new ArrayList<>();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        defendantBuilder.withId(prosecutionCaseDocumentId);
        defendantBuilder.withProsecutionAuthorityReference("ProsecutionAuthorityReference_123");
        defendantBuilder.withCpsDefendantId("0c10e736-3387-4044-85ed-e962e78caf0a");
        final PersonDefendant.Builder personDefendant = PersonDefendant.personDefendant();
        personDefendant.withArrestSummonsNumber("ASN-1234");
        defendantBuilder.withPersonDefendant(personDefendant.build());
        defendants.add(defendantBuilder.build());
        prosecutionCaseBuilder.withDefendants(defendants);
        final ProsecutionCaseIdentifier.Builder prosecutionCaseIdentifierBuilder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityReference("prosecutorAuthorityRefNumber");
        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityOUCode("OUCODE_123");
        prosecutionCaseBuilder.withProsecutionCaseIdentifier(prosecutionCaseIdentifierBuilder.build());
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCaseBuilder.build()));
        return ofNullable(objectBuilder.build());
    }
}