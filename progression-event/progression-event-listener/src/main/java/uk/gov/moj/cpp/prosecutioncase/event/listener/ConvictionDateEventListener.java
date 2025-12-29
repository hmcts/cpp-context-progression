package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;


@SuppressWarnings({"squid:S3655", "squid:S1602"})
@ServiceComponent(EVENT_LISTENER)
public class ConvictionDateEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Handles("progression.event.conviction-date-added")
    public void addConvictionDate(final JsonEnvelope event) {
        final ConvictionDateAdded convictionDateAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), ConvictionDateAdded.class);

        if(convictionDateAdded.getCourtApplicationId() == null) {
            updateConvictionDateToOffenceUnderProsecutionCase(convictionDateAdded.getCaseId(), convictionDateAdded.getOffenceId(), convictionDateAdded.getConvictionDate());
        }else{
            updateConvictionDateToApplication(convictionDateAdded.getCourtApplicationId(), convictionDateAdded.getOffenceId(), convictionDateAdded.getConvictionDate());
        }
    }

    @Handles("progression.event.conviction-date-removed")
    public void removeConvictionDate(final JsonEnvelope event) {
        final ConvictionDateRemoved convictionDateRemoved = jsonObjectConverter.convert(event.payloadAsJsonObject(), ConvictionDateRemoved.class);
        if(convictionDateRemoved.getCourtApplicationId() == null) {
            updateConvictionDateToOffenceUnderProsecutionCase(convictionDateRemoved.getCaseId(), convictionDateRemoved.getOffenceId(), null);
        }else{
            updateConvictionDateToApplication(convictionDateRemoved.getCourtApplicationId(), convictionDateRemoved.getOffenceId(), null);
        }
    }

    private void updateConvictionDateToOffenceUnderProsecutionCase(final UUID caseId, final UUID offenceId, final LocalDate convictionDate) {
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final UUID offenceIdToBeUpdated = offenceId;

        for (final Defendant defendant : prosecutionCase.getDefendants()) {
            Offence updatedOffence = null;
            boolean isConvictionDateUpdated = false;
            for (final Offence offence : defendant.getOffences()) {
                if (offence.getId().equals(offenceIdToBeUpdated)) {
                    isConvictionDateUpdated = true;
                    updatedOffence = updateOffenceConvictionDate(offence, convictionDate);
                }
            }
            updateDefendantOffences(offenceIdToBeUpdated, defendant, updatedOffence, isConvictionDateUpdated);
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));
    }

    private void updateConvictionDateToApplication(final UUID courtApplicationId, final UUID offenceId, final LocalDate convictionDate) {
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(courtApplicationId);
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication;
        if(offenceId == null){
            updatedCourtApplication = getCourtApplicationWithConvictionDate(initiateCourtApplicationProceedings.getCourtApplication(), convictionDate);
        }else{
            updatedCourtApplication = getCourtApplicationWithConvictionDate(initiateCourtApplicationProceedings.getCourtApplication(), offenceId, convictionDate);
        }
        final InitiateCourtApplicationProceedings updatedInitiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withValuesFrom(initiateCourtApplicationProceedings)
                .withCourtApplication(updatedCourtApplication)
                .build();
        initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedInitiateCourtApplicationProceedings).toString());
        initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);

        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(courtApplicationId);
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
        courtApplicationRepository.save(courtApplicationEntity);
    }

    private Offence updateOffenceConvictionDate(Offence offence, LocalDate convictionDate) {
        return new Offence.Builder().withValuesFrom(offence).withConvictionDate(convictionDate).build();
    }

    private void updateDefendantOffences(UUID offenceIdToBeUpdated, Defendant defendant, Offence updatedOffence, boolean isConvictionDateUpdated) {
        if (isConvictionDateUpdated) {
            final List<Offence> testOffences = defendant.getOffences().stream()
                    .filter(offence -> !offence.getId().equals(offenceIdToBeUpdated))
                    .collect(Collectors.toList());
            defendant.getOffences().clear();
            defendant.getOffences().addAll(testOffences);
            defendant.getOffences().add(updatedOffence);
        }
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private  CourtOrderOffence getCourtOrderOffenceWithConvictionDate(final CourtOrderOffence o, final UUID offenceId, final LocalDate convictionDate) {
        return !o.getOffence().getId().equals(offenceId) ? o : CourtOrderOffence.courtOrderOffence().withValuesFrom(o)
                .withOffence(Offence.offence().withValuesFrom(o.getOffence())
                        .withConvictionDate(convictionDate)
                        .build())
                .build();
    }

    private Offence getCourtApplicationOffenceWithConvictionDate(final Offence courtApplicationOffence, final UUID offenceId, final LocalDate convictionDate) {
        return !courtApplicationOffence.getId().equals(offenceId) ? courtApplicationOffence :
                Offence.offence().withValuesFrom(courtApplicationOffence)
                                .withConvictionDate(convictionDate)
                                .build();
    }

    private  CourtApplication getCourtApplicationWithConvictionDate(final CourtApplication courtApplication, final LocalDate convictionDate){
        return courtApplication().withValuesFrom(courtApplication)
                .withConvictionDate(convictionDate)
                .build();
    }

    private  CourtApplication getCourtApplicationWithConvictionDate(final CourtApplication courtApplication, final UUID offenceId, final LocalDate convictionDate){
        return courtApplication().withValuesFrom(courtApplication)
                .withCourtApplicationCases(courtApplication.getCourtApplicationCases() == null ? null : courtApplication.getCourtApplicationCases().stream()
                        .map(applicationCase -> CourtApplicationCase.courtApplicationCase().withValuesFrom(applicationCase)
                                .withOffences(applicationCase.getOffences().stream()
                                        .map(courtApplicationOffence -> getCourtApplicationOffenceWithConvictionDate(courtApplicationOffence, offenceId, convictionDate))
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .withCourtOrder(courtApplication.getCourtOrder() == null ? null : of(courtApplication.getCourtOrder())
                        .map(co -> CourtOrder.courtOrder().withValuesFrom(co)
                                .withCourtOrderOffences(co.getCourtOrderOffences().stream()
                                        .map(o -> getCourtOrderOffenceWithConvictionDate(o, offenceId, convictionDate))
                                        .collect(toList()))
                                .build())
                        .orElse(null))
                .build();
    }
}
