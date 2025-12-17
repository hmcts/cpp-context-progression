package uk.gov.moj.cpp.progression.query.view.service.transformer;


import uk.gov.moj.cpp.progression.domain.pojo.Prompt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.query.view.service.transformer.TransformerUtil.generatePromptList;

public class Prompt7Transformer {

    public static final String DEFENDANTS = "Defendants";

    public Prompt transform(final Prompt prompt,
                            final Map<UUID, String> defendants) {
        final Prompt.Builder transformedPrompt = Prompt.prompt().withValuesFrom(prompt);
        final List<Prompt> children = prompt.getChildren();

        if (nonNull(prompt.getCaseParticipant())) {
            final String[] caseParticipants = prompt.getCaseParticipant().split(",");
            for (final String caseParticipant : caseParticipants) {
                if (DEFENDANTS.equalsIgnoreCase(caseParticipant)) {
                    final List<Prompt> objDefendants = generatePromptList(defendants, children.get(children.size()-1).getPromptOrder(), true);
                    children.addAll(objDefendants);
                }
            }
            if (isNotEmpty(children)) {
                transformedPrompt.withChildren(children);
            }
            transformedPrompt.withIsEditable(prompt.getEditable());
        }

        return transformedPrompt.build();
    }

}
