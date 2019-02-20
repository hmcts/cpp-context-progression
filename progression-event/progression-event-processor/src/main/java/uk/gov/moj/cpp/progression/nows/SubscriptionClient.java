package uk.gov.moj.cpp.progression.nows;

import static javax.json.Json.createObjectBuilder;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.notification.Subscriptions;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SubscriptionClient {

    public static final String QUERY_DATE_FORMAT = "ddMMyyyy";
    public static final String GET_ALL_SUBSCRIPTIONS_BY_REFERENCE_DATE_REQUEST_ID = "hearing.retrieve-subscriptions";
    public static final String AS_OF_DATE_QUERY_PARAMETER = "referenceDate";
    public static final String NOWS_TYPE_ID_QUERY_PARAMETER = "nowTypeId";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public Subscriptions getAll(JsonEnvelope context, UUID nowTypeId, LocalDate localDate) {
        final String strLocalDate = localDate.format(DateTimeFormatter.ofPattern(QUERY_DATE_FORMAT));
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(context, GET_ALL_SUBSCRIPTIONS_BY_REFERENCE_DATE_REQUEST_ID)
                .apply(createObjectBuilder()
                        .add(AS_OF_DATE_QUERY_PARAMETER, strLocalDate)
                        .add(NOWS_TYPE_ID_QUERY_PARAMETER, nowTypeId.toString())
                        .build());

        final JsonEnvelope jsonResultEnvelope = requester.request(requestEnvelope);

        return jsonObjectToObjectConverter.convert(jsonResultEnvelope.payloadAsJsonObject(), Subscriptions.class);
    }
}
