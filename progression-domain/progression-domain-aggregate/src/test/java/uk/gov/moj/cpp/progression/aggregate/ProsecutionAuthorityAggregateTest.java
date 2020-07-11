package uk.gov.moj.cpp.progression.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.progression.courts.InformantRegisterGenerated;
import uk.gov.justice.progression.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.InformantRegisterNotified;
import uk.gov.justice.progression.courts.NotifyInformantRegister;
import uk.gov.moj.cpp.progression.aggregate.ProsecutionAuthorityAggregate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionAuthorityAggregateTest {
    @InjectMocks
    private ProsecutionAuthorityAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new ProsecutionAuthorityAggregate();
    }

    @Test
    public void shouldReturnInformantRegisterAdded() {
        final UUID prosecutionAuthId = randomUUID();

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue().build())
                .build();

        final List<Object> eventStream = aggregate.createInformantRegister(prosecutionAuthId, informantRegisterDocumentRequest).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(InformantRegisterRecorded.class)));
    }

    @Test
    public void shouldReturnInformantRegisterGenerated() {
        final UUID prosecutionAuthId = randomUUID();

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue().build())
                .build();

        final List<Object> eventStream = aggregate.generateInformantRegister(Collections.singletonList(informantRegisterDocumentRequest), false).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(InformantRegisterGenerated.class)));
    }

    @Test
    public void shouldReturnInformantRegisterNotified() {
        final UUID fileId = randomUUID();
        final InformantRegisterRecipient recipient = InformantRegisterRecipient.informantRegisterRecipient().withRecipientName("John").build();
        final NotifyInformantRegister notifyInformantRegister = NotifyInformantRegister.notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("template Id")
                .withFileId(fileId)
                .build();

        aggregate.setInformantRegisterRecipients(Collections.singletonList(recipient));
        final List<Object> eventStream = aggregate.notifyProsecutingAuthority(notifyInformantRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(InformantRegisterNotified.class)));
    }

    @Test
    public void shouldReturnInformantRegisterIgnored() {
        final NotifyInformantRegister notifyInformantRegister = NotifyInformantRegister.notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("template Id")
                .withFileId(UUID.randomUUID())
                .build();
        final List<Object> eventStream = aggregate.notifyProsecutingAuthority(notifyInformantRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(InformantRegisterNotificationIgnored.class)));
    }
}
