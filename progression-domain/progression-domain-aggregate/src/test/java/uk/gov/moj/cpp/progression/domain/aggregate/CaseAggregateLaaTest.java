package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAggregateLaaTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();


    @Test
    public void testResendLaaOutcomeConcluded() throws IOException {
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged1 = LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withValuesFrom(convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class, hearingId1.toString()))
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId1).build();

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged2 = LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withValuesFrom(convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class, hearingId2.toString()))
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId2).build();

        final List<Object> events = new CaseAggregate().resendLaaOutcomeConcluded(asList(laaDefendantProceedingConcludedChanged1, laaDefendantProceedingConcludedChanged2)).collect(toList());

        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent1 = (LaaDefendantProceedingConcludedResent) events.get(0);
        assertThat(laaDefendantProceedingConcludedResent1.getLaaDefendantProceedingConcludedChanged().getProsecutionCaseId(), is(caseId));
        assertThat(laaDefendantProceedingConcludedResent1.getLaaDefendantProceedingConcludedChanged().getHearingId(), is(hearingId1));

        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent2 = (LaaDefendantProceedingConcludedResent) events.get(1);
        assertThat(laaDefendantProceedingConcludedResent2.getLaaDefendantProceedingConcludedChanged().getProsecutionCaseId(), is(caseId));
        assertThat(laaDefendantProceedingConcludedResent2.getLaaDefendantProceedingConcludedChanged().getHearingId(), is(hearingId2));

    }


    public <T> T convertFromFile(final String url, final Class<T> clazz, String hearingId) throws IOException {
        final String content = readFileToString(new File(this.getClass().getClassLoader().getResource(url).getFile())).replace("HEARING_ID", hearingId);
        return OBJECT_MAPPER.readValue(content, clazz);
    }
}
