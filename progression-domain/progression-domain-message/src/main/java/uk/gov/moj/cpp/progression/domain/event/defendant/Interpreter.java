package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Interpreter implements Serializable {

    private static final long serialVersionUID = 3596692979344216424L;
  
    private Boolean needed;
    private String language;
    private String name;

    public Interpreter() {
    }

    public Interpreter(final Boolean needed, final String language) {
        this.needed = needed;
        this.language = language;
    }

    public Interpreter(final String language, final String name) {
        this.language = language;
        this.name = name;
    }

    public Boolean getNeeded() {
        return needed;
    }

    public void setNeeded(final Boolean needed) {
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof Interpreter)) {
            return false;
        }
        final Interpreter testObj = (Interpreter) obj;

        return new EqualsBuilder().append(this.needed, testObj.getNeeded())
                        .append(this.language, testObj.getName())
                        .append(this.language, testObj.getLanguage()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.needed).append(this.name).append(this.language)
                        .hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("Needed", this.needed).append("Name", this.name)
                        .append("Language", this.language).toString();
    }
}
