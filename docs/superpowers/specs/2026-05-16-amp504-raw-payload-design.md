# AMP-504: `rawPayload` Forwarding to Subscription Service — Progression Design Spec

**JIRA:** AMP-504
**Repo:** `cpp-context-progression` (CQRS / Maven / Java EE CDI)
**Branch:** `dev/AMP-504`
**Date:** 2026-05-16
**Status:** Approved for implementation

---

## 1. Requirement

The downstream subscription service (`service-cp-crime-hearing-results-document-subscription`) requires a `rawPayload` field on the inbound `EventPayload` POST it receives from progression. This field must contain the **full source data that was used to generate the PCR document** via the system document generator and material service, so that subscription consumers can retrieve it via `GET /notifications/{notificationId}/payload`.

---

## 2. Reverse-Engineered PCR Document Generation Flow

```
progression.command.add-prison-court-register
  └─ PrisonCourtRegisterHandler.handleAddPrisonCourtRegister()
  └─ CourtCentreAggregate.createPrisonCourtRegister()
  └─ emits: progression.event.prison-court-register-recorded

progression.event.prison-court-register-recorded
  └─ PrisonCourtRegisterEventProcessor.generatePrisonCourtRegister()
       │
       ├─ [1] PrisonCourtRegisterPdfPayloadGenerator.mapPayload(payload)
       │        produces: mappedPayload (JsonObject)
       │        fields: registerDate, ljaName, courtHouse, courtHouseAddress,
       │                custodyLocation, cases[] {
       │                  name, address1-5, postCode, jurisdiction, hearingType,
       │                  defendantAppearanceDetails, attendingSolicitorName,
       │                  defendantType, postHearingCustodyStatus, dateOfBirth,
       │                  age, gender, nationality, aliases, caseReference,
       │                  dateOfHearing, prosecutorName, arrestSummonsNumber,
       │                  prosecutionCounselName, prosecutionCounselStatus,
       │                  defenceCounselName, defenceCounselStatus,
       │                  defendantResults[], caseResults[], offences[], applications[]
       │                }
       │
       ├─ [2] fileService.storePayload(mappedPayload, fileName, "OEE_Layout5")
       │        → stores mappedPayload JSON bytes to File Service
       │        → returns: fileId (UUID)           ← pointer to source data
       │
       ├─ [3] systemDocGeneratorService.generateDocument(
       │          DocumentGenerationRequest {
       │            originatingSource: "PRISON_COURT_REGISTER",
       │            templateIdentifier: "OEE_Layout5",
       │            conversionFormat: PDF,
       │            sourceCorrelationId: prisonCourtRegisterStreamId,
       │            payloadFileServiceId: fileId          ← system-doc-generator reads this
       │          })
       │        → system-doc-generator fetches mappedPayload from File Service via fileId
       │        → renders OEE_Layout5 PDF
       │        → stores PDF in Material service → returns: materialId
       │
       └─ [4] sender.send(record-prison-court-register-document-sent command)
                payload: { courtCentreId, defendant, hearingVenue, hearingId,
                           hearingDate, payloadFileId=fileId, prisonCourtRegisterStreamId,
                           recipients, id }

progression.command.record-prison-court-register-document-sent
  └─ PrisonCourtRegisterHandler.recordPrisonCourtRegisterDocumentSent()
  └─ CourtCentreAggregate.recordPrisonCourtRegisterDocumentSent()
  └─ emits: progression.event.prison-court-register-sent
       → apply() stores PrisonCourtRegisterDocumentRequest in prisonCourtRegisterMap
                 keyed by payloadFileId

[system-doc-generator generates PDF → materialId]

progression.command.notify-prison-court-register   (sent by system-doc-generator)
  └─ PrisonCourtRegisterHandler.handleNotifyCourtCentre()
  └─ CourtCentreAggregate.recordPrisonCourtRegisterGenerated()
       → retrieves PrisonCourtRegisterDocumentRequest from prisonCourtRegisterMap.get(payloadFileId)
       → emits: progression.event.prison-court-register-generated-v2 (feature-guarded)
                { courtCentreId, materialId, fileId, hearingId, hearingDate,
                  hearingVenue, recipients, defendant, id }

progression.event.prison-court-register-generated-v2
  └─ PrisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2()   ← subscription call here
  └─ maps to PcrEventPayload → POST to subscription service
```

