package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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


        assertThat("User groups argument mismatch", USER_GROUPS_ORGANISATION_DETAILS, is(jsonEnvelopeArgumentCaptor.getValue().metadata().name()));
        assertThat("Organisation name is not matched", ORGANISATION_NAME, is(defenceOrganisationVO.getName()));
        assertThat("Email is not matched", EMAIL, is(defenceOrganisationVO.getEmail()));
        assertThat("Phone is not matched", PHONE_NUMBER, is(defenceOrganisationVO.getPhoneNumber()));
        assertThat("Postcode is not matched", POSTCODE, is(defenceOrganisationVO.getPostcode()));
        assertThat("Address line1 is not matched", ADDRESS_LINE1, is(defenceOrganisationVO.getAddressLine1()));
        assertThat("Address line2 is not matched", ADDRESS_LINE2, is(defenceOrganisationVO.getAddressLine2()));
        assertThat("Address line3 is not matched", ADDRESS_LINE3, is(defenceOrganisationVO.getAddressLine3()));
        assertThat("Address line4 is not matched", ADDRESS_LINE4, is(defenceOrganisationVO.getAddressLine4()));
    }

    @Test
    public void getDefenceOrganisationDetailsWithNullAddressLinesAndNullPhoneNumber() {
        jsonObject = buildJsonObjectWithNullAddressAndNullPhoneNumber();
        final DefenceOrganisationVO defenceOrganisationVO = getDefenceOrganisationVO();

        assertThat("User groups argument mismatch", USER_GROUPS_ORGANISATION_DETAILS, is(jsonEnvelopeArgumentCaptor.getValue().metadata().name()));
        assertThat("Organisation name is not matched", ORGANISATION_NAME, is(defenceOrganisationVO.getName()));
        assertThat("Email is not matched", EMAIL, is(defenceOrganisationVO.getEmail()));
        assertThat("Postcode is not matched", POSTCODE, is(defenceOrganisationVO.getPostcode()));
        assertThat("Phone is not null", defenceOrganisationVO.getPhoneNumber(), nullValue());
        assertThat("Address line1 is not null", defenceOrganisationVO.getAddressLine1(), nullValue());
        assertThat("Address line2 is not null", defenceOrganisationVO.getAddressLine2(), nullValue());
        assertThat("Address line3 is not null", defenceOrganisationVO.getAddressLine3(), nullValue());
        assertThat("Address line4 is not null", defenceOrganisationVO.getAddressLine4(), nullValue());
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

        final Map<String,String> emails = usersGroupService.getEmailsForOrganisationIds(requestMessage, orgIds);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        assertThat(envelopeArgumentCaptor.getValue(), notNullValue());

        final String requestIds = envelopeArgumentCaptor.getValue().payload().getString("ids");
        assertThat(requestIds, notNullValue());
        assertThat(requestIds, containsString(id1));
        assertThat(requestIds, containsString(id2));

        assertThat(emails, notNullValue());
        assertThat(emails.size(), is(2));
        assertThat(emails.values(), hasItem("joe@example.com"));
        assertThat(emails.values(), hasItem("bee@example.com"));
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

        final Map<String,String> emails = usersGroupService.getEmailsForOrganisationIds(requestMessage, orgIds);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        assertThat(envelopeArgumentCaptor.getValue(), notNullValue());

        final String requestIds = envelopeArgumentCaptor.getValue().payload().getString("ids");
        assertThat(requestIds, notNullValue());
        assertThat(requestIds, containsString(id1));
        assertThat(requestIds, containsString(id2));
        assertThat(emails, notNullValue());
        assertThat(emails.size(), is(0));
    }

    @Test
    public void shouldGetGroupsWithOrganisation() {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUIDAndName(), createObjectBuilder().build());

        prepareResponseMock();

        usersGroupService.getGroupsWithOrganisation(jsonEnvelope);
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        assertThat(envelopeArgumentCaptor.getValue(), notNullValue());
    }

    private void prepareResponseMock() {
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
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

        assertThat("Defence Organisation VO is null", defenceOrganisationVOResults.isPresent());

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
                                .add("organisationId", "1fc69990-bf59-4c4a-9489-d766b9abde9b")
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
