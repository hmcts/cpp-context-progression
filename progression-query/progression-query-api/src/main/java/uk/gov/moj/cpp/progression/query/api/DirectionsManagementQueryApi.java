package uk.gov.moj.cpp.progression.query.api;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection;
import uk.gov.moj.cpp.progression.domain.pojo.ReferenceDataDirectionManagementType;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;
import uk.gov.moj.cpp.progression.query.view.DirectionQueryView;
import uk.gov.moj.cpp.progression.query.view.service.DefendantService;
import uk.gov.moj.cpp.progression.query.view.service.transformer.AssigneeTransformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.WitnessPetTransformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.WitnessPtphTransformer;
import uk.gov.moj.cpp.progression.service.RefDataService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@ServiceComponent(Component.QUERY_API)
public class DirectionsManagementQueryApi {

    public static final String STRUCTURED_FORM_ID = "structuredFormId";
    public static final String PET_ID = "petId";
    public static final String DATA = "data";
    public static final String LAST_UPDATED = "lastUpdated";
    private static final String COURT_FORM_ID = "courtFormId";



    private static final String CASE_ID = "caseId";

    private static final String FORM_ID = "formId";


    private static final String PET_FORM_TYPE = "PET";
    public static final String REGEX_COMMA = ",";



    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DirectionQueryView directionQueryView;

    @Inject
    private RefDataService refDataService;

    @Inject
    private DefendantService defendantService;

    @Inject
    ProgressionService progressionService;

    @Inject
    WitnessPetTransformer witnessPetTransformer;

    @Inject
    WitnessPtphTransformer witnessPtphTransformer;

    @Inject
    AssigneeTransformer assigneeTransformer;

    @Inject
    @ServiceComponent(Component.QUERY_API)
    private Requester requester;

    @Handles("progression.query.form-directions")
    public JsonEnvelope getPetCaseDirections(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String formType = envelope.payloadAsJsonObject().getString("formType");
        final List<String> categories = Optional.ofNullable(payload.getString("categories", null))
                .map(v -> Arrays.asList(v.split(",")))
                .orElse(new ArrayList<>());

        List<ReferenceDataDirectionManagementType> directionManagementTypes = refDataService.getDirectionManagementTypes()
                .stream()
                .filter(v -> formType.equals(v.getFormType()))
                .filter(v -> !"Custom".equals(v.getVariant()))
                .collect(Collectors.toList());
        if (!categories.isEmpty()) {
            directionManagementTypes = directionManagementTypes.stream()
                    .filter(v -> nonNull(v.getCategory()))
                    .filter(d -> !Collections.disjoint(Arrays.asList(d.getCategory().split(",")), categories))
                    .collect(Collectors.toList());
        }
        final JsonArrayBuilder refDataDirectionsInJson = Json.createArrayBuilder();

        if (!directionManagementTypes.isEmpty()) {
            final List<RefDataDirection> refDataDirections = transformDirection(envelope, directionManagementTypes);


            final List<RefDataDirection> refDataDirectionsSortedOnSequence = refDataDirections.stream()
                    .filter(v -> v != null && nonNull(v.getSequenceNumber()))
                    .sorted(Comparator.comparing(RefDataDirection::getSequenceNumber))
                    .collect(Collectors.toList());

            IntStream.range(0, refDataDirectionsSortedOnSequence.size())
                    .forEach(idx -> refDataDirectionsInJson.add(objectToJsonObjectConverter.convert(
                            buildRefDataDirection(refDataDirectionsSortedOnSequence, idx, formType))));

        }

        return envelopeFrom(envelope.metadata(),
                Json.createObjectBuilder().add("directions", refDataDirectionsInJson.build()).build());
    }

    private RefDataDirection buildRefDataDirection(final List<RefDataDirection> refDataDirectionsSortedOnSequence, final int idx, final String formType) {
        final RefDataDirection.Builder builderRefDataDirection = RefDataDirection.refDataDirection().withValuesFrom(
                refDataDirectionsSortedOnSequence.get(idx));

        if (PET_FORM_TYPE.equals(formType)) {
            builderRefDataDirection.withSequenceNumber(idx);
        }

        return builderRefDataDirection.build();
    }

    private List<RefDataDirection> transformDirection(final JsonEnvelope envelope, final List<ReferenceDataDirectionManagementType> directionManagementTypes) {
        final List<RefDataDirection> refDataDirections = new ArrayList<>();
        final Map<UUID, String> witnesses;
        final Map<UUID, String> assignees;

        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        final UUID formId = UUID.fromString(envelope.payloadAsJsonObject().getString(FORM_ID));
        final List<String> defendantIds = Optional.ofNullable(envelope.payloadAsJsonObject().getString("defendantIds", null))
                .map(v -> Arrays.asList(v.split(REGEX_COMMA)))
                .orElse(new ArrayList<>())
                .stream()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());

        final String formType = envelope.payloadAsJsonObject().getString("formType");

        final List<Defendant> defendantListFromDefendantService = defendantService.getDefendantList(envelope);
        List<Defendant> defendantListTmp = defendantListFromDefendantService;
        if (CollectionUtils.isNotEmpty(defendantIds)){
            defendantListTmp = defendantListFromDefendantService
                    .stream()
                    .filter(defendant -> defendantIds.contains(defendant.getId().toString()))
                    .collect(Collectors.toList());
        }

        //This code block added because defendantList is used in lambda it should be final.
        final List<Defendant> defendantList = defendantListTmp;

        if (PET_FORM_TYPE.equals(formType)) {
            assignees = Collections.emptyMap();
            witnesses = witnessPetTransformer.transform(getPetForm(formId, requester, envelope));
        } else {
            final JsonObject ptphFormJsonObj = getPtphForm(caseId, formId, requester, envelope);
            assignees = assigneeTransformer.transform(ptphFormJsonObj);
            witnesses = witnessPtphTransformer.transform(ptphFormJsonObj);
        }

        final List<UUID> directionIds = directionManagementTypes.stream().map(ReferenceDataDirectionManagementType::getId).collect(Collectors.toList());
        final List<Direction> directions = refDataService.getDirections()
                .stream().filter(v -> directionIds.contains(v.getDirectionId()))
                .collect(Collectors.toList());

        for (Direction direction : directions) {
            final Optional<ReferenceDataDirectionManagementType> referenceDataDirectionManagementType = directionManagementTypes
                    .stream().filter(v -> v.getId().equals(direction.getDirectionId()))
                    .findFirst();
            referenceDataDirectionManagementType.ifPresent(dataDirectionManagementType -> refDataDirections.add(
                    directionQueryView.getTransformedDirections(direction, dataDirectionManagementType,
                            defendantList, witnesses, assignees, false, formType)
            ));
        }
        return refDataDirections;
    }


    public JsonObject getPetForm( final UUID formId, final Requester requester, final JsonEnvelope query) {
       JsonObject jsonObject = getPet(requester, query, formId);  ;
       return jsonObject;
    }

    private JsonObject getPet(final Requester requester, final JsonEnvelope query,final UUID petFormId) {
        final JsonEnvelope materialResponse = requester.request(envelopeFrom(metadataFrom(query.metadata()).withName("material.query.structured-form"), createObjectBuilder()
                .add(STRUCTURED_FORM_ID, petFormId.toString())
                .build()));
   return createObjectBuilder()
                .add(PET_ID, petFormId.toString())
                .add(FORM_ID, petFormId.toString())
                .add(DATA, materialResponse.payloadAsJsonObject().getString(DATA))
                .add(LAST_UPDATED, materialResponse.payloadAsJsonObject().getString(LAST_UPDATED))
                .build();
    }

    public JsonObject getPtphForm(final UUID caseId, final UUID courtFormId, final Requester requester, final JsonEnvelope query) {
        final JsonEnvelope materialResponse = requester.request(envelopeFrom(metadataFrom(query.metadata()).withName("material.query.structured-form"), createObjectBuilder()
                .add(STRUCTURED_FORM_ID, courtFormId.toString())
                .build()));
        final JsonObject responseJsonObject = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(FORM_ID, courtFormId.toString())
                .add(DATA, materialResponse.payloadAsJsonObject().getString(DATA))
                .add(LAST_UPDATED, materialResponse.payloadAsJsonObject().getString(LAST_UPDATED))
                .build();

        return responseJsonObject;
    }
}
