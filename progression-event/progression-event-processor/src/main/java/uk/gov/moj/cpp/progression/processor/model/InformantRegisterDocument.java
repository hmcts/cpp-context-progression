package uk.gov.moj.cpp.progression.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("squid:S00107")
@JsonPropertyOrder({"InfDestID",
        "InfName",
        "DoH",
        "LJAName",
        "Courthouse",
        "Courtroom",
        "SessionType",
        "DefTitle",
        "DefForenames",
        "DefSurname",
        "DefAddress",
        "DefPostcode",
        "DoBDefendant",
        "CaseNo",
        "URN",
        "ASN",
        "SeqNo",
        "OffCode",
        "OffTitle",
        "Plea",
        "Verdict",
        "CJSResultCode",
        "RegText"})
public class InformantRegisterDocument {

    private static final long serialVersionUID = 4464549498658215257L;

    @JsonProperty("InfDestID")
    private final String infDestID;

    @JsonProperty("InfName")
    private final String name;

    @JsonProperty("DoH")
    private final String hearingStartTime;

    @JsonProperty("LJAName")
    private final String ljaName;

    @JsonProperty("Courthouse")
    private final String courtHouse;

    @JsonProperty("Courtroom")
    private final String courtRoom;

    @JsonProperty("SessionType")
    private final String sessionType;

    @JsonProperty("DefTitle")
    private final String title;

    @JsonProperty("DefForenames")
    private final String foreNames;

    @JsonProperty("DefSurname")
    private final String surName;

    @JsonProperty("DefAddress")
    private final String address;

    @JsonProperty("DefPostcode")
    private final String postCode;

    @JsonProperty("DoBDefendant")
    private final String dateOfBirth;

    @JsonProperty("CaseNo")
    private final String caseNumber;

    @JsonProperty("URN")
    private final String caseOrApplicationReference;

    @JsonProperty("ASN")
    private final String arrestSummonsNumber;

    @JsonProperty("SeqNo")
    private final String seqNo;

    @JsonProperty("OffCode")
    private final String offenceCode;

    @JsonProperty("OffTitle")
    private final String offenceTitle;

    @JsonProperty("Plea")
    private final String pleaValue;

    @JsonProperty("Verdict")
    private final String verdictCode;

    @JsonProperty("CJSResultCode")
    private final String cjsResultCode;

    @JsonProperty("RegText")
    private final String resultText;

    public InformantRegisterDocument(
            final String infDestID,
            final String name,
            final String hearingStartTime,
            final String ljaName,
            final String courtHouse,
            final String courtRoom,
            final String sessionType,
            final String title,
            final String foreNames,
            final String surName,
            final String address,
            final String postCode,
            final String dateOfBirth,
            final String caseNumber,
            final String caseOrApplicationReference,
            final String arrestSummonsNumber,
            final String seqNo,
            final String offenceCode,
            final String offenceTitle,
            final String pleaValue,
            final String verdictCode,
            final String cjsResultCode,
            final String resultText
    ) {
        this.infDestID = infDestID;
        this.name = name;
        this.hearingStartTime = hearingStartTime;
        this.ljaName = ljaName;
        this.courtHouse = courtHouse;
        this.courtRoom = courtRoom;
        this.sessionType = sessionType;
        this.title = title;
        this.foreNames = foreNames;
        this.surName = surName;
        this.address = address;
        this.postCode = postCode;
        this.dateOfBirth = dateOfBirth;
        this.caseNumber = caseNumber;
        this.caseOrApplicationReference = caseOrApplicationReference;
        this.arrestSummonsNumber = arrestSummonsNumber;
        this.seqNo = seqNo;
        this.offenceCode = offenceCode;
        this.offenceTitle = offenceTitle;
        this.pleaValue = pleaValue;
        this.verdictCode = verdictCode;
        this.cjsResultCode = cjsResultCode;
        this.resultText = resultText;
    }

    public String getInfDestID() {
        return infDestID;
    }

