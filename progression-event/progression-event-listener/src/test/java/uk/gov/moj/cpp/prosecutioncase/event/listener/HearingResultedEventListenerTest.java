package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.court.HearingAddMissingResultsBdf;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultedEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingResultedEventListener hearingResultedEventListener;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Mock
    private JsonEnvelope jsonEnvelope;

    private UUID hearingId;
    private String hearingAddMissingResultsBdfEventPayload;
    private final StringToJsonObjectConverter converter = new StringToJsonObjectConverter();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }
    @Test
    public void updateResultedHearing() throws IOException {
        hearingAddMissingResultsBdfEventPayload = FileUtil.getPayload("json/hearing-add-missing-results.json");
        JsonObject jsonObject = converter.convert(hearingAddMissingResultsBdfEventPayload);
        final HearingAddMissingResultsBdf hearingAddMissingResultsBdf = this.jsonObjectToObjectConverter.convert(jsonObject, HearingAddMissingResultsBdf.class);
        hearingId = hearingAddMissingResultsBdf.getHearingId();
        final HearingEntity hearingEntity = createHearingEntity();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(converter.convert(hearingAddMissingResultsBdfEventPayload));
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingResultedEventListener.processHearingAddMissingResults(jsonEnvelope);


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream().filter(savedDbHeairng -> savedDbHeairng.getHearingId().equals(hearingId))
                .findFirst().get();

        final Hearing savedHearingObject = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearingObject, notNullValue());
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final ProsecutionCase prosecutionCase = savedHearingObject.getProsecutionCases()
                .stream().filter(aCase -> aCase.getId().equals(hearingAddMissingResultsBdf.getProsecutionCaseId()))
                .findFirst().get();
        assertThat(prosecutionCase, notNullValue());
        final Defendant defendant = prosecutionCase.getDefendants()
                .stream().filter(aDef -> aDef.getId().equals(hearingAddMissingResultsBdf.getDefendantId()))
                .findFirst().get();
        assertThat(defendant, notNullValue());

        assertThat(defendant.getProceedingsConcluded(), nullValue());
        assertTrue(defendant.getDefendantCaseJudicialResults().containsAll(hearingAddMissingResultsBdf.getDefendantCaseJudicialResults()));

        final Offence offence = defendant.getOffences().stream().filter(aOff -> aOff.getId().equals(hearingAddMissingResultsBdf.getOffenceId())).findFirst().get();
        assertThat(offence, notNullValue());
        assertTrue(offence.getJudicialResults().containsAll(hearingAddMissingResultsBdf.getOffenceJudicialResults()));
    }

    private HearingEntity createHearingEntity() throws IOException {
        String hearingPayload1 =  FileUtil.getPayload("json/hearing.json");

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload1);
        return hearingEntity;
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {
        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }
        return object;
    }
}
