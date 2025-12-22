package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.moj.cpp.progression.domain.PostalAddressee;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;
import uk.gov.moj.cpp.progression.domain.PostalHearingCourtDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class HearingTemplatePayload {

  private String issueDate;

  private String ljaCode;

  private String ljaName;

  private String courtCentreName;

  private String hearingType;

  private String reference;

  private PostalAddressee addressee;

  private PostalDefendant defendant;

  private PostalHearingCourtDetails hearingCourtDetails;

  private List<CaseOffence> offenceList;
  private List<CaseOffence> welshOffenceList;
  private boolean isCivil;


  public HearingTemplatePayload(final String issueDate, final String ljaCode, final String ljaName, final String courtCentreName, final String hearingType, final String reference, final PostalAddressee addressee, final PostalDefendant defendant, final PostalHearingCourtDetails hearingCourtDetails,
                                final List<CaseOffence> offenceList, final List<CaseOffence> welshOffenceList, final boolean isCivil) {
    this.issueDate = issueDate;
    this.ljaCode = ljaCode;
    this.ljaName = ljaName;
    this.courtCentreName = courtCentreName;
    this.hearingType = hearingType;
    this.reference = reference;
    this.addressee = addressee;
    this.defendant = defendant;
    this.hearingCourtDetails = hearingCourtDetails;
    this.offenceList = ImmutableList.copyOf(offenceList);
    this.welshOffenceList = ImmutableList.copyOf(welshOffenceList);
    this.isCivil = isCivil;
  }

  public boolean getIsCivil() {
    return isCivil;
  }

  public void setIsCivil(final boolean isCivil) {
    this.isCivil = isCivil;
  }

  public List<CaseOffence> getWelshOffenceList() {
    return new ArrayList<>(welshOffenceList);
  }

  public void setWelshOffenceList(final List<CaseOffence> welshOffenceList) {
    this.welshOffenceList = new ArrayList<>(welshOffenceList);
  }

  public String getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(final String issueDate) {
    this.issueDate = issueDate;
  }

  public String getLjaCode() {
    return ljaCode;
  }

  public void setLjaCode(final String ljaCode) {
    this.ljaCode = ljaCode;
  }

  public String getLjaName() {
    return ljaName;
  }

  public void setLjaName(final String ljaName) {
    this.ljaName = ljaName;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(final String reference) {
    this.reference = reference;
  }

  public PostalAddressee getAddressee() {
    return addressee;
  }

  public void setAddressee(final PostalAddressee addressee) {
    this.addressee = addressee;
  }

  public PostalDefendant getDefendant() {
    return defendant;
  }

  public void setDefendant(final PostalDefendant defendant) {
    this.defendant = defendant;
  }

  public PostalHearingCourtDetails getHearingCourtDetails() {
    return hearingCourtDetails;
  }

  public List<CaseOffence> getOffenceList() {
    return new ArrayList<>(offenceList);
  }

  public String getCourtCentreName() {
    return courtCentreName;
  }

  public void setCourtCentreName(final String courtCentreName) {
    this.courtCentreName = courtCentreName;
  }

  public String getHearingType() {
    return hearingType;
  }

  public void setHearingType(final String hearingType) {
    this.hearingType = hearingType;
  }

  public static HearingTemplatePayload.Builder builder() {
    return new HearingTemplatePayload.Builder();
  }

  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (obj != null && this.getClass() == obj.getClass()) {
      final HearingTemplatePayload that = (HearingTemplatePayload) obj;
      final boolean courtInfoPresence = Objects.equals(this.issueDate, that.issueDate) && Objects.equals(this.ljaCode, that.ljaCode) && Objects.equals(this.ljaName, that.ljaName) && Objects.equals(this.courtCentreName, that.courtCentreName);
      final boolean entityInfoPresence = Objects.equals(this.hearingType, that.hearingType) && Objects.equals(this.reference, that.reference) && Objects.equals(this.addressee, that.addressee) && Objects.equals(this.defendant, that.defendant)
              && Objects.equals(this.hearingCourtDetails, that.hearingCourtDetails);
      final boolean offenceListPresence = Objects.equals(this.offenceList, that.offenceList) && Objects.equals(this.welshOffenceList, that.welshOffenceList);
      return courtInfoPresence && offenceListPresence && entityInfoPresence && isCivil;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return Objects.hash(this.issueDate, this.ljaCode, this.ljaName, this.courtCentreName, this.hearingType, this.reference, this.addressee, this.defendant, this.hearingCourtDetails, this.offenceList, this.welshOffenceList, this.isCivil);
  }

  public static class Builder {

    private String issueDate;
    private String ljaCode;
    private String ljaName;

    private String courtCentreName;
    private String hearingType;

    private String reference;

    private PostalAddressee addressee;

    private PostalDefendant defendant;

    private PostalHearingCourtDetails hearingCourtDetails;

    private List<CaseOffence> offenceList;
    private List<CaseOffence> welshOffenceList;
    private boolean isCivil;

    public HearingTemplatePayload build() {
      return new HearingTemplatePayload(this.issueDate, this.ljaCode, this.ljaName, this.courtCentreName, this.hearingType, this.reference, this.addressee, this.defendant, this.hearingCourtDetails, this.offenceList, this.welshOffenceList, this.isCivil);
    }

    public HearingTemplatePayload.Builder withReference(final String reference) {
      this.reference = reference;
      return this;
    }

    public HearingTemplatePayload.Builder withIssueDate(final String issueDate) {
      this.issueDate = issueDate;
      return this;
    }

    public HearingTemplatePayload.Builder withLjaCode(final String ljaCode) {
      this.ljaCode = ljaCode;
      return this;
    }

    public HearingTemplatePayload.Builder withLjaName(final String ljaName) {
      this.ljaName = ljaName;
      return this;
    }

    public HearingTemplatePayload.Builder withPostalAddressee(final PostalAddressee addressee) {
      this.addressee = addressee;
      return this;
    }

    public HearingTemplatePayload.Builder withPostalDefendant(final PostalDefendant defendant) {
      this.defendant = defendant;
      return this;
    }

    public HearingTemplatePayload.Builder withPostalHearingCourtDetails(final PostalHearingCourtDetails hearingCourtDetails) {
      this.hearingCourtDetails = hearingCourtDetails;
      return this;
    }

    public HearingTemplatePayload.Builder withOffence(final List<CaseOffence> offenceList) {
      this.offenceList = ImmutableList.copyOf(offenceList);
      return this;
    }

    public HearingTemplatePayload.Builder withWelshOffence(final List<CaseOffence> welshOffenceList) {
      this.welshOffenceList = ImmutableList.copyOf(welshOffenceList);
      return this;
    }

    public HearingTemplatePayload.Builder withIsCivil(final boolean isCivil) {
      this.isCivil = isCivil;
      return this;
    }

    public HearingTemplatePayload.Builder withCourtCentreName(final String courtCentreName) {
      this.courtCentreName = courtCentreName;
      return this;
    }

    public HearingTemplatePayload.Builder withHearingType(final String hearingType) {
      this.hearingType = hearingType;
      return this;
    }

    public HearingTemplatePayload.Builder withValuesFrom(final HearingTemplatePayload hearingTemplatePayload) {
      this.addressee = hearingTemplatePayload.getAddressee();
      this.defendant = hearingTemplatePayload.getDefendant();
      this.ljaCode = hearingTemplatePayload.getLjaName();
      this.ljaName = hearingTemplatePayload.getLjaName();
      this.courtCentreName = hearingTemplatePayload.getCourtCentreName();
      this.hearingType = hearingTemplatePayload.getHearingType();
      this.hearingCourtDetails = hearingTemplatePayload.getHearingCourtDetails();
      this.reference = hearingTemplatePayload.getReference();
      this.issueDate = hearingTemplatePayload.getIssueDate();
      this.offenceList = hearingTemplatePayload.getOffenceList();
      this.welshOffenceList = hearingTemplatePayload.getWelshOffenceList();
      this.isCivil = hearingTemplatePayload.getIsCivil();
      return this;
    }

  }

}

