package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.query.view.service.DefenceOrganisationService.ORGANISATION_ID;
import static uk.gov.moj.cpp.progression.query.view.service.DefenceOrganisationService.ORGANISATION_NAME;
import static uk.gov.moj.cpp.progression.query.view.service.DefenceOrganisationService.USERSGROUPS_GET_ORGANISATION_DETAILS;

import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceOrganisationServiceTest {

    private static final String EXPECTED_ORG_NAME = "TestOrganisationName";

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestJsonEnvelope;

    @Mock
    private JsonObject responsePayload;

    @Mock
    private DefenceAssociationRepository defenceAssociationRepository;

    @InjectMocks
    private DefenceOrganisationService defenceOrganisationService;
    @Mock
    private DefenceAssociationDefendant defenceAssociationDefendant;


    @Test
    public void shouldReturnEmptyOrganisationWhenNoAssociatedDefenceOrganisation() {

        final UUID defendantId = UUID.randomUUID();
        when(defenceAssociationRepository.findByDefendantId(defendantId)).thenThrow(new NoResultException());

        Optional<Organisation> associatedDefenceOrganisation = defenceOrganisationService.getAssociatedDefenceOrganisation(defendantId);

        assertThat(associatedDefenceOrganisation.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyOrganisationWhenMultipleAssociationsFound() {

        final UUID defendantId = UUID.randomUUID();
        when(defenceAssociationDefendant.getDefenceAssociations()).thenReturn(getDefenceAssociations(defendantId, UUID.randomUUID()));
        when(defenceAssociationRepository.findByDefendantId(defendantId)).thenReturn(defenceAssociationDefendant);

        Optional<Organisation> associatedDefenceOrganisation = defenceOrganisationService.getAssociatedDefenceOrganisation(defendantId);

        assertThat(associatedDefenceOrganisation.isPresent(), is(false));
    }

    @Test
    public void shouldReturnAssociatedDefenceOrganisation() {

        final UUID defendantId = UUID.randomUUID();
        Set<DefenceAssociation> defenceAssociations = getDefenceAssociations(defendantId);
        when(defenceAssociationDefendant.getDefenceAssociations()).thenReturn(defenceAssociations);
        when(defenceAssociationRepository.findByDefendantId(defendantId)).thenReturn(defenceAssociationDefendant);
        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);
        when(responsePayload.getString(ORGANISATION_NAME)).thenReturn(EXPECTED_ORG_NAME);


        Optional<Organisation> associatedDefenceOrganisation = defenceOrganisationService.getAssociatedDefenceOrganisation(defendantId);

        MatcherAssert.assertThat(associatedDefenceOrganisation.get(), notNullValue());
        MatcherAssert.assertThat(associatedDefenceOrganisation.get().getName(), is(EXPECTED_ORG_NAME));

        MatcherAssert.assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString(ORGANISATION_ID), Matchers.is(defenceAssociations.iterator().next().getOrgId().toString()));
        MatcherAssert.assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        MatcherAssert.assertThat(requestJsonEnvelope.getValue().metadata().name(), Matchers.is(USERSGROUPS_GET_ORGANISATION_DETAILS));

    }

    private Set<DefenceAssociation> getDefenceAssociations(final UUID... defendantIds) {
        Set<DefenceAssociation> defenceAssociations = new HashSet<>();
        Arrays.stream(defendantIds).forEach(defendantId -> {
            DefenceAssociation defenceAssociation = new DefenceAssociation();
            defenceAssociation.setId(defendantId);
            defenceAssociation.setOrgId(UUID.randomUUID());
            defenceAssociation.setUserId(UUID.randomUUID());
            defenceAssociations.add(defenceAssociation);
        });
        return defenceAssociations;
    }
}