package uk.gov.moj.cpp.progression.cotr.cotrHelper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;

import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.progression.courts.AddFurtherInfoDefenceCotrCommand;
import uk.gov.justice.progression.courts.AddFurtherInfoProsecutionCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.progression.courts.UpdateReviewNotes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class CotrHelper {
    public static Response createCotr(final CreateCotr createCotr, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(createCotr);
        return postCommandWithUserId(getWriteUrl("/cotr"),
                "application/vnd.progression.create-cotr+json",
                jsonString, userId);
    }

    public static Response serveProsecutionCotr(final ServeProsecutionCotr serveProsecutionCotr, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(serveProsecutionCotr);
        return postCommandWithUserId(getWriteUrl("/cotr/" + serveProsecutionCotr.getCotrId()), "application/vnd.progression.serve-prosecution-cotr+json", jsonString, userId);
    }

    public static Response serveDefendantCotr(final ServeDefendantCotr serveProsecutionCotr, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(serveProsecutionCotr);
        return postCommandWithUserId(getWriteUrl("/cotr/" + serveProsecutionCotr.getCotrId()), "application/vnd.progression.serve-defendant-cotr+json", jsonString, userId);
    }

    public static Response changeCotrDefendants(final ChangeDefendantsCotr changeDefendantsCotr, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(changeDefendantsCotr);
        return postCommandWithUserId(getWriteUrl("/cotr/" + changeDefendantsCotr.getCotrId()), "application/vnd.progression.change-defendants-cotr+json", jsonString, userId);
    }

    public static Response addFurtherInfoForProsecutionCotr(final AddFurtherInfoProsecutionCotr addFurtherInfoProsecutionCotr, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(addFurtherInfoProsecutionCotr);
        return postCommandWithUserId(getWriteUrl("/cotr/" + addFurtherInfoProsecutionCotr.getCotrId()), "application/vnd.progression.add-further-info-prosecution-cotr+json", jsonString, userId);
    }

    public static Response addFurtherInfoDefenceCotr(final AddFurtherInfoDefenceCotrCommand addFurtherInfoDefenceCotr, final String userId, String cotrId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(addFurtherInfoDefenceCotr);
        return postCommandWithUserId(getWriteUrl("/cotr/" + cotrId), "application/vnd.progression.add-further-info-defence-cotr+json", jsonString, userId);
    }

    public static Response updateReviewNotes(final UpdateReviewNotes updateReviewNotes, final String userId) throws IOException {
        final String jsonString = Utilities.JsonUtil.toJsonString(updateReviewNotes);
        return postCommandWithUserId(getWriteUrl("/cotr/" + updateReviewNotes.getCotrId()), "application/vnd.progression.update-review-notes+json", jsonString, userId);
    }


    public static JsonObject verifyCotrAndGetCotr(final JmsMessageConsumerClient consumer, final String inCotrId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumer);
        assertTrue(message.isPresent());
        final String cotrId = message.get().getString("cotrId");
        assertThat(cotrId, notNullValue());
        assertThat(cotrId, is(inCotrId));
        return message.orElse(null);
    }

    public static void verifyCotrAndGetCotr(final JmsMessageConsumerClient consumer, final String cotrId, final String defendantId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumer);
        assertTrue(message.isPresent());
        final JsonObject eventPayload = message.get();
        assertThat(eventPayload.getString("cotrId"), is(cotrId));
        assertThat(eventPayload.getString("defendantId"), is(defendantId));
    }

    public static void verifyCaseId(final JsonObject message, final String inCaseId) {
        final String caseId = message.getString("caseId");
        assertThat(caseId, notNullValue());
        assertThat(caseId, is(inCaseId));
    }

    public static void verifyServeProsecution(final JsonObject event, final ServeProsecutionCotr serveProsecutionCotr) {
        assertThat(event.getString("hearingId"), is(serveProsecutionCotr.getHearingId().toString()));

    }

    public static void verifyServeDefendant(final JsonObject event, final ServeDefendantCotr serveDefendantCotr) {
        assertThat(event.getString("defendantId"), is(serveDefendantCotr.getDefendantId().toString()));
        assertThat(event.getString("defendantFormData"), is(serveDefendantCotr.getDefendantFormData()));
    }

    public static void verifyInMessagingQueue(JmsMessageConsumerClient messageConsumer) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
        assertTrue(message.isPresent());
    }

    public static void addCaseProsecutor(final String caseId) {
        CaseProsecutorUpdateHelper caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);
        caseProsecutorUpdateHelper.updateCaseProsecutor();
        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.prosecutor", notNullValue())
        };

        pollProsecutionCasesProgressionFor(caseId, caseUpdatedMatchers);
    }

    public static void queryAndVerifyCotrForm(final UUID caseId, final UUID cotrId) {
        pollForResponse(String.format("/prosecutioncases/%s/cotr/%s", caseId, cotrId),
                "application/vnd.progression.query.cotr-form+json",
                randomUUID().toString(),
                withJsonPath("$.cotrForm.caseId", Matchers.is(caseId.toString())),
                withJsonPath("$.cotrForm.id", is(cotrId.toString()))
        );
    }
}
