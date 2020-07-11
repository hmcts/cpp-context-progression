const Mapper = require('../../Mapper');
const {NextHearingCourtDetails, NowAddress} = require('../../../Model/NowContent');
const _ = require('lodash');
const dateService = require('../../../../../NowsHelper/service/DateService');

class NextHearingCourtDetailsMapper extends Mapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildNextHearingDetails() {

        const resultsWithNextHearing = this.findMatchNextHearing(this.nowVariant.results);

        if (resultsWithNextHearing) {
            const nextHearing = resultsWithNextHearing.nextHearing;
            const courtCentre = nextHearing.courtCentre;
            const nextHearingCourtDetails = new NextHearingCourtDetails();
            nextHearingCourtDetails.courtName = this.courtName(courtCentre);
            nextHearingCourtDetails.welshCourtName = this.welshCourtName(courtCentre);

            if(nextHearing.listedStartDateTime) {
                nextHearingCourtDetails.hearingDate =  dateService.getLocalDate(nextHearing.listedStartDateTime);
                nextHearingCourtDetails.hearingTime = dateService.getLocalTime(nextHearing.listedStartDateTime);
            } else {
                nextHearingCourtDetails.hearingWeekCommencing = this.hearingWeekCommencing(nextHearing);
            }

            nextHearingCourtDetails.roomName = this.roomName(courtCentre);
            nextHearingCourtDetails.welshRoomName = this.welshRoomName(courtCentre);

            if (this.isAddressAvailable(courtCentre)) {
                const address = new NowAddress();
                address.line1 = this.address1(courtCentre);
                address.line2 = this.address2(courtCentre);
                address.line3 = this.address3(courtCentre);
                address.line4 = this.address4(courtCentre);
                address.line5 = this.address5(courtCentre);
                address.postCode = this.postcode(courtCentre);
                nextHearingCourtDetails.courtAddress = address;
            }

            if (this.isWelshAddressAvailable(courtCentre)) {
                const welshAddress = new NowAddress();
                welshAddress.line1 = this.welshAddress1(courtCentre);
                welshAddress.line2 = this.welshAddress2(courtCentre);
                welshAddress.line3 = this.welshAddress3(courtCentre);
                welshAddress.line4 = this.welshAddress4(courtCentre);
                welshAddress.line5 = this.welshAddress5(courtCentre);
                welshAddress.postCode = this.welshPostcode(courtCentre);
                nextHearingCourtDetails.welshCourtAddress = welshAddress;
            }
            return nextHearingCourtDetails;
        }
    }

    courtName(courtCentre) {
        if (courtCentre.name) {
            return courtCentre.name;
        }
    }

    welshCourtName(courtCentre) {
        if (courtCentre.welshName) {
            return courtCentre.welshName;
        }
    }

    roomName(courtCentre) {
        if (courtCentre.roomName) {
            return courtCentre.roomName;
        }
    }

    welshRoomName(courtCentre) {
        if (courtCentre.welshRoomName) {
            return courtCentre.welshRoomName;
        }
    }

    hearingWeekCommencing(nextHearing) {
        if(nextHearing.weekCommencingDate) {
            return nextHearing.weekCommencingDate;
        }
    }

    address1(courtCentre) {
        if (courtCentre.address.address1) {
            return courtCentre.address.address1;
        }
    }

    address2(courtCentre) {
        if (courtCentre.address.address2) {
            return courtCentre.address.address2;
        }
    }

    address3(courtCentre) {
        if (courtCentre.address.address3) {
            return courtCentre.address.address3;
        }
    }

    address4(courtCentre) {
        if (courtCentre.address.address4) {
            return courtCentre.address.address4;
        }
    }

    address5(courtCentre) {
        if (courtCentre.address.address5) {
            return courtCentre.address.address5;
        }
    }

    postcode(courtCentre) {
        if (courtCentre.address.postcode) {
            return courtCentre.address.postcode;
        }
    }

    welshAddress1(courtCentre) {
        if (courtCentre.welshAddress.welshAddress1) {
            return courtCentre.welshAddress.welshAddress1;
        }
    }

    welshAddress2(courtCentre) {
        if (courtCentre.welshAddress.welshAddress2) {
            return courtCentre.welshAddress.welshAddress2;
        }
    }

    welshAddress3(courtCentre) {
        if (courtCentre.welshAddress.welshAddress3) {
            return courtCentre.welshAddress.welshAddress3;
        }
    }

    welshAddress4(courtCentre) {
        if (courtCentre.welshAddress.welshAddress4) {
            return courtCentre.welshAddress.welshAddress4;
        }
    }

    welshAddress5(courtCentre) {
        if (courtCentre.welshAddress.welshAddress5) {
            return courtCentre.welshAddress.welshAddress5;
        }
    }

    welshPostcode(courtCentre) {
        if (courtCentre.welshAddress.postcode) {
            return courtCentre.welshAddress.postcode;
        }
    }

    isAddressAvailable(courtCentre) {
        return !!courtCentre.address;

    }

    isWelshAddressAvailable(courtCentre) {
        return !!courtCentre.welshAddress;

    }

    findMatchNextHearing(results) {

        const resultsWithNextHearing = results.filter(result => result.nextHearing && !result.publishedForNows);
        const resultsWithoutNextHearing = results.filter(result => result.publishedForNows);

        const nextHearingMap = new Map();
        for(const resultWithNextHearing of resultsWithNextHearing) {
            if(resultWithNextHearing.judicialResultPrompts) {
                for (const judicialResultPrompt of resultWithNextHearing.judicialResultPrompts) {
                    nextHearingMap.set(judicialResultPrompt.promptReference, resultWithNextHearing);
                }
            }
        }

        for(const resultWithoutNextHearing of resultsWithoutNextHearing) {
            const resultWithNextHearing = nextHearingMap.get(resultWithoutNextHearing.judicialResultId);
            if(resultWithNextHearing) {
                return resultWithNextHearing;
            }
        }

        return undefined;
    }
}

module.exports = NextHearingCourtDetailsMapper;