package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Arrays;
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
public class ListingReferenceDataEnricherTest {

    @InjectMocks
    private ListingReferenceDataEnricher testObj;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    ReferenceDataService referenceDataService;

    final ArgumentCaptor<ListingCase> captor = ArgumentCaptor.forClass(ListingCase.class);

    @Before
    public void setUp() throws Exception {
        testObj.jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(testObj.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldEnrichReferenceData_Title_And_Legislation_By_CjsOffenceCode() throws Exception {
        //Given
        final String arbitraryCjsOffenceCode = "12345";
        final String arbitraryTitle = "abc";
        final String arbitraryLegislation = "xyz";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("cjsoffencecode", arbitraryCjsOffenceCode)
                .add("title", arbitraryTitle)
                .add("legislation", arbitraryLegislation)
                .build();

        final List<Hearing> hearings = Arrays.asList(new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PTP",
                new StringGenerator().next(),
                15,
                Arrays.asList(new Defendant(UUID.randomUUID(),
                        UUID.randomUUID(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        Arrays.asList(new Offence(UUID.randomUUID(),
                                arbitraryCjsOffenceCode,
                                new StringGenerator().next(),
                                new StringGenerator().next(),
                                null))
                ))
                , null,
                null,
                null
        ));
        final ListingCase listingCase = new ListingCase(UUID.randomUUID(), new StringGenerator().next(), hearings);

        when(referenceDataService.getOffenceByCjsCode(any(), any())).thenReturn(Optional.of(jsonObject));
        when(delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD)).thenReturn(listingCase);
        when(delegateExecution.getVariable(USER_ID)).thenReturn(UUID.randomUUID().toString());


        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final ListingCase result = captor.getValue();
        assertThat(result.getHearings().get(0).getDefendants().get(0).getOffences().get(0).getStatementOfOffence().getTitle().equals(arbitraryTitle), equalTo(true));
        assertThat(result.getHearings().get(0).getDefendants().get(0).getOffences().get(0).getStatementOfOffence().getLegislation().equals(arbitraryLegislation), equalTo(true));

    }

    @Test
    public void shouldNotEnrichReferenceData_Title_And_Legislation_By_CjsOffenceCode() throws Exception {
        //Given
        final List<Hearing> hearings = Arrays.asList(new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PTP",
                new StringGenerator().next(),
                15,
                Arrays.asList(new Defendant(UUID.randomUUID(),
                        UUID.randomUUID(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        Arrays.asList(new Offence(UUID.randomUUID(),
                                "12345",
                                new StringGenerator().next(),
                                new StringGenerator().next(), null))
                ))
                , null,
                null,
                null
        ));
        final ListingCase listingCase = new ListingCase(UUID.randomUUID(), new StringGenerator().next(), hearings);

        when(referenceDataService.getOffenceByCjsCode(any(), any())).thenReturn(Optional.empty());
        when(delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD)).thenReturn(listingCase);
        when(delegateExecution.getVariable(USER_ID)).thenReturn(UUID.randomUUID().toString());


        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final ListingCase result = captor.getValue();
        assertThat(listingCase.equals(result), equalTo(true));

    }
}
