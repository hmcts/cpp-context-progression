package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantsAddedToCourtProceedingsProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

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

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class);

        final String prosecutionCaseId = defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString();
        final Optional<JsonObject> prosecutionCaseJsonObject = progressionService.getProsecutionCaseDetailById(jsonEnvelope, prosecutionCaseId);

        if(prosecutionCaseJsonObject.isPresent()) {

            final JsonObject jsonObject = createObjectBuilder()
                    .add("prosecutionCaseId", prosecutionCaseId)
                    .build();

            sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.process-matched-defendants").apply(jsonObject));

            final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("hearingsAtAGlance"),
                    GetHearingsAtAGlance.class);

            final List<Hearings> futureHearings = hearingsAtAGlance.getHearings().stream()
                    .filter(hearing -> hearing.getHearingDays().stream()
                            .anyMatch(hearingDay -> hearingDay.getSittingDay().toLocalDate().compareTo(LocalDate.now()) >=0))
                    .collect(Collectors.toList());

            if (!futureHearings.isEmpty()) {
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, "public.progression.defendants-added-to-court-proceedings").apply(payload));
            } else {
                    final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                            Collections.singletonList(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                                    ProsecutionCase.class)), defendantsAddedToCourtProceedings.getListHearingRequests(), UUID.randomUUID());
                    listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
            }
        }
    }
}
