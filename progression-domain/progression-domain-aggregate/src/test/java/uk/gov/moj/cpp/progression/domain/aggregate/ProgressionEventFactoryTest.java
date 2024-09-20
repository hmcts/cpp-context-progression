package uk.gov.moj.cpp.progression.domain.aggregate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
// FIXME!!! Temporarily using lenient strictness to get this
// context running with junit 5. This test really needs re-writing.
@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class ProgressionEventFactoryTest {

    private static final String CASE_ID = randomUUID();
    private static final String DEFENDANT_ID = randomUUID();
    private static final String COURT_CENTRE_NAME = "Warwick Justice Centre";
    private static final String COURT_CENTRE_ID = "1234";
    private static final String HEARING_TYPE = "PTP";
    private static final String SENDING_COMMITAL_DATE = "01-01-1990";
    private static final String CASE_URN = "87GD9945217";
    private static final String DEFENDANT_PERSON_ID = randomUUID();
    private static final String DEFENDANT_FIRST_NAME = "David";
    private static final String DEFENDANT_LAST_NAME = "Lloyd";
    private static final String DEFENDANT_NATIONALITY = "British";
    private static final String DEFENDANT_GENDER = "Male";
    private static final String DEFENDANT_ADDRESS_1 = "Lima Court";
    private static final String DEFENDANT_ADDRESS_2 = "Bath Road";
    private static final String DEFENDANT_ADDRESS_3 = "Norwich";
    private static final String DEFENDANT_ADDRESS_4 = "UK";
    private static final String DEFENDANT_POSTCODE = "NR11HF";
    private static final String DEFENDANT_DATE_OF_BIRTH = "23-10-1995";
    private static final String BAIL_STATUS = "bailed";
    private static final String CUSTODY_TIME_LIMIT_DATE = "2018-01-30";
    private static final String DEFENCE_ORGANISATION = "Solicitor Jacob M";
    private static final boolean INTERPRETER_NEEDED = false;
    private static final String INTERPRETER_LANGUAGE = "English";
    private static final String OFFENCE_ID = randomUUID();
    private static final String OFFENCE_CODE = "OF61131";
    private static final String PLEA_ID = randomUUID();
    private static final String PLEA_VALUE = "GUILTY";
    private static final String PLEA_DATE = "2017-11-02";
    private static final String SECTION = "S 51";
    private static final String WORDING = "On 10 Oct ...";
    private static final String REASON = "Not stated";
    private static final String DESCRIPTION = "Not available";
    private static final String CATEGORY = "Civil";
    private static final String START_DATE = "10-10-2017";
    private static final String END_DATE = "11-11-2017";
    private static final String CC_HEARING_DATE = "15-10-2017";
    private static final String CC_COURT_CENTRE_NAME = "Liverpool crown court";
    private static final String OFFENCE_TITLE = "O_TITLE";
    private static final String LEGISLATION = "O_LEGISLATION";
    private static final int ORDER_INDEX = 2;
    private static final String CC_COURT_CENTRE_ID = randomUUID();

    @Mock
    JsonEnvelope envelope;
    @Mock
    JsonObject jsonObj;

    @BeforeEach
    public void SetUp() {
        when(this.envelope.payloadAsJsonObject()).thenReturn(this.jsonObj);
        when(this.jsonObj.getString(Mockito.eq("caseId"))).thenReturn(CASE_ID);
        when(this.jsonObj.getString(Mockito.eq("sendingCommittalDate")))
                        .thenReturn(LocalDate.now().toString());

        when(this.jsonObj.getJsonObject("hearing")).thenReturn(Json.createObjectBuilder()
                .add("courtCentreName", COURT_CENTRE_NAME)
                .add("courtCentreId", COURT_CENTRE_ID).add("type", HEARING_TYPE)
                .add("sendingCommittalDate", SENDING_COMMITAL_DATE).add("caseId", CASE_ID)
                .add("caseUrn", CASE_URN)
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", DEFENDANT_ID)
                                .add("personId", DEFENDANT_PERSON_ID)
                                .add("firstName", DEFENDANT_FIRST_NAME).add("lastName", DEFENDANT_LAST_NAME)
                                .add("nationality", DEFENDANT_NATIONALITY).add("gender", DEFENDANT_GENDER)
                                        .add("address", Json.createObjectBuilder()
                                                .add("address1", DEFENDANT_ADDRESS_1)
                                                .add("address2", DEFENDANT_ADDRESS_2)
                                                .add("address3", DEFENDANT_ADDRESS_3)
                                                .add("address4", DEFENDANT_ADDRESS_4)
                                                .add("postcode", DEFENDANT_POSTCODE).build())
                                .add("dateOfBirth", DEFENDANT_DATE_OF_BIRTH)
                                .add("bailStatus", BAIL_STATUS)
                                .add("custodyTimeLimitDate", CUSTODY_TIME_LIMIT_DATE)
                                .add("defenceOrganisation", DEFENCE_ORGANISATION)
                                        .add("interpreter", Json.createObjectBuilder()
                                                .add("needed", INTERPRETER_NEEDED)
                                                .add("language", INTERPRETER_LANGUAGE).build())
                                        .add("offences", Json.createArrayBuilder().add(Json
                                                        .createObjectBuilder()
                                                .add("id", OFFENCE_ID)
                                                .add("offenceCode", OFFENCE_CODE)
                                                .add("plea", Json.createObjectBuilder().add("id", PLEA_ID)
                                                        .add("value", PLEA_VALUE)
                                                        .add("pleaDate", PLEA_DATE).build())
                                                .add("section", SECTION)
                                                .add("wording", WORDING)
                                                .add("reason", REASON)
                                                .add("description", DESCRIPTION)
                                                .add("category", CATEGORY)
                                                .add("startDate", START_DATE)
                                                .add("title", OFFENCE_TITLE)
                                                .add("legislation", LEGISLATION)
                                                .add("orderIndex", ORDER_INDEX)
                                                .add("endDate", END_DATE).build()))
                                        .build()).build())
                        .build());
        when(this.jsonObj.getJsonObject("crownCourtHearing"))
                .thenReturn(Json.createObjectBuilder().add("ccHearingDate", CC_HEARING_DATE)
                        .add("courtCentreName", CC_COURT_CENTRE_NAME).add("courtCentreId", CC_COURT_CENTRE_ID)
                        .build());
    }

    @Test
    public void testCreateCaseAddedToCrownCourt() {
        final Object obj = ProgressionEventFactory.createCaseAddedToCrownCourt(this.envelope);
        assertThat(obj, instanceOf(CaseAddedToCrownCourt.class));
    }


    @Test
    public void testCreateSendingCommittalHearingInformationAdded() {
        final Object obj = ProgressionEventFactory
                        .createSendingCommittalHearingInformationAdded(this.envelope);
        assertThat(obj, instanceOf(SendingCommittalHearingInformationAdded.class));
    }



    @Test
    public void testCreateCompletedSendingSheet() {
        final Object obj = ProgressionEventFactory.completedSendingSheet(this.envelope);
        assertThat(obj, instanceOf(SendingSheetCompleted.class));
        final SendingSheetCompleted ssCompleted = (SendingSheetCompleted) obj;
        assertSendingSheetCompletedValues(ssCompleted);
    }

    @Test
    public void testCreatePsrForDefendantsRequest() {
        when(this.jsonObj.getJsonArray(Mockito.eq("defendants")))
                .thenReturn(
                   Json.createArrayBuilder()
                        .add(createDefendantJsonWithIsPsrRequested(true))
                        .add(createDefendantJsonWithIsPsrRequested(false))
                        .build());

        final Object obj = ProgressionEventFactory.createPsrForDefendantsRequested(this.envelope);

        assertThat(obj, instanceOf(PreSentenceReportForDefendantsRequested.class));
        final PreSentenceReportForDefendantsRequested event = (PreSentenceReportForDefendantsRequested) obj;
        final List<DefendantPSR> defendants = event.getDefendants();
        assertThat(defendants.size(), is(2));
        assertThat(defendants.get(0).getPsrIsRequested(), is(true));
        assertThat(defendants.get(1).getPsrIsRequested(), is(false));
    }









    private static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    private JsonObject createDefendantJsonWithIsPsrRequested(final Boolean isPsrRequested) {
        return Json.createObjectBuilder()
                .add("defendantId", UUID.randomUUID().toString())
                .add("psrIsRequested", isPsrRequested).build();
    }

    private void assertSendingSheetCompletedValues(final SendingSheetCompleted ssCompleted) {
        assertThat(CC_HEARING_DATE, equalTo(ssCompleted.getCrownCourtHearing().getCcHearingDate()));
        assertThat(CC_COURT_CENTRE_ID, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreId().toString()));
        assertThat(CC_COURT_CENTRE_NAME, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreName()));
        assertSendingSheetCompletedHearingValues(ssCompleted.getHearing());
    }

    private void assertSendingSheetCompletedHearingValues(final Hearing hearing) {
        assertThat(COURT_CENTRE_NAME, equalTo(hearing.getCourtCentreName()));
        assertThat(COURT_CENTRE_ID, equalTo(hearing.getCourtCentreId()));
        assertThat(HEARING_TYPE, equalTo(hearing.getType()));
        assertThat(SENDING_COMMITAL_DATE, equalTo(hearing.getSendingCommittalDate()));
        assertThat(CASE_URN, equalTo(hearing.getCaseUrn()));
        assertThat(CASE_ID, equalTo(hearing.getCaseId().toString()));
        assertHearingDefendant(hearing.getDefendants().get(0));
    }

    private void assertHearingDefendant(final Defendant defendant) {
        assertThat(DEFENDANT_ID, equalTo(defendant.getId().toString()));
        assertThat(DEFENDANT_PERSON_ID, equalTo(defendant.getPersonId().toString()));
        assertThat(DEFENDANT_FIRST_NAME, equalTo(defendant.getFirstName()));
        assertThat(DEFENDANT_LAST_NAME, equalTo(defendant.getLastName()));
        assertThat(DEFENDANT_ADDRESS_1, equalTo(defendant.getAddress().getAddress1()));
        assertThat(DEFENDANT_ADDRESS_2, equalTo(defendant.getAddress().getAddress2()));
        assertThat(DEFENDANT_ADDRESS_3, equalTo(defendant.getAddress().getAddress3()));
        assertThat(DEFENDANT_ADDRESS_4, equalTo(defendant.getAddress().getAddress4()));
        assertThat(DEFENDANT_POSTCODE, equalTo(defendant.getAddress().getPostcode()));
        assertThat(DEFENDANT_DATE_OF_BIRTH, equalTo(defendant.getDateOfBirth()));
        assertThat(BAIL_STATUS, equalTo(defendant.getBailStatus()));
        assertThat(CUSTODY_TIME_LIMIT_DATE, equalTo(defendant.getCustodyTimeLimitDate()));
        assertThat(DEFENCE_ORGANISATION, equalTo(defendant.getDefenceOrganisation()));
        assertThat(INTERPRETER_NEEDED, equalTo(defendant.getInterpreter().getNeeded()));
        assertThat(INTERPRETER_LANGUAGE, equalTo(defendant.getInterpreter().getLanguage()));
        assertDefendantOffence(defendant.getOffences().get(0));
    }

    private void assertDefendantOffence(final Offence offence) {
        assertThat(OFFENCE_ID, equalTo(offence.getId().toString()));
        assertThat(OFFENCE_CODE, equalTo(offence.getOffenceCode()));
        assertThat(PLEA_ID, equalTo(offence.getPlea().getId().toString()));
        assertThat(PLEA_VALUE, equalTo(offence.getPlea().getValue()));
        assertThat(PLEA_DATE, equalTo(offence.getPlea().getPleaDate().toString()));
        assertThat(SECTION, equalTo(offence.getSection()));
        assertThat(WORDING, equalTo(offence.getWording()));
        assertThat(REASON, equalTo(offence.getReason()));
        assertThat(DESCRIPTION, equalTo(offence.getDescription()));
        assertThat(CATEGORY, equalTo(offence.getCategory()));
        assertThat(START_DATE, equalTo(offence.getStartDate()));
        assertThat(END_DATE, equalTo(offence.getEndDate()));
        assertThat(OFFENCE_TITLE, equalTo(offence.getTitle()));
        assertThat(LEGISLATION, equalTo(offence.getLegislation()));
        assertThat(ORDER_INDEX, equalTo(offence.getOrderIndex()));
    }

}
