package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.UnlinkedCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UnlinkCasesEventListenerTest {

    @Mock
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @InjectMocks
    private UnlinkCasesEventListener listener;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void unlinkCases(){
        final UUID leadCaseId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final String caseUrn = "caseUrn";
        final UUID linkGroupId = UUID.randomUUID();
        final UUID entityId1 = UUID.randomUUID();
        final UUID entityId2 = UUID.randomUUID();

        final CasesUnlinked event = CasesUnlinked.casesUnlinked()
                .withProsecutionCaseId(leadCaseId)
                .withUnlinkedCases(Arrays.asList(UnlinkedCases.unlinkedCases()
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .withLinkGroupId(linkGroupId)
                        .build()))
                .build();

        final List<CaseLinkSplitMergeEntity> entityList = Arrays.asList(
                createLinkEntity(entityId1, leadCaseId, caseId, linkGroupId),
                createLinkEntity(entityId2, caseId, leadCaseId, linkGroupId)
        );

        when(caseLinkSplitMergeRepository.findByLinkGroupId(any())).thenReturn(entityList);

        listener.casesUnlinked(envelopeFrom(metadataWithRandomUUID("progression.event.cases-unlinked"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CaseLinkSplitMergeEntity> argumentCaptor = ArgumentCaptor.forClass(CaseLinkSplitMergeEntity.class);

        verify(this.caseLinkSplitMergeRepository, times(2)).remove(argumentCaptor.capture());
        final List<CaseLinkSplitMergeEntity> removedEntityList = argumentCaptor.getAllValues();
        assertThat(removedEntityList.size(), is(2));
        final CaseLinkSplitMergeEntity e1 = removedEntityList.get(0);
        final CaseLinkSplitMergeEntity e2 = removedEntityList.get(1);
        assertThat(e1.getCaseId(), Is.is(leadCaseId));
        assertThat(e2.getCaseId(), Is.is(caseId));
        assertThat(e1.getLinkGroupId(), Is.is(linkGroupId));
        assertThat(e1.getType(), Is.is(LinkType.LINK));
        assertThat(e2.getType(), Is.is(LinkType.LINK));
    }

    private CaseLinkSplitMergeEntity createLinkEntity(final UUID id, final UUID caseId, final UUID linkedCaseId, final UUID linkGroupId) {
        final CaseLinkSplitMergeEntity entity = new CaseLinkSplitMergeEntity();
        entity.setId(id);
        entity.setCaseId(caseId);
        entity.setLinkGroupId(linkGroupId);
        entity.setLinkedCaseId(linkedCaseId);
        entity.setType(LinkType.LINK);
        return entity;
    }
}
