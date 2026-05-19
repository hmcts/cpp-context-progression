# AMP-504: PCR rawPayload Forwarding — Implementation Plan (Design)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the PDF-ready PCR source payload (`rawPayload`) in every `EventPayload` POST to the subscription service, by fetching it from the File Service using `fileId` that is already present on `PrisonCourtRegisterGeneratedV2`.

**Architecture:** `mappedPayload` is already stored in the File Service by `generatePrisonCourtRegister()` and its `fileId` is carried in every `prison-court-register-generated-v2` event. `sendPrisonCourtRegisterV2()` calls a new `FileService.retrievePayload(fileId)` method (backed by the existing `FileRetriever` API on the classpath) to fetch the payload, converts it to `Map<String,Object>`, and passes it to `HearingResultsDocumentSubscriptionPCRMapper` as `rawPayload`. No event store changes, no schema changes, no aggregate changes.

**Tech Stack:** Java 11+, `FileRetriever` (file-service-api), Jackson `ObjectMapper` + `TypeReference`, Lombok `@Builder`, JUnit 5 + Mockito, Maven, Hamcrest.

---

## File Map

| File | Action |
|------|--------|
| `api-cp-crime-hearing-results-document-subscription/src/main/resources/openapi/openapi-spec.yml` | Add `rawPayload` to `EventPayload` schema |
| `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/FileService.java` | Add `retrievePayload(UUID)` using `FileRetriever` |
| `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/FileServiceTest.java` | Add tests for `retrievePayload()` |
| `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayload.java` | Add `Map<String,Object> rawPayload` field |
| `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java` | Add `rawPayload` param; fix `timestamp` bug |
| `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java` | Update to 4-arg; add `rawPayload` + `timestamp` assertions |
| `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessor.java` | Inject `ObjectMapper`; call `fileService.retrievePayload(fileId)`; pass `rawPayload` to mapper |
| `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessorTest.java` | Mock `fileService.retrievePayload()`; assert `rawPayload` passed to mapper |

---

## Task 0: OpenAPI spec — add `rawPayload` to `EventPayload`

**Repo:** `api-cp-crime-hearing-results-document-subscription`

**Files:**
- Modify: `src/main/resources/openapi/openapi-spec.yml`

- [ ] **Step 1: Add `rawPayload` property to `EventPayload` schema**

Open `src/main/resources/openapi/openapi-spec.yml`. Find `EventPayload:` under `components/schemas`. After the `defendant:` property block (the last property), add:

```yaml
        rawPayload:
          type: object
          additionalProperties: true
          description: >
            Full source payload used to generate the PCR document.
            Fetched from the File Service using fileId and stored
            verbatim for subscriber retrieval.
```

Do **not** add `rawPayload` to the `required:` list — it is optional.

- [ ] **Step 2: Regenerate and publish to Maven local**

```bash
cd api-cp-crime-hearing-results-document-subscription
./gradlew openApiGenerate publishToMavenLocal
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify generated `EventPayload` field**

```bash
grep -n "rawPayload" build/generated/src/main/java/uk/gov/hmcts/cp/openapi/model/EventPayload.java
```

Expected: one line showing the field declaration.

- [ ] **Step 4: Run tests**

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
cd api-cp-crime-hearing-results-document-subscription
git add src/main/resources/openapi/openapi-spec.yml
git commit -m "feat(AMP-504): add rawPayload field to EventPayload schema"
```

---

## Task 1: `FileService` — add `retrievePayload(UUID)`

**Repo:** `cpp-context-progression`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/FileService.java`
- Create/Modify: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/FileServiceTest.java`

- [ ] **Step 1: Write the failing test**

Find or create `FileServiceTest.java` at:
`progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/FileServiceTest.java`

```java
package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private FileRetriever fileRetriever;

    @Test
    void retrievePayloadShouldReturnParsedJsonObject() throws FileServiceException {
        final UUID fileId = randomUUID();
        final String json = "{\"courtHouse\":\"Southwark Crown Court\",\"registerDate\":\"2024-10-01\"}";
        final FileReference fileRef = new FileReference(
                fileId,
                javax.json.Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.of(fileRef));

        final Optional<JsonObject> result = fileService.retrievePayload(fileId);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("courtHouse"), equalTo("Southwark Crown Court"));
        assertThat(result.get().getString("registerDate"), equalTo("2024-10-01"));
    }

    @Test
    void retrievePayloadShouldReturnEmptyWhenFileNotFound() throws FileServiceException {
        final UUID fileId = randomUUID();
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.empty());

        final Optional<JsonObject> result = fileService.retrievePayload(fileId);

        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd cpp-context-progression
mvn test -pl progression-event/progression-event-processor -am -Dtest=FileServiceTest
```

Expected: compilation failure — `retrievePayload` method does not exist.

- [ ] **Step 3: Add `retrievePayload()` to `FileService.java`**

Add `FileRetriever` injection and the new method. Full updated `FileService.java`:

```java
package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2139", "squid:S00112"})
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Inject
    private FileStorer fileStorer;

    @Inject
    private FileRetriever fileRetriever;

    public UUID storePayload(final JsonObject payload, final String fileName, final String templateName) {
        try {
            final byte[] jsonPayloadInBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

            final JsonObject metadata = createObjectBuilder()
                    .add("fileName", fileName)
                    .add("conversionFormat", ConversionFormat.PDF.toString())
                    .add("templateName", templateName)
                    .add("numberOfPages", 1)
                    .add("fileSize", jsonPayloadInBytes.length)
                    .build();

            return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));

        } catch (FileServiceException fileServiceException) {
            LOGGER.error("failed to store json payload metadata into file service", fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    public Optional<JsonObject> retrievePayload(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId).map(ref -> {
                try (InputStream stream = ref.getContentStream()) {
                    final String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    return Json.createReader(new StringReader(json)).readObject();
                } catch (java.io.IOException e) {
                    LOGGER.error("Failed to read content stream for fileId {}", fileId, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (FileServiceException e) {
            LOGGER.error("Failed to retrieve payload from file service for fileId {}", fileId, e);
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
mvn test -pl progression-event/progression-event-processor -am -Dtest=FileServiceTest
```

Expected: both tests pass.

- [ ] **Step 5: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/FileService.java
git add progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/FileServiceTest.java
git commit -m "feat(AMP-504): add retrievePayload(UUID) to FileService using FileRetriever"
```

---

## Task 2: `PcrEventPayload` DTO — add `rawPayload` field

**Repo:** `cpp-context-progression`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayload.java`

- [ ] **Step 1: Add `rawPayload` field**

Open `PcrEventPayload.java`. Add `rawPayload` as the last field in the Lombok `@Builder` class, after `defendant`:

```java
private Map<String, Object> rawPayload;
```

Add import:
```java
import java.util.Map;
```

- [ ] **Step 2: Compile to confirm**

```bash
mvn clean compile -pl progression-event/progression-event-processor -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayload.java
git commit -m "feat(AMP-504): add rawPayload field to PcrEventPayload DTO"
```

---

## Task 3: `HearingResultsDocumentSubscriptionPCRMapper` — add `rawPayload` param and fix `timestamp` bug

**Repo:** `cpp-context-progression`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java`
- Modify: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java`

- [ ] **Step 1: Write the failing tests**

Add these two tests to `HearingResultsDocumentSubscriptionPCRMapperTest.java`:

```java
@Test
void mapperShouldIncludeRawPayload() {
    final Instant createdAt = Instant.parse("2024-10-01T10:00:00Z");
    final Map<String, Object> rawPayload = Map.of("courtHouse", "Southwark Crown Court");

    final PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(
            pcr, "wandsworth@example.com", createdAt, rawPayload);

    assertThat(payload.getRawPayload(), equalTo(rawPayload));
}

@Test
void mapperShouldUseCreatedAtNotNow() {
    final Instant createdAt = Instant.parse("2024-10-01T10:00:00Z");

    final PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(
            pcr, "wandsworth@example.com", createdAt, Map.of());

    assertThat(payload.getTimestamp(), equalTo(createdAt));
}
```

Add import:
```java
import java.util.Map;
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl progression-event/progression-event-processor -am -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest
```

Expected: compilation failure — 4-arg signature not found.

- [ ] **Step 3: Update the mapper**

Replace the `mapPcrForhearingResultsDocument` method signature and body:

