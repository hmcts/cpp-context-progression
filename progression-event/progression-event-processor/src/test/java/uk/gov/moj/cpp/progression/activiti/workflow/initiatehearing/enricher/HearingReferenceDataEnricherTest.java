package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.enricher;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.INITIATE_HEARING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.external.domain.hearing.Case;
import uk.gov.moj.cpp.external.domain.hearing.Defendant;
import uk.gov.moj.cpp.external.domain.hearing.Hearing;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Judge;
import uk.gov.moj.cpp.external.domain.hearing.Offence;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingReferenceDataEnricherTest {

    @InjectMocks
    private HearingReferenceDataEnricher testObj;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    ReferenceDataService referenceDataService;

    final ArgumentCaptor<InitiateHearing> captor = ArgumentCaptor.forClass(InitiateHearing.class);

    @Before
    public void setUp() throws Exception {
        testObj.jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(testObj.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldEnrichReferenceData_Judge_And_CourtRoom() throws Exception {
        Offence offence = new Offence(UUID.randomUUID());
        Defendant defendant = new Defendant(UUID.randomUUID());
        defendant.getOffences().add(offence);
        UUID courtRoomID = UUID.randomUUID();

        //Given
        final Hearing hearing = new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                courtRoomID,
                new Judge(UUID.randomUUID()),
                new StringGenerator().next(),
                Collections.singletonList(new StringGenerator().next()),
                Arrays.asList(defendant)
        );

        final List<Case> cases = Arrays.asList(new Case(UUID.randomUUID(),new StringGenerator().next()));

        final InitiateHearing initiateHearing = new InitiateHearing(cases, hearing);

        when(referenceDataService.getCourtCentreById(any(), any())).thenReturn(Optional.of(getCourtRoom(courtRoomID)));
        when(referenceDataService.getJudgeById(any(), any())).thenReturn(Optional.of(getJudge()));
        when(delegateExecution.getVariable(INITIATE_HEARING_PAYLOAD)).thenReturn(initiateHearing);
        when(delegateExecution.getVariable(USER_ID)).thenReturn(UUID.randomUUID().toString());


        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final InitiateHearing result = captor.getValue();
        assertThat(initiateHearing.equals(result), equalTo(true));

    }

    public JsonObject getCourtRoom(UUID courtRoomID) {
        final JsonObject courtRoomJsonObject = Json.createObjectBuilder()
                        .add("name", new StringGenerator().next())
                        .add("courtRooms", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                        .add("id", courtRoomID.toString())
                                                        .add("name", "xyz").build()).build())
                        .build();
        return courtRoomJsonObject;
    }

    public JsonObject getJudge() {
        final JsonObject judgeJsonObject = Json.createObjectBuilder()
                        .add("firstName", "firstName")
                        .add("lastName", "lastName")
                        .add("title","title")
                        .build();
        return judgeJsonObject;
    }

}
