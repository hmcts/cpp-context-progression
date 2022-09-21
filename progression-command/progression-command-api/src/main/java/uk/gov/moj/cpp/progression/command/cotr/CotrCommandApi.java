package uk.gov.moj.cpp.progression.command.cotr;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.command.service.OrganisationService;
import uk.gov.moj.progression.courts.AddFurtherInfoDefenceCotr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(COMMAND_API)
public class CotrCommandApi {

    @Inject
    private Requester requester;

    @Inject
    private Sender sender;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private OrganisationService organisationService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.create-cotr")
    public void createCotrForDefendantsOfHearing(final JsonEnvelope envelope) {
        final boolean isDefenceClient = userDetailsLoader.isDefenceClient(envelope, requester);
        if (!isDefenceClient) {
            sendEnvelopeWithName(envelope, "progression.command.create-cotr");
        } else {
            final List<UUID> associatedDefendantIds = organisationService.getAssociatedDefendants(envelope, requester);
            final CreateCotr createCotr = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), CreateCotr.class);

            final boolean canCreateCotr = canCreateCotrOrChangeDefendants(associatedDefendantIds, createCotr.getDefendantIds());
            if (canCreateCotr) {
                sendEnvelopeWithName(envelope, "progression.command.create-cotr");
            } else {
                throw new ForbiddenRequestException("User is not associated to defendant to change defendants in cotr");
            }
        }

    }

    @Handles("progression.serve-prosecution-cotr")
    public void serveProsecutionCotr(final JsonEnvelope envelope) {
        sendEnvelopeWithName(envelope, "progression.command.serve-prosecution-cotr");
    }

    @Handles("progression.archive-cotr")
    public void archiveCotr(final JsonEnvelope envelope) {
        sendEnvelopeWithName(envelope, "progression.command.archive-cotr");
    }

    @Handles("progression.serve-defendant-cotr")
    public void serveDefendantCotr(final JsonEnvelope envelope) {
        final boolean isDefenceClient = userDetailsLoader.isDefenceClient(envelope, requester);
        if (!isDefenceClient) {
            sendEnvelopeWithName(envelope, "progression.command.serve-defendant-cotr");
        } else {
            final List<UUID> associatedDefendantIds = organisationService.getAssociatedDefendants(envelope, requester);
            final ServeDefendantCotr serveDefendantCotr = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), ServeDefendantCotr.class);
            if (associatedDefendantIds.contains(serveDefendantCotr.getDefendantId())) {
                sendEnvelopeWithName(envelope, "progression.command.serve-defendant-cotr");
            } else {
                throw new ForbiddenRequestException("User is not associated to defendant to serve cotr");
            }
        }
    }

    @Handles("progression.change-defendants-cotr")
    public void changeDefendantsCotr(final JsonEnvelope envelope) {

        final boolean isDefenceClient = userDetailsLoader.isDefenceClient(envelope, requester);
        if (!isDefenceClient) {
            sendEnvelopeWithName(envelope, "progression.command.change-defendants-cotr");
        } else {
            final List<UUID> associatedDefendantIds = organisationService.getAssociatedDefendants(envelope, requester);
            final ChangeDefendantsCotr changeDefendantsCotr = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), ChangeDefendantsCotr.class);
            final List<UUID> defendantIdsFromCreateCotr = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(changeDefendantsCotr.getAddedDefendantIds())) {
                defendantIdsFromCreateCotr.addAll(changeDefendantsCotr.getAddedDefendantIds());
            }
            if (CollectionUtils.isNotEmpty(changeDefendantsCotr.getRemovedDefendantIds())) {
                defendantIdsFromCreateCotr.addAll(changeDefendantsCotr.getRemovedDefendantIds());
            }
            final boolean canCreateCotr = canCreateCotrOrChangeDefendants(associatedDefendantIds, defendantIdsFromCreateCotr);
            if (canCreateCotr) {
                sendEnvelopeWithName(envelope, "progression.command.change-defendants-cotr");
            } else {
                throw new ForbiddenRequestException("User is not associated to defendant to change defendants in cotr");
            }
        }

    }

    @Handles("progression.add-further-info-prosecution-cotr")
    public void addFurtherInfoForProsecutionCotr(final JsonEnvelope envelope) {
        sendEnvelopeWithName(envelope, "progression.command.add-further-info-prosecution-cotr");
    }

    @Handles("progression.add-further-info-defence-cotr")
    public void addFurtherInfoForDefenceCotr(final JsonEnvelope envelope) {

        final boolean isDefenceClient = userDetailsLoader.isDefenceClient(envelope, requester);
        if (!isDefenceClient) {
            sendEnvelopeWithName(envelope, "progression.command.add-further-info-defence-cotr");
        } else {
            final List<UUID> associatedDefendantIds = organisationService.getAssociatedDefendants(envelope, requester);
            final AddFurtherInfoDefenceCotr addFurtherInfoDefenceCotr = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), AddFurtherInfoDefenceCotr.class);
            if (associatedDefendantIds.contains(addFurtherInfoDefenceCotr.getDefendantId())) {
                sendEnvelopeWithName(envelope, "progression.command.add-further-info-defence-cotr");
            } else {
                throw new ForbiddenRequestException("User is not associated to defendant to serve cotr");
            }
        }
    }

    @Handles("progression.update-review-notes")
    public void updateReviewNotes(final JsonEnvelope envelope) {
        sendEnvelopeWithName(envelope, "progression.command.update-review-notes");
    }

    private void sendEnvelopeWithName(final JsonEnvelope envelope, final String name) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(name)
                .withMetadataFrom(envelope));
    }

    private boolean canCreateCotrOrChangeDefendants(final List<UUID> associatedDefendantIds, final List<UUID> defendantIdsFromRequest) {
        boolean canCreateCotrOrChangeDefendants = true;
        for (final UUID defendantId : defendantIdsFromRequest) {
            if (!associatedDefendantIds.contains(defendantId)) {
                canCreateCotrOrChangeDefendants = false;
            }
        }
        return canCreateCotrOrChangeDefendants;
    }

}
