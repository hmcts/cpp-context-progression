package uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
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

        List<CourtApplicationParty> courtApplicationRespondentList = new ArrayList<>();
        courtApplicationRespondentList.add(courtApplicationParty);
        courtApplicationRespondentList.add(courtApplicationParty1);

        CourtApplicationSummary courtApplicationSummary = CourtApplicationSummary.applicationSummary()
                .withApplicantDisplayName(courtApplicationParty)
                .withRespondentDisplayNames(courtApplicationRespondentList)
                .build();

        assertEquals(Arrays.asList(RESPONDENT_NAME, LEGAL_ENTITY_DEFENDANTS_NAME), courtApplicationSummary.getRespondentDisplayNames());
    }

    private CourtApplicationParty getCourtApplicationPartyWithLegalEntityDefendant(String name){
        return  CourtApplicationParty
                .courtApplicationParty()
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withName(name)
                                        .build()).build()).build()).build();
    }
}
