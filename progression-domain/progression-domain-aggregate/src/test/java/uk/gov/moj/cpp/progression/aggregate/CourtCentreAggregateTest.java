package uk.gov.moj.cpp.progression.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterSent;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterRecipient;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCustodyLocation;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CourtCentreAggregateTest {
    private CourtCentreAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new CourtCentreAggregate();
    }

    @Test
    public void shouldRecordCourtRegisterDocumentRequest() {
        final UUID courtCentreId = randomUUID();
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = CourtRegisterDocumentRequest.
                courtRegisterDocumentRequest().
                withCourtCentreId(courtCentreId).
                build();

        final List<Object> eventStream = aggregate.createCourtRegister(courtCentreId, courtRegisterDocumentRequest).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtRegisterRecorded.class)));
    }

    @Test
    public void shouldGenerateCourtRegister() {
        final UUID courtCentreId = randomUUID();

        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = CourtRegisterDocumentRequest.
                courtRegisterDocumentRequest().
                withCourtCentreId(courtCentreId).
                build();

        final List<Object> eventStream = aggregate.generateDocument(Collections.singletonList(courtRegisterDocumentRequest), false).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtRegisterGenerated.class)));
    }

    @Test
    public void shouldGeneratePrisonCourtRegister() {
        final UUID courtCentreId = randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().withName("Test").withDateOfBirth("02/07/1984").build())
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().withCourtHouse("Leamington Avenue").withLjaName("London").build())
                .withRecipients(Lists.newArrayList(PrisonCourtRegisterRecipient.prisonCourtRegisterRecipient().withEmailAddress1("test@test.com").withRecipientName("john smith").build()))
                .withCustodyLocation(PrisonCourtRegisterCustodyLocation.prisonCourtRegisterCustodyLocation().withName("john smith").build())
                .build();

        final List<Object> eventStream = aggregate.createPrisonCourtRegister(courtCentreId, prisonCourtRegisterDocumentRequest).collect(toList());
        assertThat(eventStream.size(), is(1));
        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = (PrisonCourtRegisterRecorded)eventStream.get(0);
        assertThat(prisonCourtRegisterRecorded.getClass(), is(equalTo(PrisonCourtRegisterRecorded.class)));
        assertThat(prisonCourtRegisterRecorded.getCourtCentreId(), is(courtCentreId));
        assertThat(prisonCourtRegisterRecorded.getPrisonCourtRegister(), equalTo(prisonCourtRegisterDocumentRequest));
    }

    @Test
    public void shouldCreatePrisonCourtRegisterWithEmptyRecipient() {
        final UUID courtCentreId = randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().withName("Test").withDateOfBirth("02/07/1984").build())
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().withCourtHouse("Leamington Avenue").withLjaName("London").build())
                .withCustodyLocation(PrisonCourtRegisterCustodyLocation.prisonCourtRegisterCustodyLocation().withName("john smith").build())
                .build();

        final List<Object> eventStream = aggregate.createPrisonCourtRegister(courtCentreId, prisonCourtRegisterDocumentRequest).collect(toList());
        assertThat(eventStream.size(), is(1));
        final PrisonCourtRegisterWithoutRecipientsRecorded prisonCourtRegisterWithoutRecipientsRecorded = (PrisonCourtRegisterWithoutRecipientsRecorded)eventStream.get(0);
        assertThat(prisonCourtRegisterWithoutRecipientsRecorded.getClass(), is(equalTo(PrisonCourtRegisterWithoutRecipientsRecorded.class)));
        assertThat(prisonCourtRegisterWithoutRecipientsRecorded.getCourtCentreId(), is(courtCentreId));
        assertThat(prisonCourtRegisterWithoutRecipientsRecorded.getPrisonCourtRegister(), equalTo(prisonCourtRegisterDocumentRequest));
    }


    @Test
    public void shouldNotifyCourtCentre() {
        final UUID courtCentreId = randomUUID();
        final UUID systemDocGeneratorId = randomUUID();
        final CourtRegisterRecipient recipient = CourtRegisterRecipient.courtRegisterRecipient().withRecipientName("John").build();

        final NotifyCourtRegister notifyCourtRegister = NotifyCourtRegister.notifyCourtRegister()
                .withCourtCentreId(courtCentreId)
                .withSystemDocGeneratorId(systemDocGeneratorId)
                .build();

        aggregate.setCourtRegisterRecipients(Collections.singletonList(recipient));
        final List<Object> eventStream = aggregate.notifyCourt(notifyCourtRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtRegisterNotified.class)));
    }

    @Test
    public void shouldNotifyCourtCentreEmptyRecipient() {
        final UUID courtCentreId = randomUUID();
        final UUID systemDocGeneratorId = randomUUID();

        final NotifyCourtRegister notifyCourtRegister = NotifyCourtRegister.notifyCourtRegister()
                .withCourtCentreId(courtCentreId)
                .withSystemDocGeneratorId(systemDocGeneratorId)
                .build();

        final List<Object> eventStream = aggregate.notifyCourt(notifyCourtRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtRegisterNotificationIgnored.class)));
    }

    @Test
    public void shouldRecordPrisonCourtRegisterDocumentSent() {
        final UUID courtCentreId = randomUUID();
        final List<Object> eventStream = aggregate.recordPrisonCourtRegisterDocumentSent(courtCentreId, RecordPrisonCourtRegisterDocumentSent
                .recordPrisonCourtRegisterDocumentSent().build()).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PrisonCourtRegisterSent.class)));
    }

    @Test
    public void shouldRecordPrisonCourtRegisterGenerated() {
        final UUID courtCentreId = randomUUID();
        final UUID payloadFileId = randomUUID();
        final UUID systemDocumentId = randomUUID();

        aggregate.recordPrisonCourtRegisterDocumentSent(courtCentreId,
                        RecordPrisonCourtRegisterDocumentSent.recordPrisonCourtRegisterDocumentSent()
                                .withCourtCentreId(courtCentreId)
                                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build())
                                .withHearingDate(ZonedDateTime.now())
                                .withRecipients(Arrays.asList(PrisonCourtRegisterRecipient.prisonCourtRegisterRecipient().build()))
                                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                                .withHearingId(randomUUID())
                                .withHearingDate(ZonedDateTime.now())
                                .withPayloadFileId(payloadFileId)
                                .build())
                .collect(Collectors.toList());

        final List<Object> eventStream = aggregate.recordPrisonCourtRegisterGenerated(courtCentreId,
                NotifyPrisonCourtRegister.notifyPrisonCourtRegister()
                        .withSystemDocGeneratorId(systemDocumentId)
                        .withPayloadFileId(payloadFileId).build())
                .collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = (PrisonCourtRegisterGenerated) eventStream.get(0);
        assertThat(prisonCourtRegisterGenerated.getCourtCentreId(), is(courtCentreId));
        assertThat(prisonCourtRegisterGenerated.getFileId(), is(systemDocumentId));
    }


    @Test
    public void shouldRecordPrisonCourtRegisterFailed() {
        final UUID courtCentreId = randomUUID();
        final UUID payloadFileId = randomUUID();
        final String reason = "Test";
        RecordPrisonCourtRegisterFailed recordPrisonCourtRegisterFailed = RecordPrisonCourtRegisterFailed.recordPrisonCourtRegisterFailed()
                .withCourtCentreId(courtCentreId)
                .withPayloadFileId(payloadFileId)
                .withReason("Test")
                .build();
        final List<Object> eventStream = aggregate.recordPrisonCourtRegisterFailed(courtCentreId, recordPrisonCourtRegisterFailed)
                .collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        final PrisonCourtRegisterFailed prisonCourtRegisterFailed = (PrisonCourtRegisterFailed) eventStream.get(0);
        assertThat(prisonCourtRegisterFailed.getCourtCentreId(), is(courtCentreId));
        assertThat(prisonCourtRegisterFailed.getPayloadFileId(), is(payloadFileId));
        assertThat(prisonCourtRegisterFailed.getReason(), is(reason));
    }
}
