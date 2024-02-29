package uk.gov.moj.cpp.progression.processor;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class UnscheduledHearingAllocationNotifiedEventProcessorTest {

    public static final String ENFORCEMENT_EMAIL = "any@email.com";
    public static final String TEMPLATE_ID = "9e2c73eb-8434-459b-ba7d-bfa199b6c960";
    @InjectMocks
    private UnscheduledHearingAllocationNotifiedEventProcessor unscheduledHearingAllocationNotifiedEventProcessor;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RefDataService referenceDataService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ApplicationParameters applicationParameters;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> emailChannelsArgCaptor;

    @Before
    public void setUp(){
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void unscheduledHearingAllocationNotified() {
        final JsonObject payload = getPayload("unscheduled-hearing-allocation-notified-payload.json");
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("progression.event.unscheduled-hearing-allocation-notified"), payload);

        when(applicationParameters.getUnscheduledHearingAllocationEmailTemplateId()).thenReturn(TEMPLATE_ID);
        when(referenceDataService.getEnforcementAreaByLjaCode(eq(envelope), any(), any())).thenReturn(createObjectBuilder()
                .add("email", ENFORCEMENT_EMAIL)
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "1800")
                        .add("name", "East Hampshire Magistrates' Court")
                        .build())
                .build());

        when(referenceDataService.getOrganisationUnitById(any(), eq(envelope), any())).thenReturn(Optional.of(createObjectBuilder()
                .add("lja", "1800")
                .build()));

        unscheduledHearingAllocationNotifiedEventProcessor.unscheduledHearingAllocationNotified(envelope);

        verify(notificationService, times(1)).sendEmail(eq(envelope), any(), eq(fromString("d36ba5f5-f19a-482d-8b71-c06771a41af1")), any(), any(), emailChannelsArgCaptor.capture());
        final List<EmailChannel> emailChannels = emailChannelsArgCaptor.getValue();
        assertThat(emailChannels.size(), is(2));
        assertThat(emailChannels.get(0).getSendToAddress(), is(ENFORCEMENT_EMAIL));
        assertThat(emailChannels.get(0).getTemplateId(), is(fromString(TEMPLATE_ID)));
        final Map<String, Object> additionalProperties = emailChannels.get(0).getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties.get("urn"), is("85GD7524721"));
        assertThat(additionalProperties.get("dateOfHearing").toString(), startsWith("22/02/2021 10:00 AM"));
        assertThat(additionalProperties.get("courtCentre"), is("1800 East Hampshire Magistrates' Court"));
        assertThat(additionalProperties.get("sittingAt"), is("Wimbledon Magistrates' Court"));
        assertThat(additionalProperties.get("caseNumber"), is("d36ba5f5-f19a-482d-8b71-c06771a41af1"));
        assertThat(additionalProperties.get("defendantName"), is("Luke Skywalker"));
    }

    public JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return stringToJsonObjectConverter.convert(response);
    }
}