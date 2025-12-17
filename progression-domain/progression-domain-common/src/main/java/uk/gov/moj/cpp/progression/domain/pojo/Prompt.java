package uk.gov.moj.cpp.progression.domain.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S107", "squid:S2384", "squid:S00107", "squid:S1948"})
public class Prompt implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String caseParticipant;

    private final String defaultValue;

    private final List<FixList> fixedList;

    private final Boolean header;

    private final UUID id;

    private final UUID key;

    private final String label;

    private final String name;

    private final Integer promptOrder;

    private final Boolean required;

    private final String startDateValidation;

    private final String subHeading;

    private final String type;

    private final String value;

    private final String renderText;

    private List<Prompt> children;

    private final List<Prompt> selectList;

    private final String displayTextStatic;

    private final String duplicate;

    private final String displayText;

    private final Object defaultIf;

    private final String labelId;

    private final Boolean subDirection;

    private final Boolean isAssignee;

    private final Boolean isDueDate;

    @JsonProperty(value = "isEditable")
    private final Boolean editable;

    private final String duplicateChildren;
    private final Boolean inline;
    private final String hint;
    private final Integer width;
    private final Boolean isDuplicatePrompt;
    private final Integer inlinePercentage;

    private final Boolean hide;
    private final Boolean hideLabel;
    private final Boolean isDefendant;


    public Prompt(final String caseParticipant, final String defaultValue, final List<FixList> fixedList, final Boolean header, final UUID id,
                  final UUID key, final String label, final String name, final Integer promptOrder, final Boolean required, final String startDateValidation,
                  final String subHeading, final String type, final String value, final String renderText, final List<Prompt> children,
                  final List<Prompt> selectList, final String duplicate, final String displayText,
                  final Object defaultIf, final String labelId, final Boolean editable, final String displayTextStatic,
                  final Boolean subDirection, final Boolean isAssignee, final Boolean isDueDate,
                  final String duplicateChildren, final Boolean inline, final String hint, final Integer width,
                  final Boolean isDuplicatePrompt, final Integer inlinePercentage, final Boolean hide, final Boolean hideLabel,
                  final Boolean isDefendant) {
        this.caseParticipant = caseParticipant;
        this.defaultValue = defaultValue;
        this.fixedList = fixedList;
        this.header = header;
        this.id = id;
        this.key = key;
        this.label = label;
        this.name = name;
        this.promptOrder = promptOrder;
        this.required = required;
        this.startDateValidation = startDateValidation;
        this.subHeading = subHeading;
        this.type = type;
        this.value = value;
        this.renderText = renderText;
        this.children = children;
        this.selectList = selectList;
        this.duplicate = duplicate;
        this.displayText = displayText;
        this.defaultIf = defaultIf;
        this.labelId = labelId;
        this.editable = editable;
        this.subDirection = subDirection;
        this.displayTextStatic = displayTextStatic;
        this.isAssignee = isAssignee;
        this.isDueDate = isDueDate;

        this.duplicateChildren = duplicateChildren;
        this.inline = inline;
        this.hint = hint;
        this.width = width;
        this.isDuplicatePrompt = isDuplicatePrompt;
        this.inlinePercentage = inlinePercentage;

        this.hide = hide;
        this.hideLabel = hideLabel;
        this.isDefendant = isDefendant;
    }

    public static Builder prompt() {
        return new Builder();
    }

    public String getCaseParticipant() {
        return caseParticipant;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<FixList> getFixedList() {
        return fixedList;
    }

    public Boolean getHeader() {
        return header;
    }

    public UUID getId() {
        return id;
    }

    public UUID getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public Integer getPromptOrder() {
        return promptOrder;
    }

    public Boolean getRequired() {
        return required;
    }

    public String getStartDateValidation() {
        return startDateValidation;
    }

    public String getSubHeading() {
        return subHeading;
    }

    public Object getDefaultIf() {
        return defaultIf;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getRenderText() {
        return renderText;
    }

    public List<Prompt> getChildren() {
        return children;
    }

    public void setChildren(final List<Prompt> children) {
        this.children = children;
    }

    public List<Prompt> getSelectList() {
        return selectList;
    }

    public String getDuplicate() {
        return duplicate;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getLabelId() {
        return labelId;
    }

    public Boolean getEditable() {
        return editable;
    }

    public Boolean getIsAssignee() {
        return isAssignee;
    }

    public Boolean getIsDueDate() {
        return isDueDate;
    }

    public String getDisplayTextStatic() {
        return displayTextStatic;
    }

    public String getDuplicateChildren() {
        return duplicateChildren;
    }

    public Boolean getInline() {
        return inline;
    }

    public String getHint() {
        return hint;
    }

    public Integer getWidth() {
        return width;
    }

    public Boolean getDuplicatePrompt() {
        return isDuplicatePrompt;
    }

    public Integer getInlinePercentage() {
        return inlinePercentage;
    }

    public Boolean getHide() {
        return hide;
    }

    public Boolean getHideLabel() {
        return hideLabel;
    }

    public Boolean getIsDefendant() {
        return isDefendant;
    }

    public Boolean getSubDirection() {
        return subDirection;
    }

    public static class Builder {
        private String caseParticipant;

        private String defaultValue;

        private List<FixList> fixedList;

        private Boolean header;

        private UUID id;

        private UUID key;

        private String label;

        private String name;

        private Integer promptOrder;

        private Boolean required;

        private String startDateValidation;

        private String subHeading;

        private String type;

        private String value;

        private List<Prompt> children;

        private List<Prompt> selectList;

        private String renderText;

        private String duplicate;

        private String displayText;

        private Object defaultIf;

        private String labelId;

        private Boolean editable;

        private String displayTextStatic;

        private Boolean subDirection;

        private Boolean isDueDate;

        private Boolean isAssignee;

        private String duplicateChildren;
        private Boolean inline;
        private String hint;
        private Integer width;
        private Boolean isDuplicatePrompt;
        private Integer inlinePercentage;
        private Boolean hide;
        private Boolean hideLabel;
        private Boolean isDefendant;

        public Builder withCaseParticipant(final String caseParticipant) {
            this.caseParticipant = caseParticipant;
            return this;
        }

        public Builder withDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder withFixedList(final List<FixList> fixedList) {
            this.fixedList = fixedList;
            return this;
        }

        public Builder withHeader(final Boolean header) {
            this.header = header;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withKey(final UUID key) {
            this.key = key;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withPromptOrder(final Integer promptOrder) {
            this.promptOrder = promptOrder;
            return this;
        }

        public Builder withRequired(final Boolean required) {
            this.required = required;
            return this;
        }

        public Builder withStartDateValidation(final String startDateValidation) {
            this.startDateValidation = startDateValidation;
            return this;
        }

        public Builder withSubHeading(final String subHeading) {
            this.subHeading = subHeading;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public Builder withRenderText(final String renderText) {
            this.renderText = renderText;
            return this;
        }

        public Builder withChildren(final List<Prompt> children) {
            this.children = children;
            return this;
        }

        public Builder withSelectList(final List<Prompt> selectList) {
            this.selectList = selectList;
            return this;
        }

        public Builder withDuplicate(final String duplicate) {
            this.duplicate = duplicate;
            return this;
        }

        public Builder withDisplayText(final String displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder withDisplayTextStatic(final String displayTextStatic) {
            this.displayTextStatic = displayTextStatic;
            return this;
        }

        public Builder withDefaultIf(final Object defaultIf) {
            this.defaultIf = defaultIf;
            return this;
        }

        public Builder withLabelId(final String labelId) {
            this.labelId = labelId;
            return this;
        }

        public Builder withIsEditable(final Boolean isEditable) {
            this.editable = isEditable;
            return this;
        }

        public Builder withSubDirection(final Boolean subDirection) {
            this.subDirection = subDirection;
            return this;
        }

        public Builder withIsAssignee(final Boolean isAssignee) {
            this.isAssignee = isAssignee;
            return this;
        }

        public Builder withIsDueDate(final Boolean isDueDate) {
            this.isDueDate = isDueDate;
            return this;
        }

        public Builder withDuplicateChildren(final String duplicateChildren) {
            this.duplicateChildren = duplicateChildren;
            return this;
        }

        public Builder withInline(final Boolean inline) {
            this.inline = inline;
            return this;
        }

        public Builder withHint(final String hint) {
            this.hint = hint;
            return this;
        }

        public Builder withWidth(final Integer width) {
            this.width = width;
            return this;
        }

        public Builder withIsDuplicatePrompt(final Boolean isDuplicatePrompt) {
            this.isDuplicatePrompt = isDuplicatePrompt;
            return this;
        }

        public Builder withInlinePercentage(final Integer inlinePercentage) {
            this.inlinePercentage = inlinePercentage;
            return this;
        }


        public Builder withHide(final Boolean hide) {
            this.hide = hide;
            return this;
        }


        public Builder withHideLabel(final Boolean hideLabel) {
            this.hideLabel = hideLabel;
            return this;
        }


        public Builder withIsDefendant(final Boolean isDefendant) {
            this.isDefendant = isDefendant;
            return this;
        }


        public Builder withValuesFrom(final Prompt prompt) {
            this.caseParticipant = prompt.getCaseParticipant();
            this.defaultValue = prompt.getDefaultValue();
            this.fixedList = prompt.getFixedList();
            this.header = prompt.getHeader();
            this.id = prompt.getId();
            this.key = prompt.getKey();
            this.label = prompt.getLabel();
            this.name = prompt.getName();
            this.promptOrder = prompt.getPromptOrder();
            this.required = prompt.getRequired();
            this.startDateValidation = prompt.getStartDateValidation();
            this.subHeading = prompt.getSubHeading();
            this.type = prompt.getType();
            this.value = prompt.getValue();
            this.renderText = prompt.getRenderText();
            this.children = Optional.ofNullable(prompt.getChildren()).map(Collections::unmodifiableList).orElse(null);
            this.selectList = Optional.ofNullable(prompt.getSelectList()).map(Collections::unmodifiableList).orElse(null);
            this.duplicate = prompt.getDuplicate();
            this.displayText = prompt.getDisplayText();
            this.defaultIf = prompt.getDefaultIf();
            this.labelId = prompt.getLabelId();
            this.editable = prompt.editable;
            this.subDirection = prompt.subDirection;
            this.displayTextStatic = prompt.displayTextStatic;
            this.isAssignee = prompt.isAssignee;
            this.isDueDate = prompt.isDueDate;
            this.duplicateChildren = prompt.duplicateChildren;
            this.inline = prompt.inline;
            this.hint = prompt.hint;
            this.width = prompt.width;
            this.isDuplicatePrompt = prompt.isDuplicatePrompt;
            this.inlinePercentage = prompt.inlinePercentage;
            this.hide = prompt.hide;
            this.hideLabel = prompt.hideLabel;
            this.isDefendant = prompt.isDefendant;
            return this;
        }

        public Prompt build() {
            return new Prompt(caseParticipant, defaultValue, fixedList, header, id, key, label, name, promptOrder,
                    required, startDateValidation, subHeading, type, value, renderText, children, selectList,
                    duplicate, displayText, defaultIf, labelId, editable, displayTextStatic, subDirection, isAssignee, isDueDate,
                    duplicateChildren, inline, hint, width, isDuplicatePrompt, inlinePercentage, hide, hideLabel, isDefendant);
        }
    }
}
