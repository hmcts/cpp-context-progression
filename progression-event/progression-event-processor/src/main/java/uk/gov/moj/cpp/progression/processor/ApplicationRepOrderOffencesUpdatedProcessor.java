package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.deltaspike.core.util.CollectionUtils.isEmpty;
import static uk.gov.justice.progression.courts.OffencesForDefendantChanged.offencesForDefendantChanged;
import static uk.gov.justice.progression.courts.UpdatedOffences.updatedOffences;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;

import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.UpdatedOffences;
import uk.gov.justice.progression.query.laa.HearingSummary;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.event.ApplicationRepOrderUpdatedForApplication;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S3655",})
@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationRepOrderOffencesUpdatedProcessor {

    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    protected static final String UPDATE_APPLICATION_LAA_REFERENCE_FOR_HEARING = "progression.command.update-application-laa-reference-for-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRepOrderOffencesUpdatedProcessor.class.getCanonicalName());
    public static final String APPLICATION_CASE_DEFENDANT_ORGANISATIONS = "applicationCaseDefendantOrganisations";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository repository;

    @Inject
    private ProgressionService progressionService;

    //listen to this private event to raise public event only
    @Handles("progression.event.application-reporder-offences-updated")
    public void handleApplicationOffencesUpdatedEvent(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received prosecution application offences updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final ApplicationReporderOffencesUpdated applicationOffencesUpdated =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ApplicationReporderOffencesUpdated.class);

        //Hearing and Listing contexts
        sender.send(envelop(JsonHelper.removeProperty(jsonEnvelope.payloadAsJsonObject(), APPLICATION_CASE_DEFENDANT_ORGANISATIONS)).withName(PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED).withMetadataFrom(jsonEnvelope));

        final Optional<List<HearingSummary>> hearingSummaryListOptional = progressionService.getHearingsForApplication(applicationOffencesUpdated.getApplicationId());
        if (hearingSummaryListOptional.isEmpty() || CollectionUtils.isEmpty(hearingSummaryListOptional.get())) {
            return;
        }

        //update hearing.application.courtApplicationCases.offence.laaReference
        hearingSummaryListOptional.get().stream().forEach(hearingSummary -> {
            final JsonObject jsonObject = updateEnvelopeWithHearingId(jsonEnvelope, hearingSummary.getHearingId());
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, UPDATE_APPLICATION_LAA_REFERENCE_FOR_HEARING).apply(jsonObject));
        });

        final CourtApplicationEntity applicationEntity = repository.findByApplicationId(applicationOffencesUpdated.getApplicationId());

        if (applicationEntity == null) {
            return;
        }
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
        final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);

        if (!isValidSubject(applicationOffencesUpdated, persistedApplication)) {
            return;
        }
        final OffencesForDefendantChanged offencesForDefendantChanged = getOffencesForDefendantChanged(persistedApplication.getCourtApplicationCases(), applicationOffencesUpdated.getApplicationCaseDefendantOrganisations(), applicationOffencesUpdated.getOffenceId());

        if (offencesForDefendantChanged == null) {
            return;
        }
        //Defence context
        sender.send(envelop(objectToJsonObjectConverter.convert(offencesForDefendantChanged)).withName(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).withMetadataFrom(jsonEnvelope));
    }


    @Handles("progression.event.application-rep-order-updated-for-application")
    public void handleApplicationRepOrderUpdatedForApplicationEvent(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received progression.event.application-rep-order-updated-for-application with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final ApplicationRepOrderUpdatedForApplication applicationRepOrderUpdatedForApplication = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ApplicationRepOrderUpdatedForApplication.class);

        //Hearing and Listing contexts
        sender.send(envelop(JsonHelper.removeProperty(jsonEnvelope.payloadAsJsonObject(), APPLICATION_CASE_DEFENDANT_ORGANISATIONS)).withName("public.progression.application-laa-reference-updated-for-application").withMetadataFrom(jsonEnvelope));

        final Optional<List<HearingSummary>> hearingSummaryListOptional = progressionService.getHearingsForApplication(applicationRepOrderUpdatedForApplication.getApplicationId());

        //update hearing.application.laaReference
        hearingSummaryListOptional.orElse(emptyList()).stream().forEach(hearingSummary -> {
            final JsonObject jsonObject = updateEnvelopeWithHearingId(jsonEnvelope, hearingSummary.getHearingId());
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, UPDATE_APPLICATION_LAA_REFERENCE_FOR_HEARING).apply(jsonObject));
        });

        final CourtApplicationEntity applicationEntity = repository.findByApplicationId(applicationRepOrderUpdatedForApplication.getApplicationId());

        if (applicationEntity == null) {
            return;
        }
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
        final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);

        if (!isValidSubject(applicationRepOrderUpdatedForApplication, persistedApplication)) {
            return;
        }
        final OffencesForDefendantChanged offencesForDefendantChanged = getOffencesForDefendantChanged(applicationRepOrderUpdatedForApplication.getApplicationCaseDefendantOrganisations(), applicationRepOrderUpdatedForApplication.getLaaReference(), jsonEnvelope);

        if (nonNull(offencesForDefendantChanged)) {
            //Defence context
            sender.send(envelop(objectToJsonObjectConverter.convert(offencesForDefendantChanged)).withName(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).withMetadataFrom(jsonEnvelope));
        }

    }

    private OffencesForDefendantChanged getOffencesForDefendantChanged(final List<CourtApplicationCase> courtApplicationCases, final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantList, final UUID offenceId) {

        if (isEmpty(applicationCaseDefendantList) || isEmpty(courtApplicationCases)) {
            return null;
        }

        final List<UpdatedOffences> updatedOffencesList = new ArrayList<>();
        courtApplicationCases.forEach(courtApplicationCase -> {
            final Optional<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisation = applicationCaseDefendantList.stream().filter(appCase -> appCase.getCaseId().equals(courtApplicationCase.getProsecutionCaseId())).findFirst();
            if (applicationCaseDefendantOrganisation.isPresent() && isNotEmpty(courtApplicationCase.getOffences())) {
                final List<Offence> offenceList = courtApplicationCase.getOffences().stream().filter(offence -> offence.getId().equals(offenceId)).toList();
                if (isNotEmpty(offenceList)) {
                    updatedOffencesList.add(getUpdatedOffences(applicationCaseDefendantOrganisation.get().getCaseId(), applicationCaseDefendantOrganisation.get().getDefendantId(), offenceList));
                }
            }
        });

        if (isEmpty(updatedOffencesList)) {
            return null;
        }

        return offencesForDefendantChanged()
                .withModifiedDate(LocalDate.now())
                .withUpdatedOffences(updatedOffencesList)
                .build();
    }

    private OffencesForDefendantChanged getOffencesForDefendantChanged(final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantList, final LaaReference laaReference, final JsonEnvelope jsonEnvelope) {

        if (isEmpty(applicationCaseDefendantList)) {
            return null;
        }

        final List<UpdatedOffences> updatedOffencesList = applicationCaseDefendantList.stream()
                .map(organisation -> {
                    final JsonObject prosecutionCaseJson = progressionService.getProsecutionCaseById(jsonEnvelope, organisation.getCaseId().toString());
                    return nonNull(prosecutionCaseJson) ?
                            getUpdatedOffencesForOrganisation(prosecutionCaseJson, organisation, laaReference) :
                            Optional.<UpdatedOffences>empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (isEmpty(updatedOffencesList)) {
            return null;
        }

        return offencesForDefendantChanged()
                .withModifiedDate(LocalDate.now())
                .withUpdatedOffences(updatedOffencesList)
                .build();
    }

    private Optional<UpdatedOffences> getUpdatedOffencesForOrganisation(final JsonObject prosecutionCaseJson, final ApplicationCaseDefendantOrganisation organisation, final LaaReference laaReference) {
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final List<Offence> offenceList = prosecutionCase.getDefendants().stream()
                .filter(defendant -> nonNull(defendant) && defendant.getId().equals(organisation.getDefendantId()))
                .findFirst()
                .map(defendant -> defendant.getOffences().stream()
                        .map(offence -> Offence.offence()
                                .withValuesFrom(offence)
                                .withLaaApplnReference(laaReference)
                                .build())
                        .toList())
                .orElse(emptyList());

        if (isNotEmpty(offenceList)) {
            return Optional.of(getUpdatedOffences(organisation.getCaseId(), organisation.getDefendantId(), offenceList));
        }

        return Optional.empty();
    }


    private UpdatedOffences getUpdatedOffences(final UUID prosecutionCaseId, final UUID defendantId, final List<Offence> offenceList) {
        return updatedOffences()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offenceList)
                .build();
    }

    private boolean isValidSubject(final ApplicationReporderOffencesUpdated application, final CourtApplication courtApplication) {
        return courtApplication.getSubject() != null && application.getSubjectId().equals(courtApplication.getSubject().getId())
                && isNotEmpty(courtApplication.getCourtApplicationCases());
    }

    private boolean isValidSubject(final ApplicationRepOrderUpdatedForApplication application, final CourtApplication courtApplication) {
        return courtApplication.getSubject() != null && application.getSubjectId().equals(courtApplication.getSubject().getId());
    }

    private JsonObject updateEnvelopeWithHearingId(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final JsonObject jsonObject = removeProperty(jsonEnvelope.payloadAsJsonObject(), APPLICATION_CASE_DEFENDANT_ORGANISATIONS);
        return addProperty(jsonObject, "hearingId", hearingId.toString());
    }
}