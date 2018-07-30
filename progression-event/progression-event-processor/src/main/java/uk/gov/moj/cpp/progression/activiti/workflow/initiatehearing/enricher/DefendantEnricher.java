package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.enricher;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.INITIATE_HEARING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.hearing.Address;
import uk.gov.moj.cpp.external.domain.hearing.Defendant;
import uk.gov.moj.cpp.external.domain.hearing.DefendantCases;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Interpreter;
import uk.gov.moj.cpp.external.domain.hearing.Offence;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class DefendantEnricher implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantEnricher.class.getName());

    @Inject
    ProgressionService progressionService;

    @Inject
    private ReferenceDataService referenceDataService;


    @Handles("defendant.enricher-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework
    }

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception {
        final InitiateHearing initiateHearing =
                        (InitiateHearing) delegateExecution.getVariable(INITIATE_HEARING_PAYLOAD);
        final UUID caseId = (UUID) delegateExecution.getVariable(CASE_ID);
        final String userId = delegateExecution.getVariable(USER_ID).toString();
        initiateHearing.getHearing().getDefendants().forEach(d -> {
            final Optional<JsonObject> defendantJson = progressionService.getDefendantByDefendantId(
                            userId, caseId.toString(),
                            d.getId().toString());
            LOGGER.info("DefendantEnricher for defendantId {} returns {} " , d.getId(), defendantJson);
            if (defendantJson.isPresent()) {
                populateDefendant(d, defendantJson.get(), caseId, userId);
            }

        });

        delegateExecution.setVariable(INITIATE_HEARING_PAYLOAD, initiateHearing);
    }

    private void populateDefendant(final Defendant d, final JsonObject defendantJson,
                                   final UUID caseId, final String user_id) {
        final JsonObject personJson = defendantJson.getJsonObject("person");
        if (personJson != null) {
            d.setPersonId(UUID.fromString(personJson.getString("id")));
            d.setFirstName(personJson.getString("firstName", null));
            d.setLastName(personJson.getString("lastName", null));
            d.setNationality(personJson.getString("nationality", null));
            d.setGender(personJson.getString("gender", null));
            d.setAddress(getAddress(personJson.getJsonObject("address")));
            d.setDateOfBirth(personJson.getString("dateOfBirth", null));
        }
        d.setDefenceOrganisation(defendantJson.getString("defenceSolicitorFirm", null));
        d.setInterpreter(getInterPreter(defendantJson.getJsonObject("interpreter")));
        populateDefendantCases(d, defendantJson, caseId);
        populateOffences(d.getOffences(), defendantJson.getJsonArray("offences"), caseId, user_id);

    }


    private Address getAddress(final JsonObject addressJson) {
        final Address address = new Address();
        if (addressJson != null) {
            address.setAddress1(addressJson.getString("address1", null));
            address.setAddress2(addressJson.getString("address2", null));
            address.setAddress3(addressJson.getString("address3", null));
            address.setAddress4(addressJson.getString("address4", null));
            address.setPostCode(addressJson.getString("postCode", null));
        }
        return address;
    }

    private Interpreter getInterPreter(final JsonObject interpreterJson) {
        final Interpreter interpreter = new Interpreter();
        if (interpreterJson != null) {
            interpreter.setLanguage(interpreterJson.getString("language", null));
            interpreter.setNeeded(interpreterJson.getBoolean("needed", false));
        }
        return interpreter;
    }

    public void populateDefendantCases(final Defendant d, final JsonObject defendantJson,
                    final UUID caseId) {
        final DefendantCases defendantCases = new DefendantCases();
        defendantCases.setCaseId(caseId);
        defendantCases.setBailStatus(defendantJson.getString("bailStatus", null));
        defendantCases.setCustodyTimeLimitDate(
                        defendantJson.getString("custodyTimeLimitDate", null));
        d.getDefendantCases().add(defendantCases);
    }

    private void populateOffences(final List<Offence> offences, final JsonArray jsonArray,
                                  final UUID caseId, final String user_id) {
        offences.forEach(offence -> {
            if (jsonArray != null) {
                IntStream.rangeClosed(0, jsonArray.size() - 1).forEach(i -> {
                    final JsonObject offenceJson = jsonArray.getJsonObject(i);
                    if (offenceJson.getString("id").equals(offence.getId().toString())) {
                        populateOffence(offence, offenceJson, caseId, user_id);
                    }
                });
            }
        });

    }

    private void populateOffence(final Offence offence, final JsonObject offenceJson,
                                 final UUID caseId, final String user_id) {
        offence.setCaseId(caseId);
        offence.setOffenceCode(offenceJson.getString("offenceCode", null));
        offence.setWording(offenceJson.getString("wording", null));
        offence.setSection(offenceJson.getString("section", null));
        offence.setStartDate(offenceJson.getString("startDate", null));
        offence.setEndDate(offenceJson.getString("endDate", null));
        offence.setOrderIndex(offenceJson.getInt("offenceSequenceNumber", 0));
        offence.setCount(offenceJson.getInt("count", 0));
        offence.setConvictionDate(offenceJson.getString("convictionDate", null));
        populateOffenceReferenceData(offence, offenceJson.getString("offenceCode", null),  user_id);
    }

    private void populateOffenceReferenceData(final Offence offence, final String offenceCode, final String user_id) {
        if(offenceCode != null){
            final Optional<JsonObject> offenceCodeJson = referenceDataService.getOffenceByCjsCode(envelopeFor(user_id), offenceCode);
            if(offenceCodeJson.isPresent()){
                offence.setTitle(offenceCodeJson.get().getString("title",null));
                offence.setLegislation((offenceCodeJson.get().getString("legislation", null)));
            }
        }

    }

    private JsonEnvelope envelopeFor(final String userId) {
        return envelopeFrom(
                metadataWithRandomUUID("to-be-replaced")
                        .withUserId(userId)
                        .build(),
                null);
    }
}