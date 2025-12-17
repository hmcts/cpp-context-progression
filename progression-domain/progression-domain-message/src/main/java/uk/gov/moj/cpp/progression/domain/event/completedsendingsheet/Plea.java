package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Plea implements Serializable{

    private static final long serialVersionUID = -6924534111241570298L;
    private final UUID id;
    private final String value;
    private final LocalDate pleaDate;
    public Plea(final UUID id, final String value, final LocalDate pleaDate) {
        this.id = id;
        this.value = value;
        this.pleaDate = pleaDate;
    }
    public UUID getId() {
        return this.id;
    }

    public LocalDate getPleaDate() {
        return this.pleaDate;
    }

    public String getValue() {
        return this.value;
    }

}
