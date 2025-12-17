package uk.gov.moj.cpp.progression.query.view.service.transformer;



import uk.gov.moj.cpp.progression.domain.pojo.FixList;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.query.view.service.transformer.TransformerUtil.generateFixList;

public class Prompt3Transformer {

    public static final String DEFENDANTS = "Defendants";
    public static final String WITNESS = "Witness";
    public static final String PROSECUTOR = "Prosecutor";
    public static final String ASSIGNEES = "Assignees";
    public static final String COURT = "Court";
    public static final String PROSECUTION = "Prosecution";

    @SuppressWarnings({"squid:MethodCyclomaticComplexity","squid:S3776"})
    public Prompt transform(final Prompt prompt, final Map<UUID, String> defendants,
                            final Map<UUID, String> witnesses, final Map<UUID, String> assignees,
                            final boolean changeWitnessPromptFixListToTxt) {
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
                } else if (PROSECUTOR.equalsIgnoreCase(caseParticipant)) { // check if we should remove
                    final List<String> prosecutor = asList(caseParticipant);
                    final List<FixList> objProsecutor = generateFixList(prosecutor, PROSECUTOR);
                    fixedList.addAll(objProsecutor);
                } else if (ASSIGNEES.equalsIgnoreCase(caseParticipant)) {
                    final List<FixList> objAssignees = generateFixList(assignees);
                    fixedList.addAll(objAssignees);
                }else if (COURT.equalsIgnoreCase(caseParticipant)) {
                    final List<String> court = asList(caseParticipant);
                    final List<FixList> objCourt = generateFixList(court, COURT);
                    fixedList.addAll(objCourt);
                }else if (PROSECUTION.equalsIgnoreCase(caseParticipant)) { // UI representation is Prosecution not Prosecutor
                    final List<String> prosecution = asList(caseParticipant);
                    final List<FixList> objProsecution = generateFixList(prosecution, PROSECUTION);
                    fixedList.addAll(objProsecution);
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
        return transformedPrompt.build();
    }

}
