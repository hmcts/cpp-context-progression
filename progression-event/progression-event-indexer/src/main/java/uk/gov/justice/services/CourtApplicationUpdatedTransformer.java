package uk.gov.justice.services;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CourtApplicationUpdatedTransformer implements Transform {

    public static final String PROSECUTION = "PROSECUTION";
    public static final String APPLICATION = "APPLICATION";
    public static final String ACTIVE = "ACTIVE";
    public static final String APPLICANT = "APPLICANT";
    public static final String RESPONDENT = "RESPONDENT";

    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final CourtApplicationUpdated courtApplicationUpdated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CourtApplicationUpdated.class);

        final CourtApplication courtApplication = courtApplicationUpdated.getCourtApplication();

        final CaseDetails caseDetails = new CaseDetails();
        final List<Party> parties = new ArrayList<>();

        //Applicant Organisation
        applicants(courtApplication, parties);

        respondents(courtApplication, parties);

        caseDetails.setCaseId(courtApplication.getId().toString());
        if (courtApplication.getLinkedCaseId() != null) {
            caseDetails.setCaseId(courtApplication.getLinkedCaseId().toString());
            caseDetails.set_case_type(PROSECUTION);
        } else {
            caseDetails.set_case_type(APPLICATION);
        }
        caseDetails.setCaseStatus(ACTIVE);
        caseDetails.set_is_crown(false);

        final List<Application> applications = new ArrayList<>();
        applications.add(application(courtApplicationUpdated));
        caseDetails.setApplications(applications);
        caseDetails.setParties(parties);
        return caseDetails;
    }

    private CourtApplicationParty applicants(final CourtApplication courtApplication, final List<Party> parties) {
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        if (applicant != null) {
            final Organisation organisation = applicant.getOrganisation();
            final List<AssociatedPerson> organisationPersons = applicant.getOrganisationPersons();
            if (organisation != null && organisation.getName() != null && organisationPersons != null) {
                final Party applicantOrganisation = domainToIndexMapper.organisation(organisation);
                applicantOrganisation.set_party_type(APPLICANT);
                applicantOrganisation.setOrganisationName(organisation.getName());
                applicantOrganisation.setPartyId(applicant.getId().toString());
                parties.add(applicantOrganisation);
            } else {
                //Applicant Person
                final Party applicantParty = domainToIndexMapper.person(applicant.getPersonDetails());
                if (organisation != null) {
                    applicantParty.setOrganisationName(organisation.getName());
                }
                applicantParty.set_party_type(APPLICANT);
                applicantParty.setPartyId(applicant.getId().toString());
                parties.add(applicantParty);
            }
            //Applicant DEFENDANT
            final Defendant applicantDefendant = courtApplication.getApplicant().getDefendant();
            if(applicantDefendant != null) {
                parties.add(domainToIndexMapper.party(applicantDefendant));
            }
        }
        return applicant;
    }

    private void respondents(final CourtApplication courtApplication, final List<Party> parties) {
        final List<CourtApplicationRespondent> respondents = courtApplication.getRespondents();
        for (final CourtApplicationRespondent respondent : respondents) {
            final Organisation organisation = respondent.getPartyDetails().getOrganisation();
            final CourtApplicationParty respondentPartyDetails = respondent.getPartyDetails();
            if (organisation != null && organisation.getName() != null && respondentPartyDetails.getOrganisationPersons() != null) {
                final Party respondentOrganisationPerson = domainToIndexMapper.organisation(organisation);
                respondentOrganisationPerson.setOrganisationName(organisation.getName());
                respondentOrganisationPerson.set_party_type(RESPONDENT);
                respondentOrganisationPerson.setPartyId(respondentPartyDetails.getId().toString());
                parties.add(respondentOrganisationPerson);
            } else {
                //Respondent Person
                final Person person = respondentPartyDetails.getPersonDetails();
                final Party respondentPerson = domainToIndexMapper.person(person);
                if (organisation != null) {
                    respondentPerson.setOrganisationName(organisation.getName());
                }
                respondentPerson.set_party_type(RESPONDENT);
                respondentPerson.setPartyId(respondentPartyDetails.getId().toString());
                parties.add(respondentPerson);
            }
            //Respondent DEFENDANT
            if(respondent.getPartyDetails().getDefendant() != null) {
                parties.add(domainToIndexMapper.party(respondent.getPartyDetails().getDefendant()));
            }
        }
    }

    private Application application(CourtApplicationUpdated courtApplicationCreated) {

        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();

        final Application application = new Application();
        application.setApplicationReference(courtApplication.getApplicationReference());

        final CourtApplicationType type = courtApplication.getType();
        if (null != type) {
            application.setApplicationType(type.getApplicationType());
        }

        final UUID applicationId = courtApplication.getId();
        if (null != applicationId) {
            application.setApplicationId(applicationId.toString());
        }

        final LocalDate applicationReceivedDate = courtApplication.getApplicationReceivedDate();
        if (null != applicationReceivedDate) {
            application.setReceivedDate(applicationReceivedDate.toString());
        }

        final LocalDate applicationDecisionSoughtByDate = courtApplication.getApplicationDecisionSoughtByDate();
        if (null != applicationDecisionSoughtByDate) {
            application.setDecisionDate(applicationDecisionSoughtByDate.toString());
        }

        final LocalDate dueDate = courtApplication.getDueDate();
        if (null != dueDate) {
            application.setDueDate(dueDate.toString());
        }

        return application;
    }
}