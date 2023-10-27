package uk.gov.moj.cpp.progression.service;

import static java.util.Locale.UK;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.listing.courts.PublishCourtListType.DRAFT;
import static uk.gov.justice.listing.courts.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.courts.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.courts.PublishCourtListType.WARN;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddress.publishCourtListAddressBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddressee.publishCourtListAddresseeBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload.publishCourtListPayloadBuilder;

import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.listing.courts.PublishCourtListType;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@SuppressWarnings("JUnitMalformedDeclaration")
@RunWith(DataProviderRunner.class)
public class PublishCourtListNotificationServiceTest {

    @DataProvider
    public static Object[][] weekCommencingCourtListType() {
        return new Object[][]{
                {WARN},
                {FIRM}
        };
    }

    @DataProvider
    public static Object[][] fixedDateCourtListType() {
        return new Object[][]{
                {DRAFT},
                {FINAL}
        };
    }

    @DataProvider
    public static Object[][] allCourtListType() {
        return new Object[][]{
                {DRAFT},
                {FINAL},
                {WARN},
                {FIRM}
        };
    }

    @Mock
    private DocumentGeneratorService documentGeneratorService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationParameters applicationParameters;
    @Mock
    private MaterialUrlGenerator materialUrlGenerator;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<List<EmailChannel>> emailChannelCaptor;
    @InjectMocks
    private PublishCourtListNotificationService underTest;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    private final Faker faker = new Faker(UK);
    private final Clock clock = new StoppedClock(ZonedDateTime.now());

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    @UseDataProvider("allCourtListType")
    public void shouldSendEmailNotificationIfEmailAddressIsAvailableForAllPublishListNotificationTypes(final PublishCourtListType publishCourtListType) {
        final JsonEnvelope envelope = prepareEnvelope();
        final String documentTemplateName = faker.funnyName().name();
        final UUID materialId = randomUUID();
        final String materialUrl = faker.internet().url();
        final String emailTemplateId = randomUUID().toString();

        final PublishCourtListPayload publishCourtListPayload = preparePublishCourtListPayloadWithEmailAndPostalAddress(publishCourtListType);
        doNothing().when(documentGeneratorService).generateNonNowDocument(eq(envelope), any(JsonObject.class), eq(documentTemplateName), any(), anyString());
        given(materialUrlGenerator.pdfFileStreamUrlFor(any())).willReturn(materialUrl);
        given(applicationParameters.getNotifyHearingTemplateId()).willReturn(emailTemplateId);

        underTest.sendNotification(envelope, publishCourtListPayload, documentTemplateName);

        verify(notificationService).sendEmail(eq(envelope), any(UUID.class), isNull(UUID.class), isNull(UUID.class), any(), emailChannelCaptor.capture());
        verify(notificationService, never()).sendLetter(any(JsonEnvelope.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean());

        final EmailChannel actualEmailChannel = emailChannelCaptor.getValue().get(0);
        assertThat(actualEmailChannel.getMaterialUrl(), is(materialUrl));
        assertThat(actualEmailChannel.getTemplateId(), is(fromString(emailTemplateId)));
        assertThat(actualEmailChannel.getSendToAddress(), is(publishCourtListPayload.getAddressee().getEmail()));
        assertThat(actualEmailChannel.getPersonalisation().getAdditionalProperties(), hasEntry("hearing_notification_date", LocalDates.to(clock.now().toLocalDate())));
    }

    @Test
    @UseDataProvider("fixedDateCourtListType")
    public void shouldNotSendPostalNotificationIfEmailAddressIsNotAvailableForFixedDatePublishListNotification(final PublishCourtListType publishCourtListType) {
        final JsonEnvelope envelope = prepareEnvelope();
        final String documentTemplateName = faker.funnyName().name();
        final UUID materialId = randomUUID();
        final String materialUrl = faker.internet().url();
        final String emailTemplateId = randomUUID().toString();

        final PublishCourtListPayload publishCourtListPayload = preparePublishCourtListPayloadWithNoEmailButWithPostalAddress(publishCourtListType);
        doNothing().when(documentGeneratorService).generateNonNowDocument(eq(envelope), any(JsonObject.class), eq(documentTemplateName), any(), anyString());
        given(materialUrlGenerator.pdfFileStreamUrlFor(materialId)).willReturn(materialUrl);
        given(applicationParameters.getNotifyHearingTemplateId()).willReturn(emailTemplateId);

        underTest.sendNotification(envelope, publishCourtListPayload, documentTemplateName);

        verify(notificationService, never()).sendLetter(any(JsonEnvelope.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean());
        verify(notificationService, never()).sendEmail(any(JsonEnvelope.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), anyListOf(EmailChannel.class));
    }

    @Test
    @UseDataProvider("weekCommencingCourtListType")
    public void shouldSendPostalNotificationIfEmailAddressIsNotAvailableForWeekCommencingPublishListNotification(final PublishCourtListType publishCourtListType) {
        final JsonEnvelope envelope = prepareEnvelope();
        final String documentTemplateName = faker.funnyName().name();
        final UUID materialId = randomUUID();
        final String materialUrl = faker.internet().url();
        final String emailTemplateId = randomUUID().toString();

        final PublishCourtListPayload publishCourtListPayload = preparePublishCourtListPayloadWithNoEmailButWithPostalAddress(publishCourtListType);
        doNothing().when(documentGeneratorService).generateNonNowDocument(eq(envelope), any(JsonObject.class), eq(documentTemplateName), any(), anyString());
        given(materialUrlGenerator.pdfFileStreamUrlFor(any())).willReturn(materialUrl);
        given(applicationParameters.getNotifyHearingTemplateId()).willReturn(emailTemplateId);

        underTest.sendNotification(envelope, publishCourtListPayload, documentTemplateName);

        verify(notificationService, never()).sendEmail(any(JsonEnvelope.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), anyListOf(EmailChannel.class));
        verify(notificationService).sendLetter(eq(envelope), any(UUID.class), isNull(UUID.class), isNull(UUID.class), any(), eq(true));
    }

    private JsonEnvelope prepareEnvelope() {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.event.court-list-published").build(),
                createObjectBuilder().build());
    }

    private PublishCourtListPayload preparePublishCourtListPayloadWithEmailAndPostalAddress(final PublishCourtListType publishCourtListType) {
        return publishCourtListPayloadBuilder()
                .withIssueDate(LocalDates.to(clock.now().toLocalDate()))
                .withPublishCourtListType(publishCourtListType)
                .withAddressee(publishCourtListAddresseeBuilder()
                        .withEmail(faker.internet().emailAddress())
                        .withAddress(publishCourtListAddressBuilder()
                                .withLine1(faker.address().streetAddressNumber())
                                .withLine2(faker.address().streetName())
                                .withPostCode(faker.address().zipCode())
                                .build())
                        .build())
                .build();
    }

    private PublishCourtListPayload preparePublishCourtListPayloadWithNoEmailButWithPostalAddress(final PublishCourtListType publishCourtListType) {
        return publishCourtListPayloadBuilder()
                .withIssueDate(LocalDates.to(clock.now().toLocalDate()))
                .withPublishCourtListType(publishCourtListType)
                .withAddressee(publishCourtListAddresseeBuilder()
                        .withAddress(publishCourtListAddressBuilder()
                                .withLine1(faker.address().streetAddressNumber())
                                .withLine2(faker.address().streetName())
                                .withPostCode(faker.address().zipCode())
                                .build())
                        .build())
                .build();
    }

}