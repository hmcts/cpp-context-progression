package uk.gov.moj.cpp.progression.event;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.StatementOfOffence;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffence;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffences;
import uk.gov.moj.cpp.progression.domain.event.defendant.BaseDefendantOffences;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChanged;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChangedPublic;
import uk.gov.moj.cpp.progression.domain.event.defendant.DeletedDefendantOffences;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantOffencesChangedListener {
    private static final Logger LOGGER = LoggerFactory
                    .getLogger(DefendantOffencesChangedListener.class.getCanonicalName());
    protected static final String PUBLIC_DEFENDANT_OFFENCES_CHANGED =
                    "public.progression.defendant-offences-changed";
    private static final String FIELD_TITLE = "title";
    private static final String LEGISLATION = "legislation";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    
    @Handles("progression.events.defendant-offences-changed")
    public void publicDefendantOffencesChanged(final JsonEnvelope event) {
        LOGGER.info("Received progression.defendant-offences-changed");
        final DefendantOffencesChanged defendantOffencesChanged = this.jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                DefendantOffencesChanged.class);

        final List<BaseDefendantOffences> addedOffences = defendantOffencesChanged.getAddedOffences();
        final List<DefendantOffences> addedOffencesWithSof = convertToDefendantOffences(event, addedOffences);
        List<BaseDefendantOffences> updatedOffences = defendantOffencesChanged.getUpdatedOffences();
        final List<DefendantOffences> updatedOffencesWithSof = convertToDefendantOffences(event, updatedOffences);
        List<DeletedDefendantOffences> deletedDefendantOffences = getDeletedDefendantOffences(defendantOffencesChanged);

        final DefendantOffencesChangedPublic publicEvent  =  new DefendantOffencesChangedPublic();
        publicEvent.setAddedOffences(addedOffencesWithSof);
        publicEvent.setUpdatedOffences(updatedOffencesWithSof);
        publicEvent.setDeletedOffences(deletedDefendantOffences);

        LOGGER.info("Sending public.progression.defendant-offences-changed");
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_DEFENDANT_OFFENCES_CHANGED)
                        .apply(publicEvent));
    }

    private List<DeletedDefendantOffences> getDeletedDefendantOffences(DefendantOffencesChanged defendantOffencesChanged) {
        List<DeletedDefendantOffences> deletedDefendantOffences = null;
        if(defendantOffencesChanged.getDeletedOffences()!=null) {
            deletedDefendantOffences = defendantOffencesChanged.getDeletedOffences().stream()
                    .map(deletedOffences -> {
                        List<UUID> deletedOffenceIds = deletedOffences.getOffences().stream().map(o -> o.getId()).collect(Collectors.toList());
                        return new DeletedDefendantOffences(deletedOffences.getDefendantId(), deletedOffences.getCaseId(), deletedOffenceIds);
                    })
                    .collect(toList());
        }
        return deletedDefendantOffences;
    }

    private List<DefendantOffences> convertToDefendantOffences(JsonEnvelope event, List<BaseDefendantOffences> addedOffences) {
        List<DefendantOffences> addedOffencesWithStatementOfOffence = null;
        if (addedOffences != null) {
             addedOffencesWithStatementOfOffence = addedOffences.stream()
                    .map(boa -> {
                        final List<DefendantOffence> dos = boa.getOffences().stream()
                                .map(o -> new DefendantOffence(o.getId(), o.getOffenceCode(), o.getWording(),
                                        o.getStartDate(), o.getEndDate(), o.getCount(), o.getConvictionDate(),
                                        getStatementOfOffence(o.getOffenceCode(), event))
                                ).collect(toList());
                        return new DefendantOffences(boa.getDefendantId(), boa.getCaseId(), dos);
                    })
                    .collect(toList());
        }
        return addedOffencesWithStatementOfOffence;
    }


    private StatementOfOffence getStatementOfOffence(final String offenceCode, final JsonEnvelope event) {
         final Optional<JsonObject> sofJsonOptional = referenceDataService.getOffenceByCjsCode(event, offenceCode);
         if (sofJsonOptional.isPresent()) {
             final JsonObject sofJsonObject = sofJsonOptional.get();
             final String title = sofJsonObject.getString(FIELD_TITLE,null);
             final String legislation =sofJsonObject.getString(LEGISLATION, null);
             return new StatementOfOffence(title,legislation);
         }
         return null;
     }
}
