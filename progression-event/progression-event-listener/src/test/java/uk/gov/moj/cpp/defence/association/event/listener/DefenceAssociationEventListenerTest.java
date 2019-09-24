package uk.gov.moj.cpp.defence.association.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceAssociationEventListenerTest {

    public static final String UTC = "UTC";
    private static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC);
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID ORGANISATION_ID = randomUUID();
    private static final String DEFENCE_ORGANISATION_ASSOCIATED_EVENT = "progression.event.defence-organisation-associated";
    private static final String DEFENCE_ORGANISATION_DISASSOCIATED_EVENT = "progression.event.defence-organisation-disassociated";
    private static final String END_DATE = "endDate";
    private static final String START_DATE = "startDate";

    @Mock
    private DefenceAssociationRepository repository;

    @Captor
    private ArgumentCaptor<DefenceAssociationDefendant> argumentCaptor;

    @InjectMocks
    private DefenceAssociationEventListener eventListener;

    @Test
    public void shouldPerformAssociation() {

        //Given
        final JsonEnvelope requestEnvelope = generateEvent(DEFENCE_ORGANISATION_ASSOCIATED_EVENT, USER_ID, DEFENDANT_ID, ORGANISATION_ID, START_DATE);

        //When
        eventListener.processOrganisationAssociated(requestEnvelope);

        //Then
        verify(repository).save(argumentCaptor.capture());
        final DefenceAssociationDefendant entity = argumentCaptor.getValue();
        assertEquals(DEFENDANT_ID, entity.getDefendantId());
        assertEquals(ORGANISATION_ID, entity.getDefenceAssociations().stream().findFirst().get().getOrgId());
        assertEquals(USER_ID, entity.getDefenceAssociations().stream().findFirst().get().getUserId());
        assertNull(entity.getDefenceAssociations().stream().findFirst().get().getEndDate());
    }

    @Test
    public void shouldProcessOrganisationDisassociated() {

        //Given
        addCurrentAssociationToRepository(USER_ID, DEFENDANT_ID, ORGANISATION_ID);

        final JsonEnvelope requestEnvelope = generateEvent(DEFENCE_ORGANISATION_DISASSOCIATED_EVENT, USER_ID, DEFENDANT_ID, ORGANISATION_ID, END_DATE);

        //When
        eventListener.processOrganisationDisassociated(requestEnvelope);

        //Then
        verify(repository).save(argumentCaptor.capture());
        final DefenceAssociationDefendant entity = argumentCaptor.getValue();
        assertEquals(DEFENDANT_ID, entity.getDefendantId());
        final DefenceAssociation defenceAssociation = entity.getDefenceAssociations().stream().findFirst().get();
        assertEquals(ORGANISATION_ID, defenceAssociation.getOrgId());
        assertEquals(USER_ID, defenceAssociation.getUserId());
        assertNotNull(defenceAssociation.getEndDate());
    }

    @Test
    public void shouldProcessOrganisationDisassociatedWithOtherPreviouslyDisassociatedOrganisations() {

        //Given
        addDisassociationToRepository(randomUUID(), randomUUID(), randomUUID());
        addCurrentAssociationToRepository(USER_ID, DEFENDANT_ID, ORGANISATION_ID);

        final JsonEnvelope requestEnvelope = generateEvent(DEFENCE_ORGANISATION_DISASSOCIATED_EVENT, USER_ID, DEFENDANT_ID, ORGANISATION_ID, END_DATE);

        //When
        eventListener.processOrganisationDisassociated(requestEnvelope);

        //Then
        verify(repository).save(argumentCaptor.capture());
        final DefenceAssociationDefendant entity = argumentCaptor.getValue();
        assertEquals(DEFENDANT_ID, entity.getDefendantId());
        final DefenceAssociation defenceAssociation = entity.getDefenceAssociations().stream().findFirst().get();
        assertEquals(ORGANISATION_ID, defenceAssociation.getOrgId());
        assertEquals(USER_ID, defenceAssociation.getUserId());
        assertNotNull(defenceAssociation.getEndDate());
    }


    private JsonEnvelope generateEvent(final String name, final UUID userId, final UUID defendantId, final UUID organisationId, final String dateType) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(name)
                .withUserId(userId.toString());
        final JsonObject payload = createObjectBuilder()
                .add("userId", userId.toString())
                .add("defendantId", defendantId.toString())
                .add("organisationId", organisationId.toString())
                .add(dateType, now(UTC_ZONE_ID).toString())
                .build();
        return JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);
    }

    private void addCurrentAssociationToRepository(final UUID userId, final UUID defendantId, final UUID orgId) {
        addAssociationToRepository(userId, defendantId, orgId, true);
    }


    private void addDisassociationToRepository(final UUID userId, final UUID defendantId, final UUID orgId) {
        addAssociationToRepository(userId, defendantId, orgId, false);
    }

    private void addAssociationToRepository(final UUID userId, final UUID defendantId, final UUID orgId, final boolean current) {
        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(defendantId);

        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setUserId(userId);
        defenceAssociation.setDefenceAssociationDefendant(defenceAssociationDefendant);
        defenceAssociation.setOrgId(orgId);
        if (current) {
            defenceAssociation.setEndDate(null);
        } else {
            defenceAssociation.setEndDate(now(UTC_ZONE_ID));
        }

        final HashSet<DefenceAssociation> defenceAssociations = new HashSet<>();
        defenceAssociations.add(defenceAssociation);

        defenceAssociationDefendant.setDefenceAssociations(defenceAssociations);

        when(repository.findByDefendantId(defendantId)).thenReturn(defenceAssociationDefendant);
    }

}