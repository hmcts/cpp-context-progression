package uk.gov.moj.cpp.progression.query;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantPartialMatchRepository;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class DefendantPartialMatchQueryView {

    private static final String DEFENDANT_NAME = "defendantName";
    private static final String CASE_RECEIVED_DATE = "caseReceivedDate";
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantPartialMatchQueryView.class.getName());

    @Inject
    private DefendantPartialMatchRepository defendantPartialMatchRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Handles("progression.query.partial-match-defendants")
    public JsonEnvelope getDefendantPartialMatches(JsonEnvelope jsonEnvelope) {
        final long page = JsonObjects.getLong(jsonEnvelope.payloadAsJsonObject(), "page").orElse(1L);
        final long pageSize = JsonObjects.getLong(jsonEnvelope.payloadAsJsonObject(), "pageSize").orElse(20L);
        final String sortField = JsonObjects.getString(jsonEnvelope.payloadAsJsonObject(), "sortField").orElse(CASE_RECEIVED_DATE);
        final String sortOrder = JsonObjects.getString(jsonEnvelope.payloadAsJsonObject(), "sortOrder")
                .orElse(StringUtils.equalsIgnoreCase(sortField, CASE_RECEIVED_DATE) ? DESC : ASC);
        LOGGER.info("Request:{}, page:{}, pageSize:{}, sortField:{}, sortOrder:{}", jsonEnvelope.metadata().id(), page, pageSize, sortField, sortOrder);

        final long count = defendantPartialMatchRepository.count();

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("totalMatchedDefendants", count);

        if (isPageGreaterThanMaximumPage(page, pageSize, count)) {
            LOGGER.info("findAll is not called. Current Page greater than maximum page.  Count:{}, page:{}, pageSize:{} ", count, page, pageSize);
        } else {
            final List<DefendantPartialMatchEntity> defendantPartialMatches = findAll((int) page, (int) pageSize, sortField, sortOrder);
            LOGGER.info("Request:{}, dbRecords:{}", jsonEnvelope.metadata().id(), defendantPartialMatches);


            final List<JsonObject> matchedDefendants = defendantPartialMatches.stream()
                    .map(this::mapToMatchedDefendants)
                    .collect(Collectors.toList());
            jsonObjectBuilder.add("matchedDefendants", listToJsonArrayConverter.convert(matchedDefendants));
        }
        return JsonEnvelope.envelopeFrom(
                jsonEnvelope.metadata(),
                jsonObjectBuilder.build());
    }

    private boolean isPageGreaterThanMaximumPage(final long page, final long pageSize, final long count) {
        return count / pageSize + 1 < page;
    }

    private List<DefendantPartialMatchEntity> findAll(final int page, final int pageSize, final String sortField, final String sortOrder) {
        if (StringUtils.equalsIgnoreCase(sortField, DEFENDANT_NAME)) {
            if (StringUtils.equalsIgnoreCase(sortOrder, DESC)) {
                LOGGER.info("findAll is called order by DefendantName with DESC");
                return defendantPartialMatchRepository.findAllOrderByDefendantNameDesc().withPageSize(pageSize).toPage(page - 1).getResultList();
            } else {
                LOGGER.info("findAll is called order by DefendantName with ASC");
                return defendantPartialMatchRepository.findAllOrderByDefendantNameAsc().withPageSize(pageSize).toPage(page - 1).getResultList();
            }
        } else {
            if (StringUtils.equalsIgnoreCase(sortOrder, ASC)) {
                LOGGER.info("findAll is called order by CaseReceivedDate with ASC");
                return defendantPartialMatchRepository.findAllOrderByCaseReceivedDatetimeAsc().withPageSize(pageSize).toPage(page - 1).getResultList();
            } else {
                LOGGER.info("findAll is called order by CaseReceivedDate with DESC");
                return defendantPartialMatchRepository.findAllOrderByCaseReceivedDatetimeDesc().withPageSize(pageSize).toPage(page - 1).getResultList();
            }
        }
    }

    private JsonObject mapToMatchedDefendants(final DefendantPartialMatchEntity defendantPartialMatch) {
        return stringToJsonObjectConverter.convert(defendantPartialMatch.getPayload());
    }
}
