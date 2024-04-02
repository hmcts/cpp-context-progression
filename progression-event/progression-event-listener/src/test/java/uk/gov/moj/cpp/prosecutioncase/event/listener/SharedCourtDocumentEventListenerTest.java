package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtDocumentShared.courtDocumentShared;
import static uk.gov.justice.core.courts.SharedCourtDocument.sharedCourtDocument;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtDocumentShared;
import uk.gov.justice.core.courts.CourtDocumentSharedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedCourtDocumentEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @Captor
    private ArgumentCaptor<SharedCourtDocumentEntity> sharedCourtDocumentEntityArgumentCaptor;

    @InjectMocks
    private SharedCourtDocumentEventListener sharedCourtDocumentEventListener;

    @Test
    public void shouldSaveSharedCourtDocumentToRepository() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroup = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonEnvelope courtDocumentSharedEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.court-document-shared"),
                getCourtDocumentSharedJson(courtDocumentId, hearingId, userGroup, caseId, defendantId));

        when(jsonObjectConverter.convert(courtDocumentSharedEnvelope.payloadAsJsonObject(), CourtDocumentShared.class))
                .thenReturn(getCourtDocumentShared(courtDocumentId, hearingId, userGroup, caseId, defendantId));

        sharedCourtDocumentEventListener.processCourtDocumentShared(courtDocumentSharedEnvelope);

        verify(sharedCourtDocumentRepository).save(sharedCourtDocumentEntityArgumentCaptor.capture());
        final SharedCourtDocumentEntity sharedCourtDocumentEntity = sharedCourtDocumentEntityArgumentCaptor.getValue();

        assertThat(sharedCourtDocumentEntity.getCourtDocumentId(), is(courtDocumentId));
        assertThat(sharedCourtDocumentEntity.getHearingId(), is(hearingId));
        assertThat(sharedCourtDocumentEntity.getUserGroupId(), is(userGroup));
        assertThat(sharedCourtDocumentEntity.getCaseId(), is(caseId));
        assertThat(sharedCourtDocumentEntity.getDefendantId(), is(defendantId));
        assertNull(sharedCourtDocumentEntity.getUserId());
        assertNotNull(sharedCourtDocumentEntity.getId());
    }

    @Test
    public void shouldSaveSharedCourtDocumentToRepositoryForCourtDocumentSharedV2() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroup = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonEnvelope courtDocumentSharedEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.court-document-shared-v2"),
                getCourtDocumentSharedJson(courtDocumentId, hearingId, userGroup, caseId, defendantId));

        when(jsonObjectConverter.convert(courtDocumentSharedEnvelope.payloadAsJsonObject(), CourtDocumentSharedV2.class))
                .thenReturn(getCourtDocumentSharedV2(courtDocumentId, hearingId, userGroup, caseId, defendantId));

        sharedCourtDocumentEventListener.processCourtDocumentSharedV2(courtDocumentSharedEnvelope);

        verify(sharedCourtDocumentRepository).save(sharedCourtDocumentEntityArgumentCaptor.capture());
        final SharedCourtDocumentEntity sharedCourtDocumentEntity = sharedCourtDocumentEntityArgumentCaptor.getValue();

        assertThat(sharedCourtDocumentEntity.getCourtDocumentId(), is(courtDocumentId));
        assertThat(sharedCourtDocumentEntity.getHearingId(), is(hearingId));
        assertThat(sharedCourtDocumentEntity.getUserGroupId(), is(userGroup));
        assertThat(sharedCourtDocumentEntity.getCaseId(), is(caseId));
        assertThat(sharedCourtDocumentEntity.getDefendantId(), is(defendantId));
        assertNull(sharedCourtDocumentEntity.getUserId());
        assertNotNull(sharedCourtDocumentEntity.getId());
    }

    @Test
    public void shouldNotSaveSharedCourtDocumentToRepositoryWhenCaseIDsNotPresentInEventPayload() {

        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroup = randomUUID();

        final JsonEnvelope courtDocumentSharedEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.court-document-shared"),
                getCourtDocumentSharedJsonWithoutCaseId(courtDocumentId, hearingId, userGroup));

        when(jsonObjectConverter.convert(courtDocumentSharedEnvelope.payloadAsJsonObject(), CourtDocumentShared.class))
                .thenReturn(getCourtDocumentSharedWithOutCaseIds(courtDocumentId, hearingId, userGroup));

        sharedCourtDocumentEventListener.processCourtDocumentShared(courtDocumentSharedEnvelope);

        verify(sharedCourtDocumentRepository, never()).save(any(SharedCourtDocumentEntity.class));

    }

    @Test
    public void shouldHaveSaveSharedCourtDocumentToRepositoryWhenCourtDocumentSharedV2ForApplication() {

        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroup = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonEnvelope courtDocumentSharedEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.court-document-shared-v2"),
                getCourtDocumentSharedJsonWithoutCaseId(courtDocumentId, hearingId, userGroup));

        when(jsonObjectConverter.convert(courtDocumentSharedEnvelope.payloadAsJsonObject(), CourtDocumentSharedV2.class))
                .thenReturn(getCourtDocumentSharedV2WithApplicationId(courtDocumentId, hearingId, userGroup, applicationId));

        sharedCourtDocumentEventListener.processCourtDocumentSharedV2(courtDocumentSharedEnvelope);

        verify(sharedCourtDocumentRepository).save(sharedCourtDocumentEntityArgumentCaptor.capture());
        final SharedCourtDocumentEntity sharedCourtDocumentEntity = sharedCourtDocumentEntityArgumentCaptor.getValue();

        assertThat(sharedCourtDocumentEntity.getCourtDocumentId(), is(courtDocumentId));
        assertThat(sharedCourtDocumentEntity.getHearingId(), is(hearingId));
        assertThat(sharedCourtDocumentEntity.getUserGroupId(), is(userGroup));
        assertThat(sharedCourtDocumentEntity.getApplicationId(), is(applicationId));
        assertNull(sharedCourtDocumentEntity.getUserId());
        assertNotNull(sharedCourtDocumentEntity.getId());


    }


    private CourtDocumentShared getCourtDocumentShared(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup, final UUID caseId, final UUID defendantId) {
        return courtDocumentShared()
                .withSharedCourtDocument(sharedCourtDocument()
                        .withCourtDocumentId(courtDocumentId)
                        .withHearingId(hearingId)
                        .withUserGroupId(userGroup)
                        .withCaseIds(Collections.singletonList(caseId))
                        .withDefendantId(defendantId)
                        .build())
                .build();
    }

    private CourtDocumentSharedV2 getCourtDocumentSharedV2(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup, final UUID caseId, final UUID defendantId) {
        return CourtDocumentSharedV2.courtDocumentSharedV2()
                .withSharedCourtDocument(sharedCourtDocument()
                        .withCourtDocumentId(courtDocumentId)
                        .withHearingId(hearingId)
                        .withUserGroupId(userGroup)
                        .withCaseIds(Collections.singletonList(caseId))
                        .withDefendantId(defendantId)
                        .build())
                .build();
    }

    private CourtDocumentShared getCourtDocumentSharedWithOutCaseIds(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup) {
        return courtDocumentShared()
                .withSharedCourtDocument(sharedCourtDocument()
                        .withCourtDocumentId(courtDocumentId)
                        .withHearingId(hearingId)
                        .withUserGroupId(userGroup)
                        .build())
                .build();
    }

    private CourtDocumentSharedV2 getCourtDocumentSharedV2WithApplicationId(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup, final UUID applicationId) {
        return CourtDocumentSharedV2.courtDocumentSharedV2()
                .withSharedCourtDocument(sharedCourtDocument()
                        .withCourtDocumentId(courtDocumentId)
                        .withHearingId(hearingId)
                        .withUserGroupId(userGroup)
                        .withApplicationId(applicationId)
                        .build())
                .build();
    }

    private JsonObject getCourtDocumentSharedJson(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup, final UUID caseId, final UUID defendantId) {
        return createObjectBuilder()
                .add("sharedCourtDocument", createObjectBuilder()
                        .add("courtDocumentId", courtDocumentId.toString())
                        .add("hearingId", hearingId.toString())
                        .add("userGroup", userGroup.toString())
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString())
                        .build())
                .build();
    }

    private JsonObject getCourtDocumentSharedJsonWithoutCaseId(final UUID courtDocumentId, final UUID hearingId, final UUID userGroup) {
        return createObjectBuilder()
                .add("sharedCourtDocument", createObjectBuilder()
                        .add("courtDocumentId", courtDocumentId.toString())
                        .add("hearingId", hearingId.toString())
                        .add("userGroup", userGroup.toString())
                        .build())
                .build();
    }
}