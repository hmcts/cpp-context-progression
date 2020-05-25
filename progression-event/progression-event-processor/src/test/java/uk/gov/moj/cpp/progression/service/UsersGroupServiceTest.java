package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsersGroupServiceTest {

    @InjectMocks
    private UsersGroupService usersGroupService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @SuppressWarnings("rawtypes")
    @Mock
    private Envelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    private static final UUID ORGANISATION_ID = UUID.randomUUID();
    private static final String ORGANISATION_NAME = "organisation name";
    private static final String ADDRESS_LINE1 = "line1";
    private static final String ADDRESS_LINE2 = "line2";
    private static final String ADDRESS_LINE3 = "line3";
    private static final String ADDRESS_LINE4 = "line4";
    private static final String POSTCODE = "SE14 2AB";
    private static final String PHONE_NUMBER = "080012345678";
    private static final String EMAIL = "abc@xyz.com";
    private static final String USER_GROUPS_ORGANISATION_DETAILS = "usersgroups.get-organisation-details";

    @Test
    public void getDefenceOrganisationDetails() {
        jsonObject = buildJsonObject();
        final DefenceOrganisationVO defenceOrganisationVO = getDefenceOrganisationVO();

        Assert.assertEquals("User groups argument mismatch", USER_GROUPS_ORGANISATION_DETAILS, jsonEnvelopeArgumentCaptor.getValue().metadata().name());
        Assert.assertEquals("Organisation name is not matched", ORGANISATION_NAME, defenceOrganisationVO.getName());
        Assert.assertEquals("Email is not matched", EMAIL, defenceOrganisationVO.getEmail());
        Assert.assertEquals("Phone is not matched", PHONE_NUMBER, defenceOrganisationVO.getPhoneNumber());
        Assert.assertEquals("Postcode is not matched", POSTCODE, defenceOrganisationVO.getPostcode());
        Assert.assertEquals("Address line1 is not matched", ADDRESS_LINE1, defenceOrganisationVO.getAddressLine1());
        Assert.assertEquals("Address line2 is not matched", ADDRESS_LINE2, defenceOrganisationVO.getAddressLine2());
        Assert.assertEquals("Address line3 is not matched", ADDRESS_LINE3, defenceOrganisationVO.getAddressLine3());
        Assert.assertEquals("Address line4 is not matched", ADDRESS_LINE4, defenceOrganisationVO.getAddressLine4());
    }

    @Test
    public void getDefenceOrganisationDetailsWithNullAddressLinesAndNullPhoneNumber() {
        jsonObject = buildJsonObjectWithNullAddressAndNullPhoneNumber();
        final DefenceOrganisationVO defenceOrganisationVO = getDefenceOrganisationVO();

        Assert.assertEquals("User groups argument mismatch", USER_GROUPS_ORGANISATION_DETAILS, jsonEnvelopeArgumentCaptor.getValue().metadata().name());
        Assert.assertEquals("Organisation name is not matched", ORGANISATION_NAME, defenceOrganisationVO.getName());
        Assert.assertEquals("Email is not matched", EMAIL, defenceOrganisationVO.getEmail());
        Assert.assertEquals("Postcode is not matched", POSTCODE, defenceOrganisationVO.getPostcode());
        Assert.assertNull("Phone is not null", defenceOrganisationVO.getPhoneNumber());
        Assert.assertNull("Address line1 is not null", defenceOrganisationVO.getAddressLine1());
        Assert.assertNull("Address line2 is not null", defenceOrganisationVO.getAddressLine2());
        Assert.assertNull("Address line3 is not null", defenceOrganisationVO.getAddressLine3());
        Assert.assertNull("Address line4 is not null", defenceOrganisationVO.getAddressLine4());
    }

    @Test
    public void getEmailsForOrganisationIds() {
        final JsonObject requestPayload = createObjectBuilder().add("foo", "bar").build();
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("usersgroups.get-organisations-details-forids"),
                requestPayload);
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();
        final List<String> orgIds = Arrays.asList(id1, id2);

        jsonObject = buildGetOrganisationsDetailsForIds();
        prepareResponseMock();

        final List<String> emails = usersGroupService.getEmailsForOrganisationIds(requestMessage, orgIds);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        Assert.assertNotNull(envelopeArgumentCaptor.getValue());

        final String requestIds = envelopeArgumentCaptor.getValue().payload().getString("ids");
        Assert.assertNotNull(requestIds);
        Assert.assertTrue(requestIds.contains(id1));
        Assert.assertTrue(requestIds.contains(id2));
        Assert.assertNotNull(emails);
        Assert.assertEquals(2, emails.size());
        Assert.assertTrue(emails.contains("joe@example.com"));
        Assert.assertTrue(emails.contains("bee@example.com"));
    }

    @Test
    public void getEmailsForOrganisationIdsWithNullEmails() {
        final JsonObject requestPayload = createObjectBuilder().add("foo", "bar").build();
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("usersgroups.get-organisations-details-forids"),
                requestPayload);
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();
        final List<String> orgIds = Arrays.asList(id1, id2);

        jsonObject = buildGetOrganisationsDetailsForIdsWithNullEmail();
        prepareResponseMock();

        final List<String> emails = usersGroupService.getEmailsForOrganisationIds(requestMessage, orgIds);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        Assert.assertNotNull(envelopeArgumentCaptor.getValue());

        final String requestIds = envelopeArgumentCaptor.getValue().payload().getString("ids");
        Assert.assertNotNull(requestIds);
        Assert.assertTrue(requestIds.contains(id1));
        Assert.assertTrue(requestIds.contains(id2));
        Assert.assertNotNull(emails);
        Assert.assertEquals(0, emails.size());
    }

    private void prepareResponseMock() {
        when(requester.requestAsAdmin(any())).thenReturn(jsonEnvelope);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(envelope.payload()).thenReturn(jsonObject);
    }

    private DefenceOrganisationVO getDefenceOrganisationVO() {
        final List<UUID> causation = new ArrayList<>();
        causation.add(UUID.randomUUID());
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("abc")
                .createdAt(ZonedDateTime.now())
                .withCausation(causation.get(0));

        when(requester.requestAsAdmin(any())).thenReturn(jsonEnvelope);


        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        Optional<DefenceOrganisationVO> defenceOrganisationVOResults = usersGroupService.getDefenceOrganisationDetails(ORGANISATION_ID, metadataBuilder.build());
        verify(requester).requestAsAdmin(jsonEnvelopeArgumentCaptor.capture());

        Assert.assertTrue("Defence Organisation VO is null", defenceOrganisationVOResults.isPresent());

        return defenceOrganisationVOResults.get();
    }

    private JsonObject buildJsonObject() {
        return createObjectBuilder().add("addressPostcode", POSTCODE)
                .add("addressLine1", ADDRESS_LINE1)
                .add("addressLine2", ADDRESS_LINE2)
                .add("addressLine3", ADDRESS_LINE3)
                .add("addressLine4", ADDRESS_LINE4)
                .add("organisationName", ORGANISATION_NAME)
                .add("phoneNumber", PHONE_NUMBER)
                .add("email", EMAIL)
                .build();
    }

    private JsonObject buildJsonObjectWithNullAddressAndNullPhoneNumber() {
        return createObjectBuilder().add("addressPostcode", POSTCODE)
                .add("organisationName", ORGANISATION_NAME)
                .add("email", EMAIL)
                .build();
    }

    private JsonObject buildGetOrganisationsDetailsForIds() {
        return createObjectBuilder()
                .add("organisations", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("organisationId", "1fc69990-bf59-4c4a-9489-d766b9abde9a")
                                .add("organisationType", "LEGAL_ORGANISATION")
                                .add("organisationName", "Bodgit and Scarper LLP")
                                .add("addressLine1", "Legal House")
                                .add("addressLine2", "15 Sewell Street")
                                .add("addressLine3", "Hammersmith")
                                .add("addressLine4", "London")
                                .add("addressPostcode", "SE14 2AB")
                                .add("phoneNumber", "080012345678")
                                .add("email", "joe@example.com")
                                .add("laaContractNumber", "LAA3482374WER")
                        )
                        .add(Json.createObjectBuilder()
                                .add("organisationId", "1fc69990-bf59-4c4a-9489-d766b9abde9a")
                                .add("organisationType", "LEGAL_ORGANISATION")
                                .add("organisationName", "Bodgit and Scarper LLP")
                                .add("addressLine1", "Legal House")
                                .add("addressLine2", "16 Sewell Street")
                                .add("addressLine3", "Hammersmith")
                                .add("addressLine4", "London")
                                .add("addressPostcode", "SE14 2AC")
                                .add("phoneNumber", "080012345667")
                                .add("email", "bee@example.com")
                                .add("laaContractNumber", "LAA3282974WER")
                        ))
                .build();
    }

    private JsonObject buildGetOrganisationsDetailsForIdsWithNullEmail() {
        return createObjectBuilder()
                .add("organisations", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("organisationId", "1fc69990-bf59-4c4a-9489-d766b9abde9a")
                                .add("organisationType", "LEGAL_ORGANISATION")
                                .add("organisationName", "Bodgit and Scarper LLP")
                        )
                        .add(Json.createObjectBuilder()
                                .add("organisationId", "1fc69990-bf59-4c4a-9489-d766b9abde9a")
                                .add("organisationType", "LEGAL_ORGANISATION")
                                .add("organisationName", "Bodgit and Scarper LLP")
                        ))
                .build();
    }
}
