package uk.gov.moj.cpp.progression.query;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.OpaNotice;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PressListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PublicListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PressListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PublicListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ResultListOpaNoticeRepository;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_VIEW)
public class OpaNoticeQueryView {
    private static final String OPA_NOTICE_KEY  = "opaNotices";

    @Inject
    private PublicListOpaNoticeRepository publicListOpaNoticeRepository;
    @Inject
    private PressListOpaNoticeRepository pressListOpaNoticeRepository;
    @Inject
    private ResultListOpaNoticeRepository resultListOpaNoticeRepository;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.query.public-list-opa-notices")
    public JsonEnvelope getPublicListOpaNoticesView(final JsonEnvelope envelope) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        publicListOpaNoticeRepository.findAll()
                .stream()
                .map(this::buildPublicListOpaNotice)
                .forEach(arrayBuilder::add);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder()
                .add(OPA_NOTICE_KEY, arrayBuilder.build()).build());
    }
    @Handles("progression.query.press-list-opa-notices")
    public JsonEnvelope getPressListOpaNoticesView(final JsonEnvelope envelope) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        pressListOpaNoticeRepository.findAll()
                .stream()
                .map(this::buildPressListOpaNotice)
                .forEach(arrayBuilder::add);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder()
                .add(OPA_NOTICE_KEY, arrayBuilder.build()).build());
    }


    @Handles("progression.query.result-list-opa-notices")
    public JsonEnvelope getResultListOpaNoticesView(final JsonEnvelope envelope) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        resultListOpaNoticeRepository.findAll()
                .stream()
                .map(this::buildResultListOpaNotice)
                .forEach(arrayBuilder::add);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder()
                .add(OPA_NOTICE_KEY, arrayBuilder.build()).build());
    }

    private JsonObject buildPublicListOpaNotice(PublicListOpaNotice publicListOpaNotice) {

        final OpaNotice opaNotice = OpaNotice.opaNotice()
                .withCaseId(publicListOpaNotice.getCaseId())
                .withDefendantId(publicListOpaNotice.getDefendantId())
                .withHearingId(publicListOpaNotice.getHearingId())
                .build();

        return objectToJsonObjectConverter.convert(opaNotice);
    }

    private JsonObject buildPressListOpaNotice(PressListOpaNotice pressListOpaNotice) {
        final OpaNotice opaNotice = OpaNotice.opaNotice()
                .withCaseId(pressListOpaNotice.getCaseId())
                .withDefendantId(pressListOpaNotice.getDefendantId())
                .withHearingId(pressListOpaNotice.getHearingId())
                .build();

        return objectToJsonObjectConverter.convert(opaNotice);
    }

    private JsonObject buildResultListOpaNotice(ResultListOpaNotice resultListOpaNotice) {
        final OpaNotice opaNotice = OpaNotice.opaNotice()
                .withCaseId(resultListOpaNotice.getCaseId())
                .withDefendantId(resultListOpaNotice.getDefendantId())
                .withHearingId(resultListOpaNotice.getHearingId())
                .build();

        return objectToJsonObjectConverter.convert(opaNotice);
    }
}
