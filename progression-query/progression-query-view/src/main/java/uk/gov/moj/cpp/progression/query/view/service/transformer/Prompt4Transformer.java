package uk.gov.moj.cpp.progression.query.view.service.transformer;


import uk.gov.moj.cpp.progression.domain.pojo.Prompt;

public class Prompt4Transformer {

    public Prompt transform(final Prompt prompt) {
        final Prompt.Builder transformedPrompt = Prompt.prompt().withValuesFrom(prompt);
        return transformedPrompt.build();
    }
}
