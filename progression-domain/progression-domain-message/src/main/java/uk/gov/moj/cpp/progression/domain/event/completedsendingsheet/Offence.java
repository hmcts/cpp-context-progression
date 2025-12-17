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
public class Offence implements Serializable{
    private static final long serialVersionUID = -6921834111241570298L;
    private UUID id;
    private String offenceCode;
    private LocalDate convictionDate;
    private Plea plea;
    private IndicatedPlea indicatedPlea;
    private String section;
    private String wording;
    private String reason;
    private String description;
    private String category;
    private String startDate;
    private String endDate;
    private String title;
    private String legislation;
    private Integer orderIndex;

    public UUID getId() {
        return this.id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getOffenceCode() {
        return this.offenceCode;
    }

    public void setOffenceCode(final String offenceCode) {
        this.offenceCode = offenceCode;
    }

    public Plea getPlea() {
        return this.plea;
    }

    public void setPlea(final Plea plea) {
        this.plea = plea;
    }

    public IndicatedPlea getIndicatedPlea() {
        return this.indicatedPlea;
    }

    public void setIndicatedPlea(final IndicatedPlea indicatedPlea) {
        this.indicatedPlea = indicatedPlea;
    }

    public String getSection() {
        return this.section;
    }

    public void setSection(final String section) {
        this.section = section;
    }

    public String getWording() {
        return this.wording;
    }

    public void setWording(final String wording) {
        this.wording = wording;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getStartDate() {
        return this.startDate;
    }

    public void setStartDate(final String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return this.endDate;
    }

    public void setEndDate(final String endDate) {
        this.endDate = endDate;
    }

    public LocalDate getConvictionDate() {
        return this.convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getLegislation() {
        return legislation;
    }

    public void setLegislation(final String legislation) {
        this.legislation = legislation;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(final Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
