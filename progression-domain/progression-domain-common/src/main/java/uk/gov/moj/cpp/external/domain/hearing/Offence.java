package uk.gov.moj.cpp.external.domain.hearing;

import java.io.Serializable;
import java.util.UUID;


public final class Offence implements Serializable {

    private static final long serialVersionUID = -7665846452603586679L;
    private UUID id;
    private UUID caseId;
    private String offenceCode;
    private String wording;
    private String section;
    private String startDate;
    private String endDate;
    private Integer orderIndex;
    private Integer count;
    private String convictionDate;
    private String title;
    private String legislation;

    public Offence(){
    }

    public Offence(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public void setOffenceCode(String offenceCode) {
        this.offenceCode = offenceCode;
    }

    public String getWording() {
        return wording;
    }

    public void setWording(String wording) {
        this.wording = wording;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(String convictionDate) {
        this.convictionDate = convictionDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLegislation() {
        return legislation;
    }

    public void setLegislation(String legislation) {
        this.legislation = legislation;
    }

}