    public String getName() {
        return name;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getCourtHouse() {
        return courtHouse;
    }

    public String getCourtRoom() {
        return courtRoom;
    }

    public String getSessionType() {
        return sessionType;
    }

    public String getTitle() {
        return title;
    }

    public String getForeNames() {
        return foreNames;
    }

    public String getSurName() {
        return surName;
    }

    public String getAddress() {
        return address;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getCaseOrApplicationReference() {
        return caseOrApplicationReference;
    }

    public String getArrestSummonsNumber() {
        return arrestSummonsNumber;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public String getPleaValue() {
        return pleaValue;
    }

    public String getVerdictCode() {
        return verdictCode;
    }

    public String getCjsResultCode() {
        return cjsResultCode;
    }

    public String getResultText() {
        return resultText;
    }

    public static InformantRegisterDocument.Builder informantRegisterDocument() {
        return new uk.gov.moj.cpp.progression.processor.model.InformantRegisterDocument.Builder();
    }

    public static class Builder {
        private String infDestID;

        private String name;

        private String hearingStartTime;

        private String ljaName;

        private String courtHouse;

        private String courtRoom;

        private String sessionType;

        private String title;

        private String foreNames;

        private String surName;

        private String address;

        private String postCode;

        private String dateOfBirth;

        private String caseNumber;

        private String caseOrApplicationReference;

        private String arrestSummonsNumber;

        private String seqNo;

        private String offenceCode;

        private String offenceTitle;

        private String pleaValue;

        private String verdictCode;

        private String cjsResultCode;

        private String resultText;

        public InformantRegisterDocument.Builder withInfDestID(final String infDestID) {
            this.infDestID = infDestID;
            return this;
        }

        public InformantRegisterDocument.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public InformantRegisterDocument.Builder withHearingStartTime(final String hearingStartTime) {
            this.hearingStartTime = hearingStartTime;
            return this;
        }

        public InformantRegisterDocument.Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public InformantRegisterDocument.Builder withCourtHouse(final String courtHouse) {
            this.courtHouse = courtHouse;
            return this;
        }

        public InformantRegisterDocument.Builder withCourtRoom(final String courtRoom) {
            this.courtRoom = courtRoom;
            return this;
        }

        public InformantRegisterDocument.Builder withSessionType(final String sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        public InformantRegisterDocument.Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public InformantRegisterDocument.Builder withForeNames(final String foreNames) {
            this.foreNames = foreNames;
            return this;
        }

        public InformantRegisterDocument.Builder withSurName(final String surName) {
            this.surName = surName;
            return this;
        }

        public InformantRegisterDocument.Builder withAddress(final String address) {
            this.address = address;
            return this;
        }

        public InformantRegisterDocument.Builder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public InformantRegisterDocument.Builder withDateOfBirth(final String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public InformantRegisterDocument.Builder withCaseNumber(final String caseNumber) {
            this.caseNumber = caseNumber;
            return this;
        }

        public InformantRegisterDocument.Builder withCaseOrApplicationReference(final String caseOrApplicationReference) {
            this.caseOrApplicationReference = caseOrApplicationReference;
            return this;
        }

        public InformantRegisterDocument.Builder withArrestSummonsNumber(final String arrestSummonsNumber) {
            this.arrestSummonsNumber = arrestSummonsNumber;
            return this;
        }



        public InformantRegisterDocument.Builder withSeqNo(final String seqNo) {
            this.seqNo = seqNo;
            return this;
        }



        public InformantRegisterDocument.Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public InformantRegisterDocument.Builder withOffenceTitle(final String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public InformantRegisterDocument.Builder withPleaValue(final String pleaValue) {
            this.pleaValue = pleaValue;
            return this;
        }

        public InformantRegisterDocument.Builder withVerdictCode(final String verdictCode) {
            this.verdictCode = verdictCode;
            return this;
        }

        public InformantRegisterDocument.Builder withCjsResultCode(final String cjsResultCode) {
            this.cjsResultCode = cjsResultCode;
            return this;
        }

        public InformantRegisterDocument.Builder withResultText(final String resultText) {
            this.resultText = resultText;
            return this;
        }

        public InformantRegisterDocument build() {
            return new uk.gov.moj.cpp.progression.processor.model.InformantRegisterDocument(
                    infDestID,
                    name,
                    hearingStartTime,
                    ljaName,
                    courtHouse,
                    courtRoom,
                    sessionType,
                    title,
                    foreNames,
                    surName,
                    address,
                    postCode,
                    dateOfBirth,
                    caseNumber,
                    caseOrApplicationReference,
                    arrestSummonsNumber,
                    seqNo,
                    offenceCode,
                    offenceTitle,
                    pleaValue,
                    verdictCode,
                    cjsResultCode,
                    resultText);
        }
    }
}
