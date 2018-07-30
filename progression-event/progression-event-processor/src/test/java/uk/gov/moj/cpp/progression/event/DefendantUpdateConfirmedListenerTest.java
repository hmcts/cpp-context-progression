package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.event.DefendantUpdateConfirmedListener.PUBLIC_CASE_DEFENDANT_CHANGED;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class DefendantUpdateConfirmedListenerTest {
    private static final String INTERPRETER_LANGUAGE = "French";

    private static final String INTERPRETER_NAME = "Mark Thompson";

    private static final String BAIL_STATUS = "Bailed";

    private static final String POST_CODE = "NW10";

    private static final String ADDRESS_LINE_4 = "London";

    private static final String ADDRESS_LINE_3 = "Keneth Close";

    private static final String ADDRESS_LINE_2 = "Inland Avenue";

    private static final String ADDRESS_LINE_1 = "124 Cambridge st";

    private static final String EMAIL = "david@moj.uk";

    private static final String FAX = "044512263";

    private static final String MOBILE = "078445455";

    private static final String WORK_TELEPHONE = "011799686";

    private static final String HOME_TELEPHONE = "0117885996";

    private static final String GENDER = "Male";

    private static final String NATIONALITY = "British";

    private static final String DATE_OF_BIRTH = "25-10-1981";

    private static final String LAST_NAME = "Ken";

    private static final String FIRST_NAME = "David";

    private static final String TITLE = "Mr";

    @InjectMocks
    private DefendantUpdateConfirmedListener listener;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID PERSON_ID = randomUUID();
    private static final LocalDate CUSTODY_TIME_LIMIT_DATE = LocalDate.now();
    private static final String DEFENCE_ORGANISATION = STRING.next();
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_POST_CODE = "postCode";
    private static final String FIELD_ADDRESS4 = "address4";
    private static final String FIELD_ADDRESS3 = "address3";
    private static final String FIELD_ADDRESS2 = "address2";
    private static final String FIELD_ADDRESS1 = "address1";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_FAX = "fax";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_WORK_TELEPHONE = "workTelephone";
    private static final String FIELD_HOME_TELEPHONE = "homeTelephone";
    private static final String FIELD_GENDER = "gender";
    private static final String FIELD_NATIONALITY = "nationality";
    private static final String FIELD_DATE_OF_BIRTH = "dateOfBirth";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_INTERPRETER = "interpreter";
    private static final String FIELD_DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String FIELD_CUSTODY_TIME_LIMIT_DATE = "custodyTimeLimitDate";
    private static final String FIELD_BAIL_STATUS = "bailStatus";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_PERSON = "person";
    private static final String FIELD_ID = "id";
    private static final String FIELD_INTERPRETER_NEEDED = "needed";
    private static final String FIELD_DEFENDANTS = "defendants";

    @Test
    public void testPublishCaseDefendantChanged() throws IOException {
        this.listener.publishCaseDefendantChanged(getEventJsonForDefendantUpdateConfirmed());
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        assrtPublicMessageAllFields();
    }

    private void assrtPublicMessageAllFields() {
        assertThat(this.envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName(PUBLIC_CASE_DEFENDANT_CHANGED),
                        payloadIsJson(allOf(
                                        withJsonPath(format("$.%s", FIELD_CASE_ID),
                                                        equalTo(CASE_ID.toString())),
                                        withJsonPath(format("$.%s[0].%s", FIELD_DEFENDANTS,
                                                        FIELD_ID),
                                                        equalTo(DEFENDANT_ID.toString())),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ID),
                                                        equalTo(PERSON_ID.toString())),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_TITLE), equalTo(TITLE)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_FIRST_NAME),
                                                        equalTo(FIRST_NAME)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_LAST_NAME),
                                                        equalTo(LAST_NAME)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_DATE_OF_BIRTH),
                                                        equalTo(DATE_OF_BIRTH)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_NATIONALITY),
                                                        equalTo(NATIONALITY)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_GENDER),
                                                        equalTo(GENDER)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_HOME_TELEPHONE),
                                                        equalTo(HOME_TELEPHONE)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_WORK_TELEPHONE),
                                                        equalTo(WORK_TELEPHONE)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_MOBILE),
                                                        equalTo(MOBILE)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_FAX),
                                                        equalTo(FAX.toString())),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_EMAIL), equalTo(EMAIL)),

                                        withJsonPath(format("$.%s[0].%s.%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ADDRESS,
                                                        FIELD_ADDRESS1), equalTo(ADDRESS_LINE_1)),
                                        withJsonPath(format("$.%s[0].%s.%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ADDRESS,
                                                        FIELD_ADDRESS2), equalTo(ADDRESS_LINE_2)),
                                        withJsonPath(format("$.%s[0].%s.%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ADDRESS,
                                                        FIELD_ADDRESS3), equalTo(ADDRESS_LINE_3)),
                                        withJsonPath(format("$.%s[0].%s.%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ADDRESS,
                                                        FIELD_ADDRESS4), equalTo(ADDRESS_LINE_4)),
                                        withJsonPath(format("$.%s[0].%s.%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_PERSON, FIELD_ADDRESS,
                                                        FIELD_POST_CODE), equalTo(POST_CODE)),
                                        withJsonPath(format("$.%s[0].%s", FIELD_DEFENDANTS,
                                                        FIELD_BAIL_STATUS), equalTo(BAIL_STATUS)),
                                        withJsonPath(format("$.%s[0].%s", FIELD_DEFENDANTS,
                                                        FIELD_CUSTODY_TIME_LIMIT_DATE),
                                                        equalTo(CUSTODY_TIME_LIMIT_DATE
                                                                        .toString())),
                                        withJsonPath(format("$.%s[0].%s", FIELD_DEFENDANTS,
                                                        FIELD_DEFENCE_ORGANISATION),
                                                        equalTo(DEFENCE_ORGANISATION)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_INTERPRETER, FIELD_NAME),
                                                        equalTo(INTERPRETER_NAME)),
                                        withJsonPath(format("$.%s[0].%s.%s", FIELD_DEFENDANTS,
                                                        FIELD_INTERPRETER, FIELD_LANGUAGE),
                                                        equalTo(INTERPRETER_LANGUAGE))))

        ));
    }

    public JsonEnvelope getEventJsonForDefendantUpdateConfirmed() throws IOException {
        final Metadata metadata = metadataWithDefaults().build();
        return new DefaultJsonEnvelope(metadata, createDefendantUpdateConfirmedEvent());
    }

    private JsonObject createDefendantUpdateConfirmedEvent() {
        return Json.createObjectBuilder().add(FIELD_CASE_ID, CASE_ID.toString())
                        .add(FIELD_DEFENDANT_ID, DEFENDANT_ID.toString())
                        .add(FIELD_PERSON, Json.createObjectBuilder()
                                        .add(FIELD_ID, PERSON_ID.toString()).add(FIELD_TITLE, TITLE)
                                        .add(FIELD_FIRST_NAME, FIRST_NAME)
                                        .add(FIELD_LAST_NAME, LAST_NAME)
                                        .add(FIELD_DATE_OF_BIRTH, DATE_OF_BIRTH)
                                        .add(FIELD_NATIONALITY, NATIONALITY)

                                        .add(FIELD_GENDER, GENDER)
                                        .add(FIELD_HOME_TELEPHONE, HOME_TELEPHONE)
                                        .add(FIELD_WORK_TELEPHONE, WORK_TELEPHONE)
                                        .add(FIELD_MOBILE, MOBILE).add(FIELD_FAX, FAX)
                                        .add(FIELD_EMAIL, EMAIL)
                                        .add(FIELD_ADDRESS, Json.createObjectBuilder()
                                                        .add(FIELD_ADDRESS1, ADDRESS_LINE_1)
                                                        .add(FIELD_ADDRESS2, ADDRESS_LINE_2)
                                                        .add(FIELD_ADDRESS3, ADDRESS_LINE_3)
                                                        .add(FIELD_ADDRESS4, ADDRESS_LINE_4)
                                                        .add(FIELD_POST_CODE, POST_CODE)))
                        .add(FIELD_BAIL_STATUS, BAIL_STATUS)
                        .add(FIELD_CUSTODY_TIME_LIMIT_DATE, CUSTODY_TIME_LIMIT_DATE.toString())
                        .add(FIELD_DEFENCE_ORGANISATION, DEFENCE_ORGANISATION)
                        .add(FIELD_INTERPRETER,
                                        Json.createObjectBuilder()
                                                        .add(FIELD_INTERPRETER_NEEDED, true)
                                                        .add(FIELD_NAME, INTERPRETER_NAME)
                                                        .add(FIELD_LANGUAGE, INTERPRETER_LANGUAGE))
                        .build();
    }

}
