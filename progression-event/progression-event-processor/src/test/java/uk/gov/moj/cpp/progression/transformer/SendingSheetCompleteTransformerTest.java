package uk.gov.moj.cpp.progression.transformer;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;
import static uk.gov.moj.cpp.progression.service.RefDataService.ID;
import static uk.gov.moj.cpp.progression.service.RefDataService.NATIONALITY_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.transformer.SendingSheetCompleteTransformer.PROSECUTION_AUTHORITY_CODE;
import static uk.gov.moj.cpp.progression.transformer.SendingSheetCompleteTransformer.PROSECUTION_AUTHORITY_ID;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Address;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.IndicatedPlea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SendingSheetCompleteTransformerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String CASE_URN = "URN";
    private static final LocalDate SENDING_COMMITAL_DATE = LocalDate.now();
    private static final String FIRST_NAME = "NAME1";
    private static final String LAST_NAME = "NAME2";
    private static final String NATIONALITY = "NAME2";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.now();
    private static final String INTERPRETER_LANGUAGE = "GERMAN";
    private static final String GENDER = "MALE";
    private static final String ADDRESS_1 = "3, Brunswick Road";
    private static final String POST_CODE = "NR2 6HF";
    private static final LocalDate CUSTODY_TIME_LIMIT_DATE = LocalDate.now();
    private static final String DEFENCE_ORGNISATION = "ABC Solicitors";
    private static final String OFFENCE_CODE = "BRG";
    private static final String OFFENCE_SECTION = "SECT";
    private static final String OFFENCE_WORDING = "WRDG";
    private static final LocalDate OFFENCE_START_DATE = LocalDate.now();
    private static final LocalDate OFFENCE_END_DATE = LocalDate.now();
    private static final LocalDate CONVICTION_DATE = LocalDate.now();
    private static final String PLEA_GUILTY = "GUILTY";
    private static final LocalDate PLEA_DATE = LocalDate.now();
    private static final String INDICATED_PLEA = "INDICATED_GUILTY";
    private static final String ALLOCATION_DECISION = "ELECT_TRIAL";
    private static final String OFFENCE_TITLE = "offence title";
    private static final String LEGISLATION = "O_LEGISLATION";
    private static final int ORDER_INDEX = 2;
    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private Requester requester;
    @InjectMocks
    private SendingSheetCompleteTransformer sendingSheetCompleteTransformer;

    private static JsonObject getOffence(final String modeoftrial) {
        return Json.createObjectBuilder().add("legislation", LEGISLATION)
                .add("welshlegislation", LEGISLATION_WELSH)
                .add("title", OFFENCE_TITLE)
                .add("welshoffencetitle", WELSH_OFFENCE_TITLE)
                .add("modeOfTrial", modeoftrial)
                .add("offenceId", randomUUID().toString())
                .add(CJS_OFFENCE_CODE, OFFENCE_CODE).build();

    }

    private static JsonObject getNationalityObject() {
        return Json.createObjectBuilder()
                .add(NATIONALITY_CODE, "N12")
                .add(NATIONALITY, "UK")
                .add(ID, randomUUID().toString())
                .build();
    }

    @Test
    public void testSendingSheetTransformer() {

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataOffenceService.getOffenceByCjsCode(OFFENCE_CODE, jsonEnvelope, requester))
                .thenReturn(of(getOffence("Indictable")));
        when(referenceDataService.getNationalityByNationality(jsonEnvelope, NATIONALITY, requester))
                .thenReturn(of(getNationalityObject()));

        final ProsecutionCase pCase = sendingSheetCompleteTransformer.transformToProsecutionCase(createSendingSheetCompleted(), jsonEnvelope);
        assertProsecutionCaseValues(pCase);
    }

    private SendingSheetCompleted createSendingSheetCompleted() {
        final SendingSheetCompleted ssc = new SendingSheetCompleted();
        ssc.setHearing(createHearing());
        return ssc;
    }

    private Hearing createHearing() {
        final Hearing hearing = new Hearing();
        hearing.setCaseId(CASE_ID);
        hearing.setCaseUrn(CASE_URN);
        hearing.setSendingCommittalDate(SENDING_COMMITAL_DATE.toString());
        hearing.setDefendants(createDefendants());
        return hearing;
    }

    private List<Defendant> createDefendants() {
        final List<Defendant> defendants = new ArrayList<>();
        final Defendant defendant = new Defendant();
        defendant.setId(DEFENDANT_ID);
        defendant.setFirstName(FIRST_NAME);
        defendant.setLastName(LAST_NAME);
        defendant.setNationality(NATIONALITY);
        defendant.setGender(GENDER);
        defendant.setDateOfBirth(DATE_OF_BIRTH.toString());
        final Interpreter interpreter = new Interpreter();
        interpreter.setLanguage(INTERPRETER_LANGUAGE);
        interpreter.setNeeded(true);
        defendant.setInterpreter(interpreter);
        final Address address = new Address();
        address.setAddress1(ADDRESS_1);
        address.setPostcode(POST_CODE);
        defendant.setAddress(address);
        defendant.setCustodyTimeLimitDate(CUSTODY_TIME_LIMIT_DATE.toString());
        defendant.setDefenceOrganisation(DEFENCE_ORGNISATION);
        defendant.setOffences(createOffences());
        defendants.add(defendant);
        return defendants;
    }

    private List<Offence> createOffences() {
        final List<Offence> offences = new ArrayList<>();
        final Offence offence = new Offence();
        offence.setId(OFFENCE_ID);
        offence.setOffenceCode(OFFENCE_CODE);
        offence.setSection(OFFENCE_SECTION);
        offence.setWording(OFFENCE_WORDING);
        offence.setStartDate(OFFENCE_START_DATE.toString());
        offence.setEndDate(OFFENCE_END_DATE.toString());
        offence.setConvictionDate(CONVICTION_DATE);
        offence.setPlea(new Plea(randomUUID(), PLEA_GUILTY, PLEA_DATE));
        offence.setIndicatedPlea(new IndicatedPlea(randomUUID(), INDICATED_PLEA, ALLOCATION_DECISION));
        offence.setTitle(OFFENCE_TITLE);
        offence.setLegislation(LEGISLATION);
        offence.setOrderIndex(ORDER_INDEX);
        offences.add(offence);
        return offences;
    }

    private void assertProsecutionCaseValues(final ProsecutionCase pCase) {
        assertThat(pCase.getId(), equalTo(CASE_ID));
        assertThat(pCase.getInitiationCode().toString(), equalTo("C"));
        assertThat(pCase.getProsecutionCaseIdentifier().getCaseURN(), equalTo(CASE_URN));
        assertThat(pCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), equalTo(PROSECUTION_AUTHORITY_CODE));
        assertThat(pCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId(), equalTo(PROSECUTION_AUTHORITY_ID));
        assertDefendant(pCase.getDefendants().get(0));

    }

    private void assertDefendant(final uk.gov.justice.core.courts.Defendant defendant) {
        assertThat(defendant.getId(), equalTo(DEFENDANT_ID));
        assertThat(defendant.getPersonDefendant().getCustodyTimeLimit(), equalTo(CUSTODY_TIME_LIMIT_DATE));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getAddress().getAddress1(), equalTo(ADDRESS_1));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getAddress().getPostcode(), equalTo(POST_CODE));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), equalTo(DATE_OF_BIRTH));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getFirstName(), equalTo(FIRST_NAME));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getLastName(), equalTo(LAST_NAME));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getGender().toString(), equalTo(GENDER));
        assertThat(defendant.getPersonDefendant().getPersonDetails().getInterpreterLanguageNeeds(), equalTo(INTERPRETER_LANGUAGE));
        assertOffence(defendant.getOffences().get(0));
    }

    private void assertOffence(final uk.gov.justice.core.courts.Offence offence) {
        assertThat(offence.getId(), equalTo(OFFENCE_ID));
        assertThat(offence.getConvictionDate(), equalTo(CONVICTION_DATE));
        assertThat(offence.getEndDate(), equalTo(OFFENCE_END_DATE));
        assertThat(offence.getStartDate(), equalTo(OFFENCE_START_DATE));
        assertThat(offence.getWording(), equalTo(OFFENCE_WORDING));
        assertThat(offence.getOffenceCode(), equalTo(OFFENCE_CODE));
        assertThat(offence.getOffenceTitle(), equalTo(OFFENCE_TITLE));
        assertThat(offence.getOffenceLegislation(), equalTo(LEGISLATION));
        assertThat(offence.getOrderIndex(), equalTo(ORDER_INDEX));
        assertThat(offence.getPlea().getPleaDate(), equalTo(PLEA_DATE));
        assertThat(offence.getIndicatedPlea().getIndicatedPleaDate(), equalTo(CONVICTION_DATE));
        assertThat(offence.getIndicatedPlea().getIndicatedPleaValue().toString(), equalTo(INDICATED_PLEA));
        assertThat(offence.getAllocationDecision().getMotReasonCode(), equalTo("4"));
        assertThat(offence.getAllocationDecision().getMotReasonDescription(), equalTo("Defendant chooses trial by jury"));
        assertThat(offence.getAllocationDecision().getOffenceId(), equalTo(OFFENCE_ID));
    }


}
