package uk.gov.moj.cpp.progression.processor.summons;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

        summonsService.getProsecutor(envelope, prosecutionCaseIdentifier);

        verifyNoInteractions(referenceDataService);

    }

}
