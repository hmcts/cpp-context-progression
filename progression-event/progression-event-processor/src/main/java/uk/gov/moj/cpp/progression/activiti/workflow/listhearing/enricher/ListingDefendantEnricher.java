package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.progression.activiti.common.EndDate;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class ListingDefendantEnricher implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingDefendantEnricher.class.getName());

    @Inject
    ProgressionService progressionService;

    @Handles("listing.defendant-enricher-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework
    }

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception {
        final ListingCase savedListingCase = (ListingCase) delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD);
        final UUID caseId = savedListingCase.getCaseId();
        final Map<UUID, List<List<Defendant>>> collectDefendants = savedListingCase.getHearings().stream().collect(Collectors.groupingBy(Hearing::getId, Collectors.mapping(Hearing::getDefendants, Collectors.toList())));

        final ListingCase listingCaseWithRefData = new ListingCase(savedListingCase.getCaseId(), savedListingCase.getUrn(),
                savedListingCase.getHearings().stream().map(h ->
                        new Hearing(h.getId(), h.getCourtCentreId(), h.getType(), h.getStartDate(), h.getEstimateMinutes(),
                                collectDefendants.get(h.getId()).stream().flatMap(Collection::stream).map(defendant -> {
                                    JsonObject defendantJson = progressionService.getDefendantByDefendantId(
                                            delegateExecution.getVariable(USER_ID).toString(), caseId.toString(),
                                            defendant.getId().toString()).get();
                                    final JsonObject personJson = defendantJson.getJsonObject("person");
                                    return new Defendant(defendant.getId(), UUID.fromString(personJson.getString("id")), personJson.getString("firstName", ""), personJson.getString("lastName", ""), personJson.getString("dateOfBirth", ""), defendantJson.getString("bailStatus", ""), defendantJson.getString("custodyTimeLimitDate", null), defendantJson.getString("defenceSolicitorFirm", ""),
                                            defendant.getOffences().stream().map(o -> {
                                                final Optional<JsonObject> offenceJson = getOffence(o.getId(), defendantJson.getJsonArray("offences"));
                                                if (offenceJson.isPresent()) {
                                                    JsonObject offenceJsonValue = offenceJson.get();
                                                    return new Offence(o.getId(), offenceJsonValue.getString("offenceCode", null), offenceJsonValue.getString("startDate", null), offenceJsonValue.getString("endDate", EndDate.VALUE),
                                                            o.getStatementOfOffence());
                                                }
                                                LOGGER.error("Offence code not found {}", o.getId());
                                                return o;

                                            }).collect(Collectors.toList()));
                                }).collect(Collectors.toList()), h.getCourtRoomId(), h.getJudgeId(), h.getStartTime()))
                        .collect(Collectors.toList()));


        delegateExecution.setVariable(SEND_CASE_FOR_LISTING_PAYLOAD, listingCaseWithRefData);

    }


    private Optional<JsonObject> getOffence(final UUID offenceId, final JsonArray jsonArray) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject offenceJson = jsonArray.getJsonObject(i);
                if (offenceJson.getString("id").equals(offenceId.toString())) {
                    return Optional.of(offenceJson);
                }
            }

        }
        return Optional.empty();
    }
}
