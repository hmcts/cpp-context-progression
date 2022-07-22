package uk.gov.moj.cpp.progression.service;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.summons.SummonsService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)


public class SummonsServiceTest {

    @Mock
    private RefDataService referenceDataService;

    @InjectMocks
    private SummonsService summonsService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldPopulateProsecutorInformationFromReferenceData() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(UUID.randomUUID())
                .build();

        final JsonObject jsonObject = Json.createObjectBuilder().build();
        when(referenceDataService.getProsecutor(envelope, prosecutionCaseIdentifier.getProsecutionAuthorityId(), requester)).thenReturn(Optional.of(jsonObject));

        summonsService.getProsecutor(envelope, prosecutionCaseIdentifier);

        verify(referenceDataService, times(1)).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier.getProsecutionAuthorityId()), eq(requester));

    }

    @Test
    public void shouldNotPopulateOrganisationProsecutorInformationFromReferenceData() {

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("Org Name")
                .build();

        final JsonObject jsonObject = Json.createObjectBuilder().build();
        when(referenceDataService.getProsecutor(envelope, prosecutionCaseIdentifier.getProsecutionAuthorityId(), requester)).thenReturn(Optional.of(jsonObject));

        summonsService.getProsecutor(envelope, prosecutionCaseIdentifier);

        verifyZeroInteractions(referenceDataService);

    }

}
