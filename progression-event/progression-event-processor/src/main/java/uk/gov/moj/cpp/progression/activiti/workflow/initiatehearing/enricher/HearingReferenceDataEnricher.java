package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.enricher;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.INITIATE_HEARING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.hearing.Hearing;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Judge;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class HearingReferenceDataEnricher implements JavaDelegate {

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ReferenceDataService referenceDataService;


    @Handles("HearingReferenceData.enricher.dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework enricher
    }

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception {

        final InitiateHearing initiateHearing =
                        (InitiateHearing) delegateExecution.getVariable(INITIATE_HEARING_PAYLOAD);

        Optional<JsonObject> courtCentreJson = referenceDataService.getCourtCentreById(initiateHearing.getHearing().getCourtCentreId(), envelopeFor(delegateExecution.getVariable(USER_ID).toString()));
        if(courtCentreJson.isPresent()){
            populateCourtCentreDeatil(initiateHearing.getHearing(),courtCentreJson.get());
        }

        Optional<JsonObject>  judgeJson = referenceDataService.getJudgeById(initiateHearing.getHearing().getJudge().getId(), envelopeFor(delegateExecution.getVariable(USER_ID).toString()));
        if(judgeJson.isPresent()){
            populateJudge(initiateHearing.getHearing().getJudge(), judgeJson.get());
        }


        delegateExecution.setVariable(INITIATE_HEARING_PAYLOAD, initiateHearing);

    }

    private void populateJudge(final Judge judge, final JsonObject judgeJson) {
        judge.setFirstName(judgeJson.getString("firstName"));
        judge.setLastName(judgeJson.getString("lastName"));
        judge.setTitle(judgeJson.getString("title"));
    }

    private void populateCourtCentreDeatil(final Hearing hearing, final JsonObject courtCentreJson) {
        hearing.setCourtCentreName(courtCentreJson.getString("name"));
        hearing.setCourtRoomName(getCourtRoomName(hearing.getCourtRoomId(), courtCentreJson));
    }

    private String getCourtRoomName(final UUID courtRoomId, final JsonObject courtCentreJson) {
        return courtCentreJson.getJsonArray("courtRooms").getValuesAs(JsonObject.class).stream()
                    .filter(cr -> courtRoomId.toString().equals(cr.getString("id")))
                    .map(cr -> cr.getString("name")).findFirst().get();
    }

    private JsonEnvelope envelopeFor(final String userId) {
        return envelopeFrom(
                metadataWithRandomUUID("to-be-replaced")
                        .withUserId(userId)
                        .build(),
                null);
    }
}