---

## 3. The `rawPayload` Source

The `rawPayload` is the **`mappedPayload`** produced by `PrisonCourtRegisterPdfPayloadGenerator.mapPayload()` at step [1] above.

This is the actual input to the system document generator — the exact JSON that is stored in File Service (`fileId`) and rendered by the `OEE_Layout5` template into the PCR PDF. It contains the fully expanded court register: all defendant details, case references, offences with results, applications, counsel details, hearing venue, and register date.

### Why not the `prison-court-register-generated-v2` event payload itself?

The v2 event is a trimmed notification event. It carries `defendant` (name, DOB, case URNs) and `hearingVenue`, but **not** the expanded offences, results, applications, or custody/hearing details that are in `mappedPayload`. The `mappedPayload` is the true source — `prison-court-register-generated-v2` is produced *after* document generation and carries only routing/notification fields.

---

## 4. Design: Fetch from File Service in `sendPrisonCourtRegisterV2()`

The `mappedPayload` is already stored in the File Service (via `fileService.storePayload()` in `generatePrisonCourtRegister()`), and `fileId` is already carried in `PrisonCourtRegisterGeneratedV2.getFileId()`. This means `sendPrisonCourtRegisterV2()` can retrieve the payload directly from the File Service without any event store or aggregate changes.

`FileRetriever.retrieve(UUID)` is available in the `file-service-api` JAR already on the classpath. `FileService` is already injected in `PrisonCourtRegisterEventProcessor`.

### 4.1 Data flow

```
generatePrisonCourtRegister()          ← NO CHANGE
  mappedPayload created
  fileService.storePayload(mappedPayload) → fileId (payload stored in File Service)
  command: record-prison-court-register-document-sent (no change)

CourtCentreAggregate                   ← NO CHANGE

progression.event.prison-court-register-generated-v2  ← NO CHANGE
  { ..existing fields.., fileId }      ← fileId already present

sendPrisonCourtRegisterV2()            ← ONLY THIS HANDLER CHANGES
  fileId = prisonCourtRegisterGenerated.getFileId()
  JsonObject mappedPayload = fileService.retrievePayload(fileId)
                                           ↑ new method using FileRetriever.retrieve(UUID)
  Map<String,Object> rawPayload = objectMapper.convertValue(mappedPayload, Map.class)
  PcrEventPayload { ..., rawPayload } → POST → subscription service
```

### 4.2 `FileService` extension — add `retrievePayload()`

Inject `FileRetriever` alongside the existing `FileStorer` and add a read method:

```java
@Inject
private FileRetriever fileRetriever;

public Optional<JsonObject> retrievePayload(final UUID fileId) {
    try {
        return fileRetriever.retrieve(fileId).map(ref -> {
            try (InputStream stream = ref.getContentStream()) {
                return Json.createReader(
                    new java.io.StringReader(
                        new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    )).readObject();
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
```

Imports to add: `uk.gov.justice.services.fileservice.api.FileRetriever`, `uk.gov.justice.services.fileservice.domain.FileReference`, `java.io.InputStream`, `java.io.StringReader`.

### 4.3 `PrisonCourtRegisterEventProcessor` — `sendPrisonCourtRegisterV2()` changes

Inject `ObjectMapper` and call `fileService.retrievePayload(fileId)`:

```java
@Inject
private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
```

In `sendPrisonCourtRegisterV2()`, replace the existing mapper call:

