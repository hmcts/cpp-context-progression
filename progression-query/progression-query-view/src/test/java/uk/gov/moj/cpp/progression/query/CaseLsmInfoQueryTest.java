package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.query.utils.CaseLsmInfoConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseLsmInfoQueryTest {
    private static final UUID MASTER_DEFENDANT_1 = fromString("b44fa9bb-dc36-4375-83a9-ff1bc4cd4374");
    private static final UUID MASTER_DEFENDANT_2 = fromString("1f1fba3c-34ee-454e-b3be-5fb845b2de4a");
    private static final String CASE_URN = "CN12345";
    private static final String RELATED_URN = "CN54321";
    private static final String CASE_URN_M = "CN12345/M";
    private static final UUID CASE_ID = randomUUID();

    @InjectMocks
    private CaseLsmInfoQuery caseLsmInfoQuery;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseLsmInfoConverter caseLsmInfoConverter;

    @Mock
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    private JsonEnvelope envelope;


    @BeforeEach
    public void setUp() {
        JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        envelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.case-lsm-info"), payload);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(randomUUID());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        when(jsonObjectToObjectConverter.convert(any(), eq(ProsecutionCase.class))).thenReturn(createProsecutionCase(RELATED_URN));

    }

    @Test
    public void shouldReturnMatchedCases() {
        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = Arrays.asList(
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_1, true),
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_2, false)
        );

        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(matchDefendantCaseHearingEntities);

        when(caseLinkSplitMergeRepository.findByCaseId(any()))
                .thenReturn(new ArrayList<>());

        when(caseLsmInfoConverter.convertMatchedCaseDefendants(any(),any(),any()))
                .thenReturn(Json.createArrayBuilder());

        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNotNull(matchedDefendantCasesArray);
        assertThat(matchedDefendantCasesArray.size(), is(2));
        assertThat(matchedDefendantCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN));
        assertThat(matchedDefendantCasesArray.getJsonObject(0).getString("relatedUrn"), is(RELATED_URN));

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNull(linkedCasesArray);

        when(jsonObjectToObjectConverter.convert(any(), eq(ProsecutionCase.class))).thenReturn(createProsecutionCase(null));

        JsonEnvelope responseEnvelope2 = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload2 = responseEnvelope2.payloadAsJsonObject();
        JsonArray matchedDefendantCasesArray2 = responsePayload2.getJsonArray("matchedDefendantCases");
        assertThat(matchedDefendantCasesArray2.getJsonObject(0).getString("caseUrn"), is(CASE_URN));
        assertThat(matchedDefendantCasesArray2.getJsonObject(0).containsKey("relatedUrn"), is(false));
    }


    @Test
    public void shouldReturnEmptyWhenNoCaseFound() {
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(new ArrayList<>());
        when(caseLinkSplitMergeRepository.findByCaseId(any()))
                .thenReturn(new ArrayList<>());
        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();

        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNull(matchedDefendantCasesArray);

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNull(linkedCasesArray);
    }

    @Test
    public void shouldReturnLinkedCases() {
        final List<CaseLinkSplitMergeEntity> caseLinkSplitMergeEntities = Arrays.asList(
                buildCaseLinkSplitMergeEntity(MASTER_DEFENDANT_1, LinkType.LINK, true),
                buildCaseLinkSplitMergeEntity(MASTER_DEFENDANT_2, LinkType.LINK, false)
        );
        when(caseLinkSplitMergeRepository.findByCaseId(any())).thenReturn(caseLinkSplitMergeEntities);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(new ArrayList<>());


        when(caseDefendantHearingRepository.findByCaseId(any()))
                .thenReturn(Arrays.asList(createCaseDefendantHearingEntity()));

        when(caseLsmInfoConverter.convertRelatedCaseDefendants(any(),any()))
                .thenReturn(Json.createArrayBuilder());

        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();

        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNull(matchedDefendantCasesArray);

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNotNull(linkedCasesArray);
        assertThat(linkedCasesArray.size(), is(2));
        assertThat(linkedCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN));
    }

    @Test
    public void shouldReturnMergedCases() {
        final List<CaseLinkSplitMergeEntity> caseLinkSplitMergeEntities = Arrays.asList(
                buildCaseLinkSplitMergeEntity(MASTER_DEFENDANT_1, LinkType.LINK, true),
                buildCaseLinkSplitMergeEntity(MASTER_DEFENDANT_2, LinkType.MERGE, false)
        );
        when(caseLinkSplitMergeRepository.findByCaseId(any())).thenReturn(caseLinkSplitMergeEntities);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(new ArrayList<>());


        when(caseDefendantHearingRepository.findByCaseId(any()))
                .thenReturn(Arrays.asList(createCaseDefendantHearingEntity()));

        when(caseLsmInfoConverter.convertRelatedCaseDefendants(any(),any())).thenReturn(Json.createArrayBuilder());

        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();

        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNull(matchedDefendantCasesArray);

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNotNull(linkedCasesArray);
        assertThat(linkedCasesArray.size(), is(1));
        assertThat(linkedCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN));

        JsonArray mergedCasesArray = responsePayload.getJsonArray("mergedCases");
        assertNotNull(mergedCasesArray);
        assertThat(mergedCasesArray.size(), is(1));

        assertThat(mergedCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN_M));
    }

    @Test
    public void shouldReturnCasesWithoutDuplicatesEvenWhenOneHearingIsNull() {
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = Arrays.asList(
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_1, hearingId, prosecutionCaseId, true),
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_1, null, prosecutionCaseId, false)
        );

        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(matchDefendantCaseHearingEntities);

        when(caseLinkSplitMergeRepository.findByCaseId(any()))
                .thenReturn(new ArrayList<>());

        when(caseLsmInfoConverter.convertMatchedCaseDefendants(any(),any(),any()))
                .thenReturn(Json.createArrayBuilder());

        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNotNull(matchedDefendantCasesArray);
        assertThat(matchedDefendantCasesArray.size(), is(1));
        assertThat(matchedDefendantCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN));

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNull(linkedCasesArray);
    }

    @Test
    public void shouldReturnCasesWithoutDuplicatesWithUniqueHearingIds() {
        final UUID prosecutionCaseId = randomUUID();
        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = Arrays.asList(
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_1, randomUUID(), prosecutionCaseId, true),
                buildMatchDefendantCaseEntity(MASTER_DEFENDANT_1, randomUUID(), prosecutionCaseId, false)
        );

        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList()))
                .thenReturn(matchDefendantCaseHearingEntities);

        when(caseLinkSplitMergeRepository.findByCaseId(any()))
                .thenReturn(new ArrayList<>());

        when(caseLsmInfoConverter.convertMatchedCaseDefendants(any(),any(),any()))
                .thenReturn(Json.createArrayBuilder());

        JsonEnvelope responseEnvelope = caseLsmInfoQuery.getCaseLsmInfo(envelope);
        JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        JsonArray matchedDefendantCasesArray = responsePayload.getJsonArray("matchedDefendantCases");
        assertNotNull(matchedDefendantCasesArray);
        assertThat(matchedDefendantCasesArray.size(), is(2));
        assertThat(matchedDefendantCasesArray.getJsonObject(0).getString("caseUrn"), is(CASE_URN));

        JsonArray linkedCasesArray = responsePayload.getJsonArray("linkedCases");
        assertNull(linkedCasesArray);
    }

    private MatchDefendantCaseHearingEntity buildMatchDefendantCaseEntity(UUID masterDefendantId, boolean hearingExists) {
        MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(randomUUID());
        entity.setDefendantId(randomUUID());
        entity.setHearing(hearingExists ? new HearingEntity() : null);
        entity.setProsecutionCase(new ProsecutionCaseEntity());
        return entity;
    }

    private MatchDefendantCaseHearingEntity buildMatchDefendantCaseEntity(final UUID masterDefendantId, final UUID hearingId, final UUID prosecutionCaseId, boolean hearingExists) {
        MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(hearingId);
        entity.setDefendantId(masterDefendantId);
        entity.setHearing(hearingExists ? new HearingEntity() : null);
        entity.setProsecutionCaseId(prosecutionCaseId);
        entity.setProsecutionCase(new ProsecutionCaseEntity());
        return entity;
    }

    private CaseLinkSplitMergeEntity buildCaseLinkSplitMergeEntity(UUID masterDefendantId, LinkType linkType, boolean hearingExists) {
        CaseLinkSplitMergeEntity entity = new CaseLinkSplitMergeEntity();
        entity.setId(randomUUID());
        entity.setCaseId(masterDefendantId);
        entity.setLinkedCaseId(randomUUID());
        if (linkType == LinkType.MERGE) {
            entity.setReference(CASE_URN_M);
        }
        entity.setType(linkType);
        entity.setLinkedCase(new ProsecutionCaseEntity());
        entity.setLinkGroupId(randomUUID());
        return entity;
    }

    private CaseDefendantHearingEntity createCaseDefendantHearingEntity() {
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        return caseDefendantHearingEntity;
    }

    private ProsecutionCase createProsecutionCase(final String relatedUrn) {
        final ProsecutionCase.Builder builder = ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withDefendants(Arrays.asList(
                        Defendant.defendant().withMasterDefendantId(MASTER_DEFENDANT_1).build(),
                        Defendant.defendant().withMasterDefendantId(MASTER_DEFENDANT_2).build()
                ))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(CASE_URN)
                        .build());

        if (Objects.nonNull(relatedUrn)) {
            builder.withRelatedUrn(relatedUrn);
        }

        return builder.build();
    }
}
