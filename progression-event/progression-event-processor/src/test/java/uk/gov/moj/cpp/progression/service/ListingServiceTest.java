package uk.gov.moj.cpp.progression.service;

import static java.time.LocalDate.parse;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;
import static uk.gov.moj.cpp.progression.service.ListingService.LISTING_COMMAND_SEND_CASE_FOR_LISTING;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredHearingType;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class ListingServiceTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private Sender sender;
    @InjectMocks
    private ListingService listingService;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void shouldSendCaseForListing() throws IOException {
        //given

        PodamFactory factory = new PodamFactoryImpl();
        SendCaseForListing sendCaseForListing = factory.manufacturePojoWithFullData(SendCaseForListing.class);

        final JsonObject sendCaseForListingJson = Json.createObjectBuilder().build();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonEnvelope envelopeSendCaseForListing = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName(LISTING_COMMAND_SEND_CASE_FOR_LISTING).build(),
                sendCaseForListingJson);


        when(objectToJsonObjectConverter.convert(any(SendCaseForListing.class)))
                .thenReturn(sendCaseForListingJson);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        listingService.sendCaseForListing(jsonEnvelope,sendCaseForListing);

        when(enveloper.withMetadataFrom(envelopeReferral, LISTING_COMMAND_SEND_CASE_FOR_LISTING)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(any(JsonObject.class))).thenReturn(envelopeSendCaseForListing);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_CASE_FOR_LISTING));


        verifyNoMoreInteractions(sender);
    }





}
