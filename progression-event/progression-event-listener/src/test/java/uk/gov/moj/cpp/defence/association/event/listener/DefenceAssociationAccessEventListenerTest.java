package uk.gov.moj.cpp.defence.association.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.defence.asscociation.event.listener.DefenceAssociationAccessEventListener;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZoneId;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceAssociationAccessEventListenerTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANISATION_ID = UUID.randomUUID();
    private static final String UTC = "UTC";
    private static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC);

    @Mock
    private DefenceAssociationRepository repository;

    @Captor
    private ArgumentCaptor<DefenceAssociation> argumentCaptor;

    @InjectMocks
    private DefenceAssociationAccessEventListener eventListener;

    @Test
    public void shouldPerformAssocitation() {

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.event.defence-organisation-associated")
                .withId(UUID.randomUUID())
                .withUserId(USER_ID.toString())
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadata,
                createObjectBuilder()
                        .add("defendantId", DEFENDANT_ID.toString())
                        .add("organisationId", ORGANISATION_ID.toString())
                        .add("associationDate","2019-07-09T16:27:39.744Z")
                        .build());

        eventListener.processOrganisationAssociated(requestEnvelope);

        verify(repository).save(argumentCaptor.capture());
        final DefenceAssociation entity = argumentCaptor.getValue();
        assertEquals(DEFENDANT_ID ,entity.getDefendantId());
        assertEquals(ORGANISATION_ID,entity.getDefenceAssociationHistories().stream().findFirst().get().getGrantorOrgId());
        assertEquals(USER_ID,entity.getDefenceAssociationHistories().stream().findFirst().get().getGrantorUserId());

    }
}