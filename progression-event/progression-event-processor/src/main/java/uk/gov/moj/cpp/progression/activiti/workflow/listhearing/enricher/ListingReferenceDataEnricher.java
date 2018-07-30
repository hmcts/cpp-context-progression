package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.external.domain.listing.StatementOfOffence;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class ListingReferenceDataEnricher implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingReferenceDataEnricher.class.getName());

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ReferenceDataService referenceDataService;


    @Handles("referenceData.enricher.dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework enricher
    }

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception {
        final ListingCase savedListingCase = (ListingCase) delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD);

        final Map<UUID, List<List<Defendant>>> collectDefendants = savedListingCase.getHearings().stream().collect(Collectors.groupingBy(Hearing::getId, Collectors.mapping(Hearing::getDefendants, Collectors.toList())));

        final ListingCase listingCaseWithRefData = new ListingCase(savedListingCase.getCaseId(), savedListingCase.getUrn(),
                savedListingCase.getHearings().stream().map(h ->
                        new Hearing(h.getId(), h.getCourtCentreId(), h.getType(), h.getStartDate(), h.getEstimateMinutes(),
                                collectDefendants.get(h.getId()).stream().flatMap(Collection::stream).map(defendant -> new Defendant(defendant.getId(), defendant.getPersonId(), defendant.getFirstName(), defendant.getLastName(), defendant.getDateOfBirth(), defendant.getBailStatus(), defendant.getCustodyTimeLimit(), defendant.getDefenceOrganisation(),
                                        defendant.getOffences().stream().map(o -> {
                                            final Optional<JsonObject> offenceCode = referenceDataService.getOffenceByCjsCode(envelopeFor(delegateExecution.getVariable(USER_ID).toString()), o.getOffenceCode());
                                            if (offenceCode.isPresent()) {
                                                return new Offence(o.getId(), o.getOffenceCode(), o.getStartDate(), o.getEndDate(),
                                                        jsonObjectToObjectConverter.convert(offenceCode.get(), StatementOfOffence.class));
                                            }
                                            LOGGER.error("Reference data offence code not found {}", o.getOffenceCode());
                                            return o;

                                        }).collect(Collectors.toList()))).collect(Collectors.toList()),h.getCourtRoomId(),h.getJudgeId(),h.getStartTime()))
                        .collect(Collectors.toList()));

        delegateExecution.setVariable(SEND_CASE_FOR_LISTING_PAYLOAD, listingCaseWithRefData);

    }

    private JsonEnvelope envelopeFor(final String userId) {
        return envelopeFrom(
                metadataWithRandomUUID("to-be-replaced")
                        .withUserId(userId)
                        .build(),
                null);
    }
}
