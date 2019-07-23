package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantsAddedToCourtProceedingsProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    static final String PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class);

        final Optional<JsonObject> prosecutionCaseJsonObject = progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString());

        if(prosecutionCaseJsonObject.isPresent()) {

            final GetCaseAtAGlance caseAtAGlance = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("caseAtAGlance"),
                    GetCaseAtAGlance.class);

            final List<Hearings> futureHearings = caseAtAGlance.getHearings().stream().filter(h -> h.getHearingDays().stream()
                    .anyMatch(hday -> hday.getSittingDay().toLocalDate().compareTo(LocalDate.now())>=0
                    )).collect(Collectors.toList());

            if (!futureHearings.isEmpty()) {
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS).apply(payload));
            } else {

                    final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                            Collections.singletonList(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                                    ProsecutionCase.class)), defendantsAddedToCourtProceedings.getListHearingRequests(), UUID.randomUUID());
                    listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

            }

        }
    }
}
