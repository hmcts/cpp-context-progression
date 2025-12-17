package uk.gov.moj.cpp.progression.query.view.service.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.domain.pojo.FixList;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;


import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.progression.query.view.service.transformer.TransformerUtil.generateFixList;

public class Prompt5Transformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Prompt5Transformer.class);

    private static final String LANGUAGES = "languages";

    @Inject
    ReferenceDataService referenceDataService;

    public Prompt transform(final Prompt prompt) {
        final Prompt.Builder transformedPrompt = Prompt.prompt().withValuesFrom(prompt);
        if (nonNull(prompt.getName()) && "language".equalsIgnoreCase(prompt.getName())) {
            final JsonObject response = referenceDataService.getLanguages();
            transformedPrompt.withFixedList(getLanguage(response));
        }
        return transformedPrompt.build();
    }


    private List<FixList> getLanguage(final JsonObject response) {
        if (response.getJsonArray(LANGUAGES) == null || response.getJsonArray(LANGUAGES).isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No languages found in response from reference data");
            }
            return emptyList();
        }
        return generateFixList(response.getJsonArray(LANGUAGES).getValuesAs(JsonObject.class).stream().map(json-> json.getString("description")).collect(Collectors.toList()),LANGUAGES);
    }
}
