package uk.gov.moj.cpp.progression.domain.transformation.domain;

import java.util.List;

public class ResultDefinition {

    private String id;
    private String label;
    private String urgent;
    private String d20;
    private List<Prompt> prompts;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getUrgent() {
        return urgent;
    }

    public void setUrgent(final String urgent) {
        this.urgent = urgent;
    }

    public String getD20() {
        return d20;
    }

    public void setD20(final String d20) {
        this.d20 = d20;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(final List<Prompt> prompts) {
        this.prompts = prompts;
    }
}
