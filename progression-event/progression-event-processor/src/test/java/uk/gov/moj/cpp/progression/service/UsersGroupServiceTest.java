package uk.gov.moj.cpp.progression.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersGroupServiceTest {

    @InjectMocks
    private UsersGroupService usersGroupService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

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
        final List<UUID> causation =  new ArrayList<>();
        causation.add(UUID.randomUUID());
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("abc")
                .createdAt(ZonedDateTime.now())
                .withCausation(causation.get(0));

        when(requester.requestAsAdmin(any())).thenReturn(jsonEnvelope);

        jsonObject = buildJsonObject();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        Optional<DefenceOrganisationVO> defenceOrganisationVOResults = usersGroupService.getDefenceOrganisationDetails(ORGANISATION_ID, metadataBuilder.build());
        verify(requester).requestAsAdmin(jsonEnvelopeArgumentCaptor.capture());

        Assert.assertTrue("Defence Organisation VO is null", defenceOrganisationVOResults.isPresent());

        final DefenceOrganisationVO defenceOrganisationVO = defenceOrganisationVOResults.get();

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


}
