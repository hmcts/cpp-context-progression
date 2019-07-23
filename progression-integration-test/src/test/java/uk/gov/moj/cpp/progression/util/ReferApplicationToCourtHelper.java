package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;


public class ReferApplicationToCourtHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferApplicationToCourtHelper.class);
    private static final String PUBLIC_HEARING_HEARING_ADJOURNED = "public.hearing.adjourned";
    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createProducer();

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-offences-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_OFFENCES_PAYLOAD = "progression.update-offences-for-prosecution-case.json";
    private final MessageConsumer publicEventsConsumerForOffencesUpdated =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.defendant-offences-changed");

    private String adjournHearingRequest;


    public ReferApplicationToCourtHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("listing.command.list-court-hearing");
    }



}
