package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import uk.gov.moj.cpp.progression.events.LinkCases;
import uk.gov.moj.cpp.progression.events.MergeCases;
import uk.gov.moj.cpp.progression.events.SplitCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LinkSplitMergeCasesEventListenerTest {

    @Mock
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private SearchProsecutionCaseRepository searchCaseRepository;

    @InjectMocks
    private LinkSplitMergeCasesEventListener listener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void linkCases() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final List<String> caseUrns = Arrays.asList("caseUrn1");
        final String caseId = UUID.randomUUID().toString();

        final LinkCases event = LinkCases.linkCases()
                .withProsecutionCaseId(prosecutionCaseId)
                .withCaseUrns(caseUrns)
                .build();

        final SearchProsecutionCaseEntity searchProsecutionCaseEntity = new SearchProsecutionCaseEntity();
        searchProsecutionCaseEntity.setCaseId(caseId);
        searchProsecutionCaseEntity.setReference(caseUrns.get(0));

        when(searchCaseRepository.findByCaseUrn(any())).thenReturn(Arrays.asList(searchProsecutionCaseEntity));

        listener.linkCases(envelopeFrom(metadataWithRandomUUID("progression.event.link-cases"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CaseLinkSplitMergeEntity> argumentCaptor = ArgumentCaptor.forClass(CaseLinkSplitMergeEntity.class);
        verify(this.caseLinkSplitMergeRepository, times(2)).save(argumentCaptor.capture());
        final List<CaseLinkSplitMergeEntity> savedEntityList = argumentCaptor.getAllValues();

        final CaseLinkSplitMergeEntity e1 = savedEntityList.get(0);
        final CaseLinkSplitMergeEntity e2 = savedEntityList.get(1);

        assertThat(e1.getCaseId(), is(e2.getLinkedCaseId()));
        assertThat(e2.getCaseId(), is(e1.getLinkedCaseId()));
        assertThat(e1.getLinkGroupId(), is(e2.getLinkGroupId()));
        assertThat(e1.getType(), is(LinkType.LINK));
        assertThat(e2.getType(), is(LinkType.LINK));

    }

    @Test
    public void mergeCases() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final List<String> caseUrns = Arrays.asList("caseUrnToMerge");
        final String caseId = UUID.randomUUID().toString();

        final MergeCases event = MergeCases.mergeCases()
                .withProsecutionCaseId(prosecutionCaseId)
                .withCaseUrns(caseUrns)
                .build();

        final SearchProsecutionCaseEntity entityToMerge = new SearchProsecutionCaseEntity();
        entityToMerge.setCaseId(caseId);
        entityToMerge.setReference(caseUrns.get(0));

        final SearchProsecutionCaseEntity leadCaseEntity = new SearchProsecutionCaseEntity();
        leadCaseEntity.setCaseId(prosecutionCaseId.toString());
        leadCaseEntity.setReference("leadCaseUrn");

        when(searchCaseRepository.findByCaseUrn(any())).thenReturn(Arrays.asList(entityToMerge)); //to get case id with given urn (queried to get case ids for the given case urns to merge)
        when(searchCaseRepository.findByCaseId(any())).thenReturn(Arrays.asList(leadCaseEntity)); //to get urn with given case id (used in giving lead case id and returning lead case's urn)

        listener.mergeCases(envelopeFrom(metadataWithRandomUUID("progression.event.merge-cases"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CaseLinkSplitMergeEntity> argumentCaptor = ArgumentCaptor.forClass(CaseLinkSplitMergeEntity.class);
        verify(this.caseLinkSplitMergeRepository, times(2)).save(argumentCaptor.capture());
        final List<CaseLinkSplitMergeEntity> savedEntityList = argumentCaptor.getAllValues();

        final CaseLinkSplitMergeEntity e1 = savedEntityList.get(0);
        final CaseLinkSplitMergeEntity e2 = savedEntityList.get(1);

        assertThat(e1.getCaseId(), is(e2.getLinkedCaseId()));
        assertThat(e2.getCaseId(), is(e1.getLinkedCaseId()));
        assertThat(e1.getLinkGroupId(), is(e2.getLinkGroupId()));
        assertThat(e1.getType(), is(LinkType.MERGE));
        assertThat(e2.getType(), is(LinkType.MERGE));
        assertThat(e1.getReference(), is(caseUrns.get(0) + "/M"));
        assertThat(e2.getReference(), is(leadCaseEntity.getReference()));

    }

    @Test
    public void splitCases() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final List<String> caseUrns = Arrays.asList("caseUrn/1", "caseUrn/2", "caseUrn/3");

        final SplitCases event = SplitCases.splitCases()
                .withProsecutionCaseId(prosecutionCaseId)
                .withCaseUrns(caseUrns)
                .build();

        listener.splitCases(envelopeFrom(metadataWithRandomUUID("progression.event.split-cases"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CaseLinkSplitMergeEntity> argumentCaptor = ArgumentCaptor.forClass(CaseLinkSplitMergeEntity.class);
        verify(this.caseLinkSplitMergeRepository).save(argumentCaptor.capture());
        final CaseLinkSplitMergeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getCaseId(), is(prosecutionCaseId));
        assertThat(savedEntity.getLinkedCaseId(), is(prosecutionCaseId));
        assertThat(savedEntity.getType(), is(LinkType.SPLIT));
        final String references = String.join(",", caseUrns);
        assertEquals(savedEntity.getReference(), references);

    }

}
