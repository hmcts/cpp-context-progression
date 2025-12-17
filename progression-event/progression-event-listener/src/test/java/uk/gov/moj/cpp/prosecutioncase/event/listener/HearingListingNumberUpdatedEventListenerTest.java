package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingListingNumberUpdatedEventListenerTest {

    @Mock
    private Envelope<ListingNumberUpdated> envelope;

    @Mock
    private HearingRepository hearingRepository;


    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter =new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private HearingListingNumberUpdatedEventListener hearingListingNumberUpdatedEventListener;

    @Captor
    private ArgumentCaptor<HearingEntity>  hearingEntityArgumentCaptor;

    @Test
    public void shouldUpdateOffencesInProsecutionCaseWithListingNumber(){
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId3)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId4)
                                                        .withListingNumber(13)
                                                        .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();


        ListingNumberUpdated listingNumberUpdated = ListingNumberUpdated.listingNumberUpdated()
                .withHearingId(hearingId)
                .withOffenceListingNumbers(asList(OffenceListingNumbers.offenceListingNumbers()
                        .withListingNumber(10)
                        .withOffenceId(offenceId1)
                        .build(),
                        OffenceListingNumbers.offenceListingNumbers()
                                .withListingNumber(12)
                                .withOffenceId(offenceId3)
                                .build()))
                .build();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());


        when(envelope.payload()).thenReturn(listingNumberUpdated);
        when(hearingRepository.findBy(eq(hearingId))).thenReturn(hearingEntity);
        hearingListingNumberUpdatedEventListener.handleListingNumberUpdatedEvent(envelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        HearingEntity savedEntity = hearingEntityArgumentCaptor.getValue();
        Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(savedEntity.getPayload()),Hearing.class);

        ProsecutionCase prosecutionCase = savedHearing.getProsecutionCases().get(0);

        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(10));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(1).getId(), is(offenceId2));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(1).getListingNumber(), is(11));
        assertThat(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId(), is(offenceId3));
        assertThat(prosecutionCase.getDefendants().get(1).getOffences().get(0).getListingNumber(), is(12));
        assertThat(prosecutionCase.getDefendants().get(1).getOffences().get(1).getId(), is(offenceId4));
        assertThat(prosecutionCase.getDefendants().get(1).getOffences().get(1).getListingNumber(), is(13));

        assertThat(savedHearing.getCourtApplications(), is(nullValue()));
    }

}
