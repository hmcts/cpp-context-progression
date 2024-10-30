package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefendantRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CotrQueryServiceTest {

    @InjectMocks
    private CotrQueryService cotrQueryService;

    @Mock
    private COTRDetailsRepository cotrDetailsRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObject hearingJson;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private COTRDefendantRepository cotrDefendantRepository;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldNotFetchCotrDetailsForAProsecutionCaseByLatestHearingDate(){
        List<COTRDetailsEntity> cotrDetailsEntities = new ArrayList<>();
        when(cotrDetailsRepository.findByProsecutionCaseId(any())).thenReturn(cotrDetailsEntities);
        List<CotrDetail> cotrDetailList =  cotrQueryService.getCotrDetailsForAProsecutionCaseByLatestHearingDate(randomUUID());
        assertThat(cotrDetailList.size(), is(0));
    }

    @Test
    public void shouldGetCotrDetailsForAProsecutionCaseByLatestHearingDate(){
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case1sDefendant1Id = randomUUID();
        final UUID case1sDefendant2Id = randomUUID();
        final UUID case1sDefendant1sOffenceId = randomUUID();
        final UUID case1sDefendant2sOffenceId = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case2sDefendant1Id = randomUUID();
        final UUID case2sDefendant2Id = randomUUID();
        final UUID case2sDefendant1sOffenceId = randomUUID();
        final UUID case2sDefendant2sOffence1Id = randomUUID();
        final UUID case2sDefendant2sOffence2Id = randomUUID();

        List<COTRDetailsEntity> cotrDetailList = new ArrayList<>();
        COTRDetailsEntity c1 = new COTRDetailsEntity();
        c1.setId(randomUUID());
        c1.setHearingId(randomUUID());
        c1.setProsecutionFormData("formdata");
        cotrDetailList.add(c1);
        COTRDetailsEntity c2 = new COTRDetailsEntity();
        c2.setId(randomUUID());
        c2.setHearingId(randomUUID());
        c2.setProsecutionFormData("formdata");
        cotrDetailList.add(c2);
        when(cotrDetailsRepository.findByProsecutionCaseId(any())).thenReturn(cotrDetailList);

        final List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay()
                .withCourtCentreId(randomUUID())
                .build());

        final Hearing hearing = Hearing.hearing().withId((hearingId))
                .withHearingDays(hearingDays)
                .withProsecutionCases(new ArrayList<>(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(case1Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                                .withId(case1sDefendant1Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case1sDefendant1sOffenceId).build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case1sDefendant2Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case1sDefendant2sOffenceId).build())))
                                                .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                                .withId(case2sDefendant1Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case2sDefendant1sOffenceId).build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case2sDefendant2Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(
                                                        Offence.offence().withId(case2sDefendant2sOffence1Id).build(),
                                                        Offence.offence().withId(case2sDefendant2sOffence2Id).build())))
                                                .build())))
                                .build()
                ))).build();

        final String payload = objectToJsonObjectConverter.convert(hearing).toString();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(payload);
        when(hearingRepository.findBy(any())).thenReturn(hearingEntity);
        when(stringToJsonObjectConverter.convert(any())).thenReturn(hearingJson);
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(hearing);
        final List<COTRDefendantEntity> defendantEntities = new ArrayList<>();
        defendantEntities.add(new COTRDefendantEntity());
        when(cotrDefendantRepository.findByCotrId(any())).thenReturn(defendantEntities);

        List<CotrDetail> cotrDetailList1 =  cotrQueryService.getCotrDetailsForAProsecutionCaseByLatestHearingDate(randomUUID());
        assertThat(cotrDetailList1.size(), is(2));
    }
}
