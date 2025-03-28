package uk.gov.justice.api.resource.dto;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDefinitionPrompt {

    private UUID id;

    private String label;

    private String welshLabel;

    private Boolean mandatory;

    private String resultPromptRule;

    private String type;

    private Integer sequence;

    private String duration;

    private List<String> wordGroup;

    private List<String> userGroups;

    private UUID fixedListId;

    private String qual;

    private String reference;

    private String courtExtract;

    private Integer durationSequence;

    private boolean isAvailableForCourtExtract;

    private Boolean hidden;

    private String cacheDataPath;

    private Integer cacheable;

    private String partName;

    private Boolean isDurationStartDate;

    private Boolean isDurationEndDate;

    public static ResultDefinitionPrompt prompt() {
        return new ResultDefinitionPrompt();
    }

    public UUID getId() {
        return this.id;
    }

    public ResultDefinitionPrompt setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getLabel() {
        return this.label;
    }

    public ResultDefinitionPrompt setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean getMandatory() {
        return this.mandatory;
    }

    public ResultDefinitionPrompt setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public String getResultPromptRule() {
        return this.resultPromptRule;
    }

    public ResultDefinitionPrompt setResultPromptRule(final String resultPromptRule) {
        this.resultPromptRule = resultPromptRule;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public ResultDefinitionPrompt setType(String type) {
        this.type = type;
        return this;
    }

    public Integer getSequence() {
        return this.sequence;
    }

    public ResultDefinitionPrompt setSequence(Integer sequence) {
        this.sequence = sequence;
        return this;
    }

    public String getDuration() {
        return this.duration;
    }

    public ResultDefinitionPrompt setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public List<String> getWordGroup() {
        return Collections.unmodifiableList(ofNullable(this.wordGroup).orElseGet(ArrayList::new));
    }

    public ResultDefinitionPrompt setWordGroup(List<String> wordGroup) {
        this.wordGroup = Collections.unmodifiableList(ofNullable(wordGroup).orElseGet(ArrayList::new));
        return this;
    }

    public List<String> getUserGroups() {
        return Collections.unmodifiableList(ofNullable(this.userGroups).orElseGet(ArrayList::new));
    }

    public ResultDefinitionPrompt setUserGroups(List<String> userGroups) {
        this.userGroups = Collections.unmodifiableList(ofNullable(userGroups).orElseGet(ArrayList::new));
        return this;
    }

    public UUID getFixedListId() {
        return this.fixedListId;
    }

    public ResultDefinitionPrompt setFixedListId(UUID fixedListId) {
        this.fixedListId = fixedListId;
        return this;
    }

    public String getReference() {
        return this.reference;
    }

    public ResultDefinitionPrompt setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public boolean isAvailableForCourtExtract() {
        return isAvailableForCourtExtract;
    }

    public ResultDefinitionPrompt setAvailableForCourtExtract(boolean availableForCourtExtract) {
        isAvailableForCourtExtract = availableForCourtExtract;
        return this;
    }

    public String getCourtExtract() {
        return courtExtract;
    }

    public ResultDefinitionPrompt setCourtExtract(final String courtExtract) {
        this.courtExtract = courtExtract;
        return this;
    }

    public String getWelshLabel() {
        return welshLabel;
    }

    public ResultDefinitionPrompt setWelshLabel(String welshLabel) {
        this.welshLabel = welshLabel;
        return this;
    }

    public String getQual() {
        return qual;
    }

    public ResultDefinitionPrompt setQual(final String qual) {
        this.qual = qual;
        return this;
    }

    public Integer getDurationSequence() {
        return durationSequence;
    }

    public ResultDefinitionPrompt setDurationSequence(final Integer durationSequence) {
        this.durationSequence = durationSequence;
        return this;
    }

    public Boolean isHidden() {
        return this.hidden;
    }

    public ResultDefinitionPrompt setHidden(final Boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCacheDataPath() {
        return cacheDataPath;
    }

    public ResultDefinitionPrompt setCacheDataPath(final String cacheDataPath) {
        this.cacheDataPath = cacheDataPath;
        return this;
    }

    public Integer getCacheable() {
        return cacheable;
    }

    public ResultDefinitionPrompt setCacheable(final Integer cacheable) {
        this.cacheable = cacheable;
        return this;
    }


    public String getPartName() {
        return partName;
    }

    public ResultDefinitionPrompt setPartName(final String partName) {
        this.partName = partName;
        return this;
    }

    public Boolean getIsDurationStartDate() {
        return isDurationStartDate;
    }

    public ResultDefinitionPrompt setIsDurationStartDate(final Boolean isDurationStartDate) {
        this.isDurationStartDate = isDurationStartDate;
        return this;
    }

    public Boolean getIsDurationEndDate() {
        return isDurationEndDate;
    }

    public ResultDefinitionPrompt setIsDurationEndDate(final Boolean isDurationEndDate) {
        this.isDurationEndDate = isDurationEndDate;
        return this;
    }
}