```java
final UUID fileId = prisonCourtRegisterGenerated.getFileId();
final Map<String, Object> rawPayload = fileService.retrievePayload(fileId)
    .map(sp -> objectMapper.convertValue(sp, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){}))
    .orElseGet(() -> objectMapper.convertValue(
        prisonCourtRegisterGenerated,
        new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){}));

PcrEventPayload pcrEventPayload = hearingResultsDocumentSubscriptionPCRMapper
    .mapPcrForhearingResultsDocument(prisonCourtRegisterGenerated, emailRecipient, createdAt, rawPayload);
```

The `.orElseGet()` is the backward-compat fallback — if File Service does not hold the payload (e.g., it has been purged), the v2 event JSON itself is used as `rawPayload`.

### 4.4 DTO change — `PcrEventPayload`

```java
// Add to existing Lombok @Builder class:
private Map<String, Object> rawPayload;
```

### 4.5 Mapper change — `HearingResultsDocumentSubscriptionPCRMapper`

Update signature and fix the `timestamp` bug (currently uses `Instant.now()` instead of `createdAt`):

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
            .timestamp(createdAt)           // FIXED: was Instant.now()
            .defendant(mapDefendant(pcrIn, prisonEmail))
            .rawPayload(rawPayload)
            .build();
}
```

---

## 5. Backward Compatibility

No event store changes — zero backward-compatibility concerns for event replay.

The `.orElseGet()` fallback in `sendPrisonCourtRegisterV2()` handles the edge case where `FileService.retrievePayload()` returns empty (file purged or unavailable). In that case `rawPayload` is the v2 event itself serialised to a map — still a valid, non-null payload for the subscription service.

---

## 6. Impact Analysis

| File | Change | Type |
|---|---|---|
| `FileService.java` | Add `retrievePayload(UUID)` using `FileRetriever` | Service |
| `PrisonCourtRegisterEventProcessor.java` | Inject `ObjectMapper`; call `fileService.retrievePayload(fileId)`; pass `rawPayload` to mapper | Event Processor |
| `PcrEventPayload.java` | Add `Map<String,Object> rawPayload` field | DTO |
| `HearingResultsDocumentSubscriptionPCRMapper.java` | Add `rawPayload` param; fix `timestamp` bug | Mapper |
| `HearingResultsDocumentSubscriptionPCRMapperTest.java` | Update 3-arg calls to 4-arg; add `rawPayload` + `timestamp` assertions | Test |
| `PrisonCourtRegisterEventProcessorTest.java` | Mock `fileService.retrievePayload()`; assert `rawPayload` passed to mapper | Test |
| `FileServiceTest.java` | Add tests for `retrievePayload()` | Test |
| `api-cp-crime-hearing-results-document-subscription` OpenAPI spec | Add `rawPayload: {type: object, additionalProperties: true}` to `EventPayload` | Separate repo |

**Not changed:**
- No JSON schema changes in `progression-domain-message`
- No POJO regeneration
- No `CourtCentreAggregate` changes
- No `PrisonCourtRegisterHandler` changes

### Additional bug fixes (in scope)

- **`HearingResultsDocumentSubscriptionPCRMapper`**: `timestamp` is always `Instant.now()` despite `createdAt` being passed as a parameter — fixed.

---

## 7. Technical Notes

### Why not thread through the CQRS event chain?

The CQRS handlers (`generatePrisonCourtRegister` and `sendPrisonCourtRegisterV2`) run in separate JVM invocations connected by the event store and message bus. There is no shared in-memory state. The payload must be persisted between them — File Service is already that persistence layer.

### File Service read latency

`FileRetriever.retrieve(UUID)` adds one synchronous read to the File Service per v2 event processed. The system document generator already performs a File Service read on the same payload — this adds a second read per PCR. Under normal load this is acceptable. Monitor P95 latency of `sendPrisonCourtRegisterV2()` post-deploy.

### Feature guard alignment

`prison-court-register-generated-v2` is only emitted when `FEATURE_HEARING_RESULTS_DOCUMENT_SUBSCRIPTION_ENABLED` is on. The File Service read in `sendPrisonCourtRegisterV2()` is inside that feature-guarded event handler — no impact when the feature is off.