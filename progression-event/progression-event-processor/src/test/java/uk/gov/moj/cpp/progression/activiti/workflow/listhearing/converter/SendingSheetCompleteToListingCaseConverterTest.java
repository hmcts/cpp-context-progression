package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.CrownCourtHearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class SendingSheetCompleteToListingCaseConverterTest {

    private final UUID defendantId = UUID.randomUUID();
    private final UUID personId = UUID.randomUUID();
    private final String firstName = "first Name";
    private final String lastName = "last Name";
    private final String dateOfBirth = "01-01-2001";
    private final String bailStatus = "bail";
    private final String custodyTimeLimit = "";
    private final String defenceOrganisation = "abc";

    private final UUID courtCentreId = UUID.randomUUID();
    private final String type = null;
    private final String startDate = "01-01-2017";
    private final Integer estimateMinutes = null;

    private final UUID caseProgressionId = UUID.randomUUID();
    private final String urn = "ABC";

    private final UUID offenceId = UUID.randomUUID();
    private final String offenceCode = "code";
    private final String offenceStartDate = "01-01-2017";
    private final String endDate = "01-12-2017";

    private final String title = "title";
    private final String legislation = "legislation";

    @InjectMocks
    private SendingSheetCompleteToListingCaseConverter sendingSheetCompleteToListingCaseConverter;

    @Mock
    private DelegateExecution execution;


    @Test
    public void convert() throws Exception {

        //Given
        final SendingSheetCompleted sendingSheetCompleted = buildSendingSheetComplete();
        when(execution.getVariable("sendingSheetComplete")).thenReturn(sendingSheetCompleted);
        //When
        final ListingCase listingCase = sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted);

        assertThat(listingCase.getCaseId(), is(caseProgressionId));
        assertThat(listingCase.getUrn(), is(urn));
        assertThat(listingCase.getHearings().get(0).getStartDate(), is(startDate));
        assertThat(listingCase.getHearings().get(0).getCourtCentreId(), is(courtCentreId));
        assertThat(listingCase.getHearings().get(0).getEstimateMinutes(), is(estimateMinutes));
        assertNotNull(listingCase.getHearings().get(0).getId());
        assertThat(listingCase.getHearings().get(0).getType(), is(type));

        final Defendant defendant = listingCase.getHearings().get(0).getDefendants().get(0);
        assertThat(defendant.getId(), is(defendantId));
        assertThat(defendant.getBailStatus(), is(bailStatus));
        assertThat(defendant.getCustodyTimeLimit(), is(custodyTimeLimit));
        assertThat(defendant.getDateOfBirth(), is(dateOfBirth));
        assertThat(defendant.getDefenceOrganisation(), is(defenceOrganisation));
        assertThat(defendant.getFirstName(), is(firstName));
        assertThat(defendant.getLastName(), is(lastName));
        assertThat(defendant.getPersonId(), is(personId));

        final Offence offence = defendant.getOffences().get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getEndDate(), is(endDate));
        assertThat(offence.getOffenceCode(), is(offenceCode));
        assertThat(offence.getStartDate(), is(offenceStartDate));
        assertThat(offence.getStatementOfOffence() == null, is(true));
    }

    private SendingSheetCompleted buildSendingSheetComplete() {
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();

        final Hearing hearing = new Hearing();
        hearing.setCourtCentreId(courtCentreId.toString());
        hearing.setCaseId(caseProgressionId);
        hearing.setType(type);
        hearing.setCaseUrn(urn);
        final List<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant> defendants = new ArrayList<>();
        defendants.add(createDefendantObj());
        hearing.setDefendants(defendants);
        sendingSheetCompleted.setHearing(hearing);

        sendingSheetCompleted.setCrownCourtHearing(createCrownCourtHearingObj());
        return sendingSheetCompleted;
    }

    private uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant createDefendantObj() {
        final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant defendant =
                new uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant();

        defendant.setId(defendantId);
        defendant.setPersonId(personId);
        defendant.setFirstName(firstName);
        defendant.setLastName(lastName);
        defendant.setDateOfBirth(dateOfBirth);
        defendant.setBailStatus(bailStatus);
        defendant.setCustodyTimeLimitDate(custodyTimeLimit);
        defendant.setDefenceOrganisation(defenceOrganisation);
        final List<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence> offences = new ArrayList<>();
        offences.add(createOffenceObj());
        defendant.setOffences(offences);
        return defendant;
    }


    private uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence createOffenceObj() {
        final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence = new uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence();
        offence.setStartDate(offenceStartDate);
        offence.setEndDate(endDate);
        offence.setId(offenceId);
        offence.setOffenceCode(offenceCode);
        return offence;
    }

    private CrownCourtHearing createCrownCourtHearingObj() {
        final CrownCourtHearing crownCourtHearing = new CrownCourtHearing();
        crownCourtHearing.setCcHearingDate(startDate);
        crownCourtHearing.setCourtCentreId(courtCentreId);
        return crownCourtHearing;
    }

}
