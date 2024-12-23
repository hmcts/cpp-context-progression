package uk.gov.moj.cpp.progression.query.view.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.view.service.exception.ProgressionServiceException;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery.PROSECUTION_CASE;

public class DefendantService {

    @Inject
    private ProsecutionCaseQuery  prosecutionCaseQuery;


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantService.class);

    public String getDefendantFullName(final Person person) {
        return format("%s %s", person.getFirstName(), person.getLastName());
    }

    public ProsecutionCase getProsecutionCase(final JsonEnvelope query) {

        JsonEnvelope responseEnvelope =  prosecutionCaseQuery.getCase(query);

        LOGGER.info(" Calling {} to get prosecution case for {} ", PROSECUTION_CASE);


        if (Objects.isNull(responseEnvelope.payloadAsJsonObject()) || Objects.isNull(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"))) {
            throw new ProgressionServiceException(format("Failed to get prosecution case from progression"));
        }

        LOGGER.info("Got prosecution - {}", responseEnvelope.payloadAsJsonObject());

     return jsonObjectToObjectConverter.convert(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"), ProsecutionCase.class);

    }
    public List<Defendant> getDefendantList(final JsonEnvelope query) {
        final ProsecutionCase prosecutionCase = getProsecutionCase(query);
        if (isNotEmpty(prosecutionCase.getDefendants())) {
            return prosecutionCase.getDefendants();
        }
        return emptyList();
    }
}
