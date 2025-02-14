package uk.gov.moj.cpp.progression.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtApplicationServiceTest {

    @InjectMocks
    private CourtApplicationService courtApplicationService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    private UUID prosecutionAuthorityId;

    @BeforeEach
    public void setUp() {
        prosecutionAuthorityId = UUID.randomUUID();
    }

    @Test
    public void shouldReturnCourtApplicationPartyWhenProsecutingAuthorityDataExists() {
        // Mocking the JSON response from referenceDataService
        JsonObject prosecutorJson = Json.createObjectBuilder()
                .add("fullName", "John Doe")
                .add("nameWelsh", "Ioan Dda")
                .add("contactEmailAddress", "john.doe@test.com")
                .add("address", Json.createObjectBuilder()
                        .add("line1", "123 Main St")
                        .add("line2", "Apt 4B")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .build();

       var address =  Address.address()
               .withAddress1("123 Main St")
               .withAddress2("Apt 4B")
               .withPostcode("SW1A 1AA").build();

        // Mocking the behavior of dependencies
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class), any(Requester.class)))
                .thenReturn(Optional.of(prosecutorJson));

        when(jsonObjectToObjectConverter.convert(prosecutorJson.getJsonObject("address"), Address.class))
                .thenReturn(address);

        // Execute the method
        ProsecutingAuthority result = courtApplicationService.getProsecutingAuthority(prosecutionAuthorityId, jsonEnvelope);

        // Verifying the result
        assertThat(result.getName(), is("John Doe"));
        assertThat(result.getWelshName(), is("Ioan Dda"));
        assertThat(result.getContact().getPrimaryEmail(), is("john.doe@test.com"));

        // Verify address conversion
        assertThat(result.getAddress().getAddress1(), is("123 Main St"));
        assertThat(result.getAddress().getAddress2(), is("Apt 4B"));
        assertThat(result.getAddress().getPostcode(), is("SW1A 1AA"));
    }

    @Test
    public void shouldReturnEmptyCourtApplicationPartyWhenNoProsecutingAuthorityDataExists() {
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class), any(Requester.class)))
                .thenReturn(Optional.empty());

        ProsecutingAuthority result = courtApplicationService.getProsecutingAuthority(prosecutionAuthorityId, jsonEnvelope);
        assertThat(result.getName(), is(nullValue()));
        assertThat(result.getContact(), is(nullValue()));
        assertThat(result.getAddress(), is(nullValue()));
        verify(referenceDataService, times(1)).getProsecutor(jsonEnvelope, prosecutionAuthorityId, requester);
    }
}