```java
public PcrEventPayload mapPcrForhearingResultsDocument(
        final PrisonCourtRegisterGeneratedV2 pcrIn,
        final String prisonEmail,
        final Instant createdAt,
        final Map<String, Object> rawPayload) {
    return PcrEventPayload.builder()
            .eventType(PcrEventType.PRISON_COURT_REGISTER_GENERATED)
            .eventId(pcrIn.getId())
            .hearingId(pcrIn.getHearingId())
            .materialId(pcrIn.getMaterialId())
            .timestamp(createdAt)
            .defendant(mapDefendant(pcrIn, prisonEmail))
            .rawPayload(rawPayload)
            .build();
}
```

Add import:
```java
import java.util.Map;
```

- [ ] **Step 4: Update the existing test calls to use 4-arg signature**

In `HearingResultsDocumentSubscriptionPCRMapperTest.java`, update both existing calls:

```java
// mapperShouldCreateAmpPayload — change to:
Instant createdAt = Instant.parse("2024-10-01T10:00:00Z");
PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(
        pcr, "wandsworth@example.com", createdAt, Map.of());
// replace: assertThat(payload.getTimestamp(), is(notNullValue()));
// with:
assertThat(payload.getTimestamp(), equalTo(createdAt));

// mapperShouldBeNullSafe — change to:
PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(emptyPcr, null, null, null);
```

- [ ] **Step 5: Run all mapper tests**

```bash
mvn test -pl progression-event/progression-event-processor -am -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest
```

Expected: all 4 tests pass.

- [ ] **Step 6: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java
git add progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java
git commit -m "feat(AMP-504): add rawPayload to PCR mapper; fix timestamp always-now bug"
```

---

## Task 4: `PrisonCourtRegisterEventProcessor` — wire File Service fetch into `sendPrisonCourtRegisterV2()`

**Repo:** `cpp-context-progression`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessor.java`
- Modify: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessorTest.java`

- [ ] **Step 1: Write the failing tests**

Add these tests to `PrisonCourtRegisterEventProcessorTest.java`:

```java
@Test
public void shouldPassRawPayloadFromFileServiceToMapper() throws InterruptedException {
    final UUID fileId = randomUUID();
    final Map<String, Object> sourcePayload = Map.of("courtHouse", "Southwark Crown Court");
    final JsonObject sourcePayloadJson = Json.createObjectBuilder()
            .add("courtHouse", "Southwark Crown Court").build();

    final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGeneratedV2 =
            PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                    .withId(randomUUID())
                    .withFileId(fileId)
                    .withMaterialId(randomUUID())
                    .withHearingId(randomUUID())
                    .withCourtCentreId(randomUUID())
                    .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                            .withEmailAddress1("prison@example.com")
                            .withEmailTemplateName("templateName").build()))
                    .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                            .withName("Test Defendant").build())
                    .build();

    final JsonEnvelope requestMessage = envelopeFrom(
            MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-generated-v2"),
            objectToJsonObjectConverter.convert(prisonCourtRegisterGeneratedV2));

    when(applicationParameters.getHearingResultsDocumentSubscriptionUrl()).thenReturn("http://hrds/notifications");
    when(applicationParameters.getHearingResultsDocumentSubscriptionRetryTimes()).thenReturn("3");
    when(applicationParameters.getHearingResultsDocumentSubscriptionRetryInterval()).thenReturn("1000");
    when(fileService.retrievePayload(fileId)).thenReturn(Optional.of(sourcePayloadJson));
    when(hearingResultsDocumentSubscriptionClient.post(anyString(), any()))
            .thenReturn(Response.ok().build());
    when(hearingResultsDocumentSubscriptionPCRMapper.mapPcrForhearingResultsDocument(
            any(), anyString(), any(), any())).thenReturn(PcrEventPayload.builder().build());

    prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> rawPayloadCaptor = ArgumentCaptor.forClass(Map.class);
    verify(hearingResultsDocumentSubscriptionPCRMapper).mapPcrForhearingResultsDocument(
            any(), anyString(), any(), rawPayloadCaptor.capture());
    assertThat(rawPayloadCaptor.getValue().get("courtHouse"), equalTo("Southwark Crown Court"));
}

