package uk.gov.moj.cpp.progression.query.view.service;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.progression.courts.SharedCourtDocumentsLinksForApplication;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedAllCourtDocumentsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedAllCourtDocumentsRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAllCourtDocumentsServiceTest {

    @Mock
    private SharedAllCourtDocumentsRepository sharedAllCourtDocumentsRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SharedAllCourtDocumentsService sharedAllCourtDocumentsService;

    @Test
    void shouldGetSharedAllCourtDocumentsForTrialHearing() {
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(Envelope.metadataBuilder().
                withId(UUID.randomUUID()).
                withUserId(userId.toString())
                .withName("test"), Json.createObjectBuilder().build());
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final String caseUrn = string(8).next();
        final List<UUID> userGroups = singletonList(UUID.randomUUID());
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final String defendantFirstName1 = string(12).next();
        final String defendantLastName1 = string(12).next();
        final String defendantFirstName2 = string(12).next();
        final String defendantLastName2 = string(12).next();
        final List<Defendant> defendants = asList(Defendant.defendant()
                .withId(randomUUID())
                .withMasterDefendantId(defendantId1)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                        Person.person().withFirstName(defendantFirstName1).withLastName(defendantLastName1).build()).build())
                        .build(),
                Defendant.defendant()
                        .withId(randomUUID())
                        .withMasterDefendantId(defendantId2)
                        .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person().withFirstName(defendantFirstName2).withLastName(defendantLastName2).build()).build())
                        .build());

        when(userService.getUserGroupIdsByUserId(envelope)).thenReturn(userGroups);
        when(sharedAllCourtDocumentsRepository.findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(caseId, hearingId, defendantId1, userGroups, userId)).thenReturn(Collections.emptyList());
        when(sharedAllCourtDocumentsRepository.findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(caseId, hearingId, defendantId2, userGroups, userId)).thenReturn(singletonList(new SharedAllCourtDocumentsEntity()));

        final List<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinks = sharedAllCourtDocumentsService.getSharedAllCourtDocumentsForTrialHearing(envelope, caseId, caseUrn, defendants, hearingId);
        assertThat(sharedCourtDocumentsLinks.size(), is(1));
        assertThat(sharedCourtDocumentsLinks.get(0).getCaseId(), is(caseId));
        assertThat(sharedCourtDocumentsLinks.get(0).getCaseUrn(), is(caseUrn));
        assertThat(sharedCourtDocumentsLinks.get(0).getDefendantId(), is(defendantId2));
        assertThat(sharedCourtDocumentsLinks.get(0).getDefendantName(), is(defendantFirstName2 + " " + defendantLastName2));

    }

    @Test
    void shouldGetSharedAllCourtDocuments() {
        final UUID caseId = UUID.randomUUID();
        final String caseUrn = string(8).next();
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final String defendantFirstName1 = string(12).next();
        final String defendantLastName1 = string(12).next();
        final String defendantFirstName2 = string(12).next();
        final String defendantLastName2 = string(12).next();
        final List<Defendant> defendants = asList(Defendant.defendant()
                        .withId(defendantId1)
                        .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person().withFirstName(defendantFirstName1).withLastName(defendantLastName1).build()).build())
                        .build(),
                Defendant.defendant()
                        .withId(defendantId2)
                        .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person().withFirstName(defendantFirstName2).withLastName(defendantLastName2).build()).build())
                        .build());


        final List<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinks = sharedAllCourtDocumentsService.getSharedAllCourtDocuments(caseId, caseUrn, defendants);
        assertThat(sharedCourtDocumentsLinks.size(), is(2));
        assertThat(sharedCourtDocumentsLinks.get(0).getCaseId(), is(caseId));
        assertThat(sharedCourtDocumentsLinks.get(0).getCaseUrn(), is(caseUrn));
        assertThat(sharedCourtDocumentsLinks.get(0).getDefendantId(), is(defendantId1));
        assertThat(sharedCourtDocumentsLinks.get(0).getDefendantName(), is(defendantFirstName1 + " " + defendantLastName1));

        assertThat(sharedCourtDocumentsLinks.get(1).getCaseId(), is(caseId));
        assertThat(sharedCourtDocumentsLinks.get(1).getCaseUrn(), is(caseUrn));
        assertThat(sharedCourtDocumentsLinks.get(1).getDefendantId(), is(defendantId2));
        assertThat(sharedCourtDocumentsLinks.get(1).getDefendantName(), is(defendantFirstName2 + " " + defendantLastName2));

    }
}
