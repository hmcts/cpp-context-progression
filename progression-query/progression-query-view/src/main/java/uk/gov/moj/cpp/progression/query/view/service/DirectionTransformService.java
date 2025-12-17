package uk.gov.moj.cpp.progression.query.view.service;

import static com.google.common.collect.Lists.newArrayList;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.query.view.service.transformer.DirectionPrompts;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt1Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt2Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt3Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt4Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt5Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt6Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt7Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt8Transformer;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DirectionTransformService {

    @Inject
    private Prompt1Transformer prompt1Transformer;

    @Inject
    private Prompt2Transformer prompt2Transformer;

    @Inject
    private Prompt3Transformer prompt3Transformer;

    @Inject
    private Prompt4Transformer prompt4Transformer;

    @Inject
    private Prompt5Transformer prompt5Transformer;

    @Inject
    private Prompt6Transformer prompt6Transformer;

    @Inject
    private Prompt7Transformer prompt7Transformer;

    @Inject
    private Prompt8Transformer prompt8Transformer;

    public List<Prompt> transform(final Direction direction,
                                  final List<Defendant> defendantList,
                                  final Map<UUID, String> defendants,
                                  final Map<UUID, String> witnesses,
                                  final Map<UUID, String> assignees,
                                  final boolean changeWitnessPromptFixListToTxt,
                                  final String formType) {

        return Optional.ofNullable(direction.getPrompts()).map(v ->
                        v.stream().map(prompt -> transform(prompt,
                                defendantList,
                                defendants,
                                witnesses,
                                assignees,
                                changeWitnessPromptFixListToTxt,
                                formType)).filter(Objects::nonNull).collect(Collectors.toList()))
                .orElse(newArrayList());
    }

    private Prompt transform(final Prompt prompt,
                             final List<Defendant> defendantList,
                             final Map<UUID, String> defendants,
                             final Map<UUID, String> witnesses,
                             final Map<UUID, String> assignees,
                             final boolean changeWitnessPromptFixListToTxt,
                             final String formType) {

        final Prompt newPrompt;
        if (Objects.nonNull(prompt.getChildren())) {
            newPrompt = Prompt.prompt()
                    .withValuesFrom(prompt)
                    .withChildren(prompt.getChildren().stream()
                            .map(p -> transform(p, defendantList, defendants, witnesses, assignees, changeWitnessPromptFixListToTxt, formType))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build();
        } else {
            newPrompt = prompt;
        }
        switch (DirectionPrompts.valueFor(newPrompt.getId().toString())) {
            case PROMPT2:
                return prompt2Transformer.transform(newPrompt);
            case PROMPT3:
                return prompt3Transformer.transform(newPrompt, defendants, witnesses, assignees, changeWitnessPromptFixListToTxt);
            case PROMPT4:
                return prompt4Transformer.transform(newPrompt);
            case PROMPT5:
                return prompt5Transformer.transform(newPrompt);
            case PROMPT6:
                return prompt6Transformer.transform(newPrompt, defendantList, defendants, witnesses, assignees, changeWitnessPromptFixListToTxt, formType);
            case PROMPT7:
                return prompt7Transformer.transform(newPrompt, defendants);
            case PROMPT8:
                return prompt8Transformer.transform(newPrompt, defendantList, defendants, witnesses, assignees, changeWitnessPromptFixListToTxt);

            default:
                return prompt1Transformer.transform(newPrompt);
        }
    }
}
