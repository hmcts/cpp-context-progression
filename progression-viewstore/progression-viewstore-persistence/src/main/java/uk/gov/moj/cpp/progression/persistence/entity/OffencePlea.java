package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by jchondig on 01/12/2017.
 * Represents plea associated with an offence.
 * @deprecated
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Entity
@Table(name = "offence_plea")
public class OffencePlea implements Serializable {

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "value")
    private String value;

    @Column(name = "plea_date")
    private LocalDate pleaDate;

    public OffencePlea(){

    }

    public OffencePlea(final UUID id, final String value, final LocalDate pleaDate) {
        this.id = id;
        this.value = value;
        this.pleaDate = pleaDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(final LocalDate pleaDate) {
        this.pleaDate = pleaDate;
    }
}
