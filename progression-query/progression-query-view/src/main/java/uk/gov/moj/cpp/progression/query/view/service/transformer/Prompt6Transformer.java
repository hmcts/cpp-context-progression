package uk.gov.moj.cpp.progression.query.view.service.transformer;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.progression.domain.pojo.FixList;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.query.view.service.DefendantService;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.query.view.service.transformer.TransformerUtil.generateFixList;

@SuppressWarnings({"squid:S1168", "pmd:NullAssignment"})
public class Prompt6Transformer {

    public static final String DEFENDANTS = "Defendants";
    public static final String OPTION = "option";
    public static final String WITNESS = "Witness";
    public static final String PROSECUTOR = "Prosecutor";
    public static final String ASSIGNEES = "Assignees";
    private static final String PTPH_FORM_TYPE = "PTPH";

    @Inject
    private DefendantService defendantService;

    @SuppressWarnings({"squid:MethodCyclomaticComplexity"})
    public Prompt transform(final Prompt prompt,
                            final List<Defendant> defendantList,
                            final Map<UUID, String> defendants,
                            final Map<UUID, String> witnesses,
                            final Map<UUID, String> assignees,
                            final boolean changeWitnessPromptFixListToTxt,
                            final String formType) {
        final Prompt.Builder transformedPrompt = Prompt.prompt().withValuesFrom(prompt);
        if (nonNull(prompt.getCaseParticipant())) {
            final List<FixList> fixedList = new ArrayList<>();
            final String[] caseParticipants = prompt.getCaseParticipant().split(",");
            for (final String caseParticipant : caseParticipants) {
                if (DEFENDANTS.equalsIgnoreCase(caseParticipant)) {
                    final List<FixList> objDefendants = generateFixList(defendants, true);
                    fixedList.addAll(objDefendants);
                } else if (WITNESS.equalsIgnoreCase(caseParticipant)) {
                    final List<FixList> objWitness = generateFixList(witnesses);
                    fixedList.addAll(objWitness);
                } else if (PROSECUTOR.equalsIgnoreCase(caseParticipant)) {
                    final List<String> prosecutor = asList(caseParticipant);
                    final List<FixList> objProsecutor = generateFixList(prosecutor, PROSECUTOR);
                    fixedList.addAll(objProsecutor);
                } else if (ASSIGNEES.equalsIgnoreCase(caseParticipant)) {
                    final List<FixList> objAssignees = generateFixList(assignees);
                    fixedList.addAll(objAssignees);
                }
            }
            if (isNotEmpty(fixedList)) {
                transformedPrompt.withFixedList(fixedList);
            }

            // this if condition should be in the end
            if (prompt.getCaseParticipant().contains(WITNESS) && changeWitnessPromptFixListToTxt) {
                transformedPrompt.withType("txt");
                transformedPrompt.withFixedList(null);
            }

            transformedPrompt.withIsEditable(prompt.getEditable());
        }

        final List<Prompt> childrenRefData = Optional.ofNullable(prompt.getChildren()).orElse(new ArrayList<>());

        if (nonNull(childrenRefData)) {
            final List<Prompt> transformedChildren = transformChildren(childrenRefData, defendantList, formType);
            transformedPrompt.withChildren(transformedChildren);
        }
        return transformedPrompt.build();
    }

    private List<Prompt> transformChildren(final List<Prompt> children, final List<Defendant> defendants, final String formType) {
        List<Prompt> promptList = new ArrayList<>(children);
        final int lastElement = children.size() - 1;
        int tmpCount = 0;
        if (lastElement>=0){
             tmpCount = children.get(lastElement).getPromptOrder() + 1;
        }
        final AtomicInteger count = new AtomicInteger(tmpCount);
        AtomicInteger index = new AtomicInteger(0);
        if (isNotEmpty(defendants)) {
            defendants.forEach(defendant -> {
                if (nonNull(defendant.getPersonDefendant())) {
                    final String personLabel = defendantService.getDefendantFullName(defendant.getPersonDefendant().getPersonDetails());
                    final Prompt prompt = buildDefendantPrompt(defendant, count, lastElement, children, index, personLabel, formType);
                    promptList.add(prompt);
                } else if (nonNull(defendant.getLegalEntityDefendant())) {
                    final String companyLabel = defendant.getLegalEntityDefendant().getOrganisation().getName();
                    final Prompt prompt = buildDefendantPrompt(defendant, count, lastElement, children, index, companyLabel, formType);
                    promptList.add(prompt);
                }
            });
        }
        return promptList;
    }

    private List<Prompt> getTransformedChildren(final List<Prompt> children, final AtomicInteger index) {
        if (isNull(children)) {
            return null;
        }
        return IntStream
                .range(0, children.size())
                .mapToObj(i -> Prompt.prompt().withValuesFrom(children.get(i)).withName(children.get(i).getName().concat(String.valueOf(index.getAndIncrement()))).build())
                .collect(Collectors.toList());
    }

    private Prompt buildDefendantPrompt(final Defendant defendant, AtomicInteger count, final int lastElement, final List<Prompt> children, AtomicInteger index, String label, final String formType) {

        return Prompt.prompt()
                .withId(randomUUID())
                .withLabelId(defendant.getId().toString())
                .withLabel(label)
                .withName(defendant.getId().toString())
                .withPromptOrder(count.getAndIncrement())
                .withType(OPTION)
                .withChildren(getTransformedChildren(lastElement>=0 ? children.get(lastElement).getChildren() : null, index))
                .withValue(PTPH_FORM_TYPE.equals(formType) ? label : null)
                .withIsDefendant(true)
                .build();
    }

}
