package uk.gov.moj.cpp.progression.transformer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
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
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.time.LocalDate.parse;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

public class SendCaseForListingTransformerTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    final private String postcode = "CR11111";
    final private String prosecutingAuth = "CPS";
    final private UUID prosecutionCaseId = UUID.randomUUID();
    final private UUID defendantId = UUID.randomUUID();
    final private UUID offenceId = UUID.randomUUID();
    final private UUID courtCenterId = UUID.randomUUID();
    final private int estimateMinutes = 15;
    final private String referralDate = "2018-02-15";
    @Mock
    private Sender sender;
    @InjectMocks
    private SendCaseForListingTransformer sendCaseForListingTransformer;
    @Mock
    private ReferenceDataService referenceDataService;
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
    public void shouldTransformToSendCaseForListing() throws IOException {
        //given

        final SjpCourtReferral courtReferral = getCourtReferral();


        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        when(referenceDataService.getHearingType(any(), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtCentre(envelopeReferral, postcode, prosecutingAuth))
                .thenReturn(CourtCentre.courtCentre().withId(courtCenterId).build());
        when(referenceDataService.getReferralReasonById(any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason","reason for referral").build()));

        final SendCaseForListing sendCaseForListing = sendCaseForListingTransformer
                .transform(envelopeReferral, Arrays.asList(getProsecutionCase()), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(sendCaseForListing.getHearings().size(), is(1));
        assertThat(sendCaseForListing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(sendCaseForListing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(sendCaseForListing.getHearings().get(0).getEarliestStartDateTime().toLocalDate().minusDays(14).toString(), is(referralDate));
        assertThat(sendCaseForListing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(sendCaseForListing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(sendCaseForListing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(sendCaseForListing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(sendCaseForListing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));

    }


    @Test
    public void shouldCalculateEarliestHearingDate() throws IOException {
        //When noticeDate and referralDate is same date
        assertThat(SendCaseForListingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-01")), is(parse("2018-01-01").plusDays(28)));
        // When referralDate is greater
        assertThat(SendCaseForListingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-20")), is(parse("2018-01-20").plusDays(14)));
        // When referralDate is greater but not after noticeDate+28
        assertThat(SendCaseForListingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-10")), is(parse("2018-01-01").plusDays(28)));

    }

    private ProsecutionCase getProsecutionCase() {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address().withPostcode(postcode).build()).build()).build())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

    private SjpCourtReferral getCourtReferral() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral().withNoticeDate("2018-01-01").withReferralDate("2018-02-15").build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(Integer.valueOf(15))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(Arrays.asList(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                .withPersonDetails(ReferredPerson.referredPerson()
                                        .withAddress(Address.address().withPostcode(postcode).build()).build()).build())
                        .withOffences(Arrays.asList(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();

        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(Arrays.asList(referredProsecutionCase))
                .withListHearingRequests(Arrays.asList(listHearingRequest)).build();
    }


}