@Test
public void shouldFallBackToV2EventJsonWhenFileServiceReturnsEmpty() throws InterruptedException {
    final UUID fileId = randomUUID();

    final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGeneratedV2 =
            PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                    .withId(randomUUID())
                    .withFileId(fileId)
                    .withMaterialId(randomUUID())
                    .withHearingId(randomUUID())
                    .withCourtCentreId(randomUUID())
                    .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                            .withEmailAddress1("prison@example.com")
                            .withEmailTemplateName("templateName").build()))
                    .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                            .withName("Test Defendant").build())
                    .build();

    final JsonEnvelope requestMessage = envelopeFrom(
            MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-generated-v2"),
            objectToJsonObjectConverter.convert(prisonCourtRegisterGeneratedV2));

    when(applicationParameters.getHearingResultsDocumentSubscriptionUrl()).thenReturn("http://hrds/notifications");
    when(applicationParameters.getHearingResultsDocumentSubscriptionRetryTimes()).thenReturn("3");
    when(applicationParameters.getHearingResultsDocumentSubscriptionRetryInterval()).thenReturn("1000");
    when(fileService.retrievePayload(fileId)).thenReturn(Optional.empty());
    when(hearingResultsDocumentSubscriptionClient.post(anyString(), any()))
            .thenReturn(Response.ok().build());
    when(hearingResultsDocumentSubscriptionPCRMapper.mapPcrForhearingResultsDocument(
            any(), anyString(), any(), any())).thenReturn(PcrEventPayload.builder().build());

    prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> rawPayloadCaptor = ArgumentCaptor.forClass(Map.class);
    verify(hearingResultsDocumentSubscriptionPCRMapper).mapPcrForhearingResultsDocument(
            any(), anyString(), any(), rawPayloadCaptor.capture());
    assertThat(rawPayloadCaptor.getValue(), is(notNullValue())); // fallback: v2 event JSON
}
```

Add imports:
```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
```

Also update any existing 3-arg mapper mock stubs in the test file to 4-arg:
```java
// Old:
when(hearingResultsDocumentSubscriptionPCRMapper.mapPcrForhearingResultsDocument(any(), anyString(), any()))
// New:
when(hearingResultsDocumentSubscriptionPCRMapper.mapPcrForhearingResultsDocument(any(), anyString(), any(), any()))
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl progression-event/progression-event-processor -am -Dtest=PrisonCourtRegisterEventProcessorTest
```

Expected: compilation failure — `fileService.retrievePayload` and 4-arg mapper call not found.

- [ ] **Step 3: Inject `ObjectMapper` into `PrisonCourtRegisterEventProcessor`**

Add after the existing `@Inject` fields:

```java
@Inject
private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
```

- [ ] **Step 4: Update `sendPrisonCourtRegisterV2()` to fetch rawPayload from File Service**

Inside `sendPrisonCourtRegisterV2()`, replace the existing `pcrEventPayload` construction:

```java
final UUID fileId = prisonCourtRegisterGenerated.getFileId();
final Map<String, Object> rawPayload = fileService.retrievePayload(fileId)
        .map(sp -> objectMapper.convertValue(
                sp, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}))
        .orElseGet(() -> objectMapper.convertValue(
                prisonCourtRegisterGenerated,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));

PcrEventPayload pcrEventPayload = hearingResultsDocumentSubscriptionPCRMapper
        .mapPcrForhearingResultsDocument(prisonCourtRegisterGenerated, emailRecipient, createdAt, rawPayload);
```

Add import:
```java
import java.util.Map;
import java.util.Optional;
```

- [ ] **Step 5: Run all event processor tests**

```bash
mvn test -pl progression-event/progression-event-processor -am -Dtest=PrisonCourtRegisterEventProcessorTest
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessor.java
git add progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/processor/PrisonCourtRegisterEventProcessorTest.java
git commit -m "feat(AMP-504): fetch rawPayload from File Service in sendPrisonCourtRegisterV2"
```

---

## Task 5: Full build verification

- [ ] **Step 1: Build the entire progression project**

```bash
cd cpp-context-progression
mvn clean install
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Confirm no aggregate or schema changes leaked in**

```bash
git diff HEAD~5 -- progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/CourtCentreAggregate.java
git diff HEAD~5 -- progression-domain/progression-domain-message/src/raml/json/schema/
```

Expected: no diff — aggregate and schemas untouched.