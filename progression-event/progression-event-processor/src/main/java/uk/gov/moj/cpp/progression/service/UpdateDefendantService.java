package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDefendantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantService.class);
    public static final String MASTER_DEFENDANT = "masterDefendant";
    public static final String APPLICANT = "applicant";
    public static final String SUBJECT = "subject";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_FOR_CASE = "progression.command.update-defendant-for-prosecution-case";
    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_FOR_PROSECUTION_CASE_WITH_CUSTODIAL_ESTABLISHMENT = "progression.command.update-defendant-for-prosecution-case-with-custodial-establishment";

    public void updateDefendantCustodialEstablishment(final Metadata eventMetadata, final Defendant defendantWithPrisonSentence, final CustodialEstablishment custodialEstablishment) {
        final DefendantUpdate defendantUpdate = toDefendantUpdate(defendantWithPrisonSentence, custodialEstablishment);
        final JsonObject defendantUpdateJson = objectToJsonObjectConverter.convert(defendantUpdate);

        final JsonObject jsonPayload = createObjectBuilder()
                .add(PROSECUTION_CASE_ID, defendantUpdate.getProsecutionCaseId().toString())
                .add("id", defendantUpdate.getId().toString())
                .add("defendant", defendantUpdateJson).build();

        LOGGER.info("Updating defendant custody establishment with payload={}", jsonPayload);

        sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(eventMetadata)
                .withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_FOR_CASE), jsonPayload));
    }

    public void updateDefendantCustodialEstablishment(final Metadata eventMetadata, final JsonObject application, final CustodialEstablishment custodialEstablishment) {
        final JsonObject custodialEstablishmentJson = objectToJsonObjectConverter.convert(custodialEstablishment);

        JsonObject masterDefendant = application.getJsonObject(APPLICANT).containsKey(MASTER_DEFENDANT) ?
                application.getJsonObject(APPLICANT) : application.getJsonObject(SUBJECT);
        String prosecutionCaseId;
        if (application.containsKey("courtApplicationCases")) {
            prosecutionCaseId = application.getJsonArray("courtApplicationCases").get(0).asJsonObject().getString(PROSECUTION_CASE_ID);
        } else {
            prosecutionCaseId = application.getJsonObject("courtOrder").getJsonArray("courtOrderOffences").get(0).asJsonObject().getString(PROSECUTION_CASE_ID);
        }

        final JsonObject jsonPayload = createObjectBuilder()
                .add(PROSECUTION_CASE_ID, prosecutionCaseId)
                .add("masterDefendantId", masterDefendant.getJsonObject(MASTER_DEFENDANT).getString("masterDefendantId"))
                .add("defendantId", masterDefendant.getJsonObject(MASTER_DEFENDANT).getJsonArray("defendantCase").get(0).asJsonObject().getString("defendantId"))
                .add("custodialEstablishment", custodialEstablishmentJson).build();

        LOGGER.info("Updating defendant custody establishment with payload={}", jsonPayload);

        sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(eventMetadata)
                .withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_FOR_PROSECUTION_CASE_WITH_CUSTODIAL_ESTABLISHMENT), jsonPayload));
    }

    private DefendantUpdate toDefendantUpdate(final Defendant defendant, final CustodialEstablishment custodialEstablishment) {


        return DefendantUpdate.defendantUpdate()
                .withAliases(defendant.getAliases())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withCroNumber(defendant.getCroNumber())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withId(defendant.getId())
                .withIsYouth(defendant.getIsYouth())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withOffences(defendant.getOffences())
                .withPersonDefendant(buildPersonDefendant(defendant.getPersonDefendant(), custodialEstablishment))
                .withPncId(defendant.getPncId())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .build();
    }

    private PersonDefendant buildPersonDefendant(PersonDefendant personDefendant, CustodialEstablishment custodialEstablishment) {
        return personDefendant().withValuesFrom(personDefendant)
                .withCustodialEstablishment(custodialEstablishment)
                .build();
    }

}
