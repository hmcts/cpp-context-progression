package uk.gov.justice.api.resource.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.service.HearingQueryService.HEARING_GET_DRAFT_RESULT_V2;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultLine;
import uk.gov.justice.api.resource.utils.FileUtil;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingQueryServiceTest {

    @Mock
    private Requester requester;

    @Spy
    private JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private HearingQueryService hearingQueryService;

    @Test
    public void shouldGetResultAmendmentLogFromDraftResults() {

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID defendantId = UUID.fromString("119107bc-8e34-4bca-b986-9ab2e280efad");

        when(requester.request(any(Envelope.class))).thenReturn(JsonEnvelopeBuilder.envelope()
                .withPayloadFrom(FileUtil.jsonFromPath("hearing-results/payload-hearing-get-draft-result-v2.json"))
                .with(metadataWithRandomUUID(HEARING_GET_DRAFT_RESULT_V2).withCausation(randomUUID())).build());

        final List<DraftResultsWrapper> defendantSharedResults = hearingQueryService.getDraftResultsWithAmendments(randomUUID(), hearingId, List.of(hearingDay));

        assertThat(defendantSharedResults.size(), is(1));
        assertThat(defendantSharedResults.get(0).getResultLines().size(), is(4));
        assertThat(defendantSharedResults.stream().flatMap(dr -> dr.getResultLines().stream()).allMatch(rl -> rl.getDefendantId().equals(defendantId)), is(true));
    }
}