package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class FormQueryView {

    private static final String COURT_FORM_ID = "courtFormId";
    private static final String CASE_ID = "caseId";
    private static final String FORM_TYPE = "formType";
    private static final String DEFENDANTS = "defendants";
    private static final String FORMS = "forms";
    private static final String DEFENDANT_ID = "defendantId";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String LAST_UPDATED = "lastUpdated";

    @Inject
    private CaseDefendantOffenceRepository caseDefendantOffenceRepository;

    @Handles("progression.query.forms-for-case")
    public JsonEnvelope getFormsForCase(final JsonEnvelope envelope) {
        final JsonObject payloadAsJsonObject = envelope.asJsonObject();
        final UUID caseId = fromString(payloadAsJsonObject.getString(CASE_ID));
        final String formType = payloadAsJsonObject.getString(FORM_TYPE, null);
        final List<CaseDefendantOffence> formCaseDefendantOffenceListForCase;
        if (nonNull(formType)) {
            formCaseDefendantOffenceListForCase = caseDefendantOffenceRepository.findByCaseIdAndFormType(caseId, FormType.valueOf(formType));
        } else {
            formCaseDefendantOffenceListForCase = caseDefendantOffenceRepository.findByCaseId(caseId);
        }

        final List<UUID> courtFormIds = formCaseDefendantOffenceListForCase.stream().map(CaseDefendantOffence::getCourtFormId)
                .distinct()
                .collect(toList());

        final JsonArrayBuilder formsArrayBuilder = createArrayBuilder();

        courtFormIds.stream().map(courtFormId -> {
            final List<CaseDefendantOffence> caseDefendantOffenceList = formCaseDefendantOffenceListForCase.stream()
                    .filter(item -> item.getCourtFormId().equals(courtFormId))
                    .collect(toList());

            return formsArrayBuilder.add(buildForm(courtFormId, caseDefendantOffenceList, caseDefendantOffenceList.get(0).getFormType()));
        }).collect(toList());

        final JsonObject responseJson = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(FORMS, formsArrayBuilder)
                .build();

        return envelopeFrom(envelope.metadata(), responseJson);
    }

    @Handles("progression.query.form")
    public JsonEnvelope getForm(final JsonEnvelope envelope) {
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();

        final UUID caseId = fromString(payloadAsJsonObject.getString(CASE_ID));
        final UUID courtFormId = fromString(payloadAsJsonObject.getString(COURT_FORM_ID));

        final List<CaseDefendantOffence> defendantOffencesList = caseDefendantOffenceRepository.findByCourtFormId(courtFormId);

        final JsonObjectBuilder responseJsonBuilder = createObjectBuilder();

        if (!defendantOffencesList.isEmpty()) {
            responseJsonBuilder
                    .add(CASE_ID, caseId.toString())
                    .add(COURT_FORM_ID, courtFormId.toString())
                    .add(FORM_TYPE, defendantOffencesList.get(0).getFormType().name())
                    .add(DEFENDANTS, buildDefendantsAndOffences(defendantOffencesList));
        }
        return envelopeFrom(envelope.metadata(), responseJsonBuilder.build());
    }

    private JsonObject buildForm(final UUID courtFormId, final List<CaseDefendantOffence> defendantOffencesList, final FormType formType) {
        final JsonObjectBuilder formBuilder = createObjectBuilder()
                .add(COURT_FORM_ID, courtFormId.toString())
                .add(FORM_TYPE, formType.name())
                .add(DEFENDANTS, buildDefendantsAndOffences(defendantOffencesList));

        final Optional<String> lastUpdated = defendantOffencesList.stream()
                .filter(caseDefendantOffence -> nonNull(caseDefendantOffence.getLastUpdated()))
                .map(caseDefendant -> caseDefendant.getLastUpdated().format(ZONE_DATETIME_FORMATTER))
                .findFirst();

        lastUpdated.ifPresent(date -> formBuilder.add(LAST_UPDATED, date));

        return formBuilder.build();
    }

    private Map<UUID, List<CaseDefendantOffence>> groupByDefendantId(final List<CaseDefendantOffence> formCaseDefendantOffenceList) {
        final Map<UUID, List<CaseDefendantOffence>> formCaseDefendantOffenceMapByDefendant = new HashMap<>();
        formCaseDefendantOffenceList.forEach(each -> {
            if (!formCaseDefendantOffenceMapByDefendant.containsKey(each.getDefendantId())) {
                formCaseDefendantOffenceMapByDefendant.put(each.getDefendantId(), new ArrayList<>());
            }

            formCaseDefendantOffenceMapByDefendant.get(each.getDefendantId()).add(each);
        });
        return formCaseDefendantOffenceMapByDefendant;
    }

    private JsonArrayBuilder buildDefendantsAndOffences(final List<CaseDefendantOffence> defendantOffencesList) {
        final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();

        // group by defendant ids
        final Map<UUID, List<CaseDefendantOffence>> offencesGroupedByDefendant = groupByDefendantId(defendantOffencesList);

        offencesGroupedByDefendant.keySet().forEach(defendantId -> {
            final JsonObjectBuilder defendantObjectBuilder = createObjectBuilder().add(DEFENDANT_ID, defendantId.toString());
            defendantsArrayBuilder.add(defendantObjectBuilder);
        });

        return defendantsArrayBuilder;
    }
}
