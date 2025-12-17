package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class PartiesMapper {

    public static final String APPLICANT = "APPLICANT";
    public static final String RESPONDENT = "RESPONDENT";

    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    public List<Party> transform(final CourtApplication courtApplication) {
        final List<Party> parties = new ArrayList<>();
        applicants(courtApplication, parties);
        respondents(courtApplication, parties);

        return parties;
    }

    private void applicants(final CourtApplication courtApplication, final List<Party> parties) {
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        if (applicant != null) {
            Party applicantParty = null;
            if (applicant.getMasterDefendant() != null) {
                //Applicant Master Defendant
                final MasterDefendant masterDefendant = applicant.getMasterDefendant();
                applicantParty = domainToIndexMapper.party(masterDefendant);
            } else if (applicant.getProsecutingAuthority() != null) {
                //Applicant Prosecuting Authority
                final ProsecutingAuthority prosecutingAuthority = applicant.getProsecutingAuthority();
                applicantParty = domainToIndexMapper.prosecutingAuthority(prosecutingAuthority);
            } else if (applicant.getPersonDetails() != null) {
                //Applicant Person
                final Person person = applicant.getPersonDetails();
                applicantParty = domainToIndexMapper.person(person);
            } else if (applicant.getOrganisation() != null) {
                //Applicant Organisation
                final Organisation organisation = applicant.getOrganisation();
                applicantParty = domainToIndexMapper.organisation(organisation);
            }

            if (applicantParty != null) {
                applicantParty.set_party_type(APPLICANT);
                applicantParty.setPartyId(applicant.getId().toString());
                parties.add(applicantParty);
            }
        }
    }

    private void respondents(final CourtApplication courtApplication, final List<Party> parties) {
        final List<CourtApplicationParty> respondents = courtApplication.getRespondents();

        if (CollectionUtils.isEmpty(respondents)) {
            return;
        }

        for (final CourtApplicationParty respondent : respondents) {
            Party respondentParty = null;
            if (respondent.getMasterDefendant() != null) {
                //Respondent Master Defendant
                final MasterDefendant masterDefendant = respondent.getMasterDefendant();
                respondentParty = domainToIndexMapper.party(masterDefendant);
            } else if (respondent.getProsecutingAuthority() != null) {
                //Respondent Prosecuting Authority
                final ProsecutingAuthority prosecutingAuthority = respondent.getProsecutingAuthority();
                respondentParty = domainToIndexMapper.prosecutingAuthority(prosecutingAuthority);
            } else if (respondent.getPersonDetails() != null) {
                //Respondent Person
                final Person person = respondent.getPersonDetails();
                respondentParty = domainToIndexMapper.person(person);
            } else if (respondent.getOrganisation() != null) {
                //Respondent Organisation
                final Organisation organisation = respondent.getOrganisation();
                respondentParty = domainToIndexMapper.organisation(organisation);
            }

            if (respondentParty != null) {
                respondentParty.set_party_type(RESPONDENT);
                respondentParty.setPartyId(respondent.getId().toString());
                parties.add(respondentParty);
            }
        }
    }
}
