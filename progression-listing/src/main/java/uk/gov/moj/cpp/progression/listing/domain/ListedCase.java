package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S1067", "squid:S00121", "pmd:BeanMembersShouldSerialize"})
public class ListedCase {
  private final CaseIdentifier caseIdentifier;

  private final Prosecutor prosecutor;

  private final List<Defendant> defendants;

  private final List<CaseMarker> caseMarkers;

  private final UUID id;

  private final Optional<Boolean> shadowListed;

  private final String trialReceiptType;

  private final Optional<Boolean> isCivil;

  private final Optional<UUID> groupId;

  private final Optional<Boolean> isGroupMaster;

  private final Optional<Boolean> isGroupMember;

  @SuppressWarnings({"squid:S2384"})
  public ListedCase(final CaseIdentifier caseIdentifier, final Prosecutor prosecutor, final List<Defendant> defendants, final List<CaseMarker> caseMarkers, final UUID id, final Optional<Boolean> shadowListed, final String trialReceiptType,
                    final Optional<Boolean> isCivil, final Optional<UUID> groupId, final Optional<Boolean> isGroupMember, final Optional<Boolean> isGroupMaster) {
    this.caseIdentifier = caseIdentifier;
    this.prosecutor = prosecutor;
    this.defendants = defendants;
    this.caseMarkers = caseMarkers;
    this.id = id;
    this.shadowListed = shadowListed;
    this.trialReceiptType = trialReceiptType;
    this.isCivil = isCivil;
    this.groupId = groupId;
    this.isGroupMember = isGroupMember;
    this.isGroupMaster = isGroupMaster;
  }

  public CaseIdentifier getCaseIdentifier() {
    return caseIdentifier;
  }

  public Prosecutor getProsecutor() { return prosecutor; }

  @SuppressWarnings({"squid:S2384"})
  public List<Defendant> getDefendants() {
    return defendants;
  }

  @SuppressWarnings({"squid:S2384"})
  public List<CaseMarker> getCaseMarkers() {
    return caseMarkers;
  }

  public UUID getId() {
    return id;
  }

  public Optional<Boolean> getShadowListed() {
    return shadowListed;
  }

  public String getTrialReceiptType() {
    return trialReceiptType;
  }

  public Optional<Boolean> getIsCivil() {
    return isCivil;
  }

  public Optional<UUID> getGroupId() {
    return groupId;
  }

  public Optional<Boolean> getIsGroupMember() {
    return isGroupMember;
  }

  public Optional<Boolean> getIsGroupMaster() {
    return isGroupMaster;
  }

  public static Builder listedCase() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    final ListedCase that = (ListedCase) obj;

    return Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
    Objects.equals(this.prosecutor, that.prosecutor) &&
    Objects.equals(this.defendants, that.defendants) &&
    Objects.equals(this.id, that.id) &&
    Objects.equals(this.trialReceiptType, that.trialReceiptType) &&
    Objects.equals(this.shadowListed, that.shadowListed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseIdentifier, prosecutor, defendants, caseMarkers, id, shadowListed);
  }

  @Override
  public String toString() {
    return "ListedCase{" +
            "caseIdentifier=" + caseIdentifier +
            ", prosecutor=" + prosecutor +
            ", defendants=" + defendants +
            ", caseMarkers=" + caseMarkers +
            ", id=" + id +
            ", shadowListed=" + shadowListed +
            ", trialReceiptType=" + trialReceiptType +
            ", isCivil=" + isCivil +
            ", groupId=" + groupId +
            ", isGroupMember=" + isGroupMember +
            ", isGroupMaster=" + isGroupMaster +
            '}';
  }

  @SuppressWarnings("pmd:BeanMembersShouldSerialize")
  public static class Builder {
    private CaseIdentifier caseIdentifier;

    private Prosecutor prosecutor;

    private List<Defendant> defendants;

    private UUID id;

    private List<CaseMarker> caseMarkers;

    private Optional<Boolean> shadowListed = empty();

    private String trialReceiptType;

    private Optional<Boolean> isCivil;

    private Optional<UUID> groupId;

    private Optional<Boolean> isGroupMember;

    private Optional<Boolean> isGroupMaster;

    public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
      this.caseIdentifier = caseIdentifier;
      return this;
    }

    public Builder withProsecutor(final Prosecutor prosecutor) {
      this.prosecutor = prosecutor;
      return this;
    }

    @SuppressWarnings({"squid:S2384"})
    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    @SuppressWarnings({"squid:S2384"})
    public Builder withCaseMarkers(final List<CaseMarker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withShadowListed(final Optional<Boolean> shadowListed) {
      this.shadowListed = shadowListed;
      return this;
    }

    public Builder withTrialReceiptType(final String trialReceiptType) {
      this.trialReceiptType = trialReceiptType;
      return this;
    }

    public Builder withIsCivil(final Optional<Boolean> isCivil){
      this.isCivil = isCivil;
      return this;
    }

    public Builder withGroupId(final Optional<UUID> groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withIsGroupMember(final Optional<Boolean> isGroupMember){
      this.isGroupMember = isGroupMember;
      return this;
    }

    public Builder withIsGroupMaster(final Optional<Boolean> isGroupMaster){
      this.isGroupMaster = isGroupMaster;
      return this;
    }

    public Builder withValuesFrom(final ListedCase listedCase) {
      this.caseIdentifier = listedCase.caseIdentifier;
      this.prosecutor = listedCase.getProsecutor();
      this.defendants = listedCase.getDefendants();
      this.caseMarkers = listedCase.getCaseMarkers();
      this.id = listedCase.getId();
      this.shadowListed = listedCase.getShadowListed();
      this.trialReceiptType = listedCase.getTrialReceiptType();
      this.isCivil = listedCase.getIsCivil();
      this.groupId = listedCase.getGroupId();
      this.isGroupMember = listedCase.getIsGroupMember();
      this.isGroupMaster = listedCase.getIsGroupMaster();
      return this;
    }

    public ListedCase build() {
      return new ListedCase(caseIdentifier, prosecutor, defendants, caseMarkers, id, shadowListed, trialReceiptType,
              isCivil, groupId, isGroupMember, isGroupMaster);
    }
  }
}
