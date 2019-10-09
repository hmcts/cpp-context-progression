package uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationSummaryTest {

    private static final String LEGAL_ENTITY_DEFENDANTS_NAME = "ABC LTD";
    private static final String RESPONDENT_NAME = "XYZ LTD";

    @Test
    public void applicantDisplayNameShouldMatchWithLegalEntityDefendant(){

        CourtApplicationParty courtApplicationParty = getCourtApplicationPartyWithLegalEntityDefendant(LEGAL_ENTITY_DEFENDANTS_NAME);

        CourtApplicationSummary courtApplicationSummary = CourtApplicationSummary.applicationSummary()
                .withApplicantDisplayName(courtApplicationParty)
                .build();

        assertEquals(LEGAL_ENTITY_DEFENDANTS_NAME, courtApplicationSummary.getApplicantDisplayName());
    }

    @Test
    public void respondentDisplayNamesShouldMatchWithLegalEntityDefendant(){

        CourtApplicationParty courtApplicationParty = getCourtApplicationPartyWithLegalEntityDefendant(RESPONDENT_NAME);
        CourtApplicationParty courtApplicationParty1 = getCourtApplicationPartyWithLegalEntityDefendant(LEGAL_ENTITY_DEFENDANTS_NAME);

        CourtApplicationRespondent courtApplicationRespondent = CourtApplicationRespondent
                .courtApplicationRespondent()
                .withPartyDetails(courtApplicationParty).build();

        CourtApplicationRespondent courtApplicationRespondent1 = CourtApplicationRespondent
                .courtApplicationRespondent()
                .withPartyDetails(courtApplicationParty1).build();

        List<CourtApplicationRespondent> courtApplicationRespondentList = new ArrayList<>();
        courtApplicationRespondentList.add(courtApplicationRespondent);
        courtApplicationRespondentList.add(courtApplicationRespondent1);

        CourtApplicationSummary courtApplicationSummary = CourtApplicationSummary.applicationSummary()
                .withApplicantDisplayName(courtApplicationParty)
                .withRespondentDisplayNames(courtApplicationRespondentList)
                .build();

        assertEquals(Arrays.asList(RESPONDENT_NAME, LEGAL_ENTITY_DEFENDANTS_NAME), courtApplicationSummary.getRespondentDisplayNames());
    }

    private CourtApplicationParty getCourtApplicationPartyWithLegalEntityDefendant(String name){
        return  CourtApplicationParty
                .courtApplicationParty()
                .withDefendant(Defendant.defendant()
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withName(name)
                                        .build()).build()).build()).build();
    }
}
