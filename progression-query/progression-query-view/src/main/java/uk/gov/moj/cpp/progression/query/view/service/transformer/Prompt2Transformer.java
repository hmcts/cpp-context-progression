package uk.gov.moj.cpp.progression.query.view.service.transformer;



import uk.gov.moj.cpp.progression.domain.pojo.Prompt;

import java.time.LocalDate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class Prompt2Transformer{

    public Prompt transform(final Prompt prompt) {

        if (isNull(prompt.getDefaultValue()) && isEmpty(prompt.getStartDateValidation())) {
            return prompt;
        }

        final  Prompt.Builder transformedPrompt = Prompt.prompt().withValuesFrom(prompt);
        if (isNotEmpty(prompt.getStartDateValidation()) && "currentDate".equalsIgnoreCase(prompt.getStartDateValidation())) {
            transformedPrompt.withStartDateValidation(LocalDate.now().toString());
        }

        if (nonNull(prompt.getDefaultValue())) {
            final long defaultValue = Long.parseLong(prompt.getDefaultValue());
            transformedPrompt.withDefaultValue(LocalDate.now().plusDays(defaultValue).toString());
        }

        return transformedPrompt.build();
    }
}
