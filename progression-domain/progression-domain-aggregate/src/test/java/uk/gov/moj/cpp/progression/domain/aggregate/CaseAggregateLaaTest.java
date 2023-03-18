package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAggregateLaaTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Test
    public void testResendLaaOutcomeConcluded() throws IOException {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withValuesFrom(convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class))
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId).build();

        final Stream<Object> eventStream = new CaseAggregate().resendLaaOutcomeConcluded(laaDefendantProceedingConcludedChanged);
        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent = (LaaDefendantProceedingConcludedResent) eventStream.findFirst().get();
        assertThat(laaDefendantProceedingConcludedResent.getLaaDefendantProceedingConcludedChanged().getProsecutionCaseId(), is(caseId));
        assertThat(laaDefendantProceedingConcludedResent.getLaaDefendantProceedingConcludedChanged().getHearingId(), is(hearingId));
    }


    public <T> T convertFromFile(final String url, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(new File(this.getClass().getClassLoader().getResource(url).getFile()), clazz);
    }
}
