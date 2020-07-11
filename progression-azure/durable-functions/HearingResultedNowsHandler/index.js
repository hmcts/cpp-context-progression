const df = require('durable-functions');

module.exports = df.orchestrator(function* (context) {

    try {

        const output = [];

        const inputs = context.df.getInput();

        const hearingResultedObj = yield context.df.callActivity('HearingResultedCacheQuery', {
            hearingId: inputs.hearingId,
            cjscppuid: inputs.cjscppuid,
            redisClient: null,
            payloadPrefix : "INT_"
        });

        if(hearingResultedObj) {

            const complianceEnforcementArray = yield context.df.callActivity('SetComplianceEnforcement', {
                    hearingResultedObj: hearingResultedObj.hearing
                });

            const nowsVariants = yield context.df.callActivity('SetNowVariants', {
                cjscppuid: inputs.cjscppuid,
                complianceEnforcements: complianceEnforcementArray,
                hearingResultedJson: hearingResultedObj.hearing
            });

            if (nowsVariants.length) {

                const userGroupsNowVariants = yield context.df.callActivity('CloneNowsForUserGroupVariants', {
                        nowsVariants: nowsVariants
                    });

                const mdeVariants = yield context.df.callActivity('SetMDEVariants', {
                    userGroupsNowVariants: userGroupsNowVariants
                });

                const nowsVariantsSubscriptions = yield context.df.callActivity('NowsVariantsSubscriptions', {
                        cjscppuid: inputs.cjscppuid,
                        mdeVariants: mdeVariants
                    });

                const outboundNowsVariants = yield context.df.callActivity('OutboundNowsVariants', {
                    cjscppuid: inputs.cjscppuid,
                    hearingResultedObj: hearingResultedObj.hearing,
                    nowsVariantsSubscriptions: nowsVariantsSubscriptions
                });

                output.push(yield context.df.callActivity('ProcessOutboundNowVariants', {
                    cjscppuid: inputs.cjscppuid,
                    outboundNowsVariants: outboundNowsVariants
                }));

                if (complianceEnforcementArray.length) {

                    const stagingEnforcementRequests = yield context.df.callActivity('OutboundComplianceEnforcement', {
                            complianceEnforcements: complianceEnforcementArray,
                            hearingResultedJson: hearingResultedObj.hearing
                        });

                    output.push(
                        yield context.df.callActivity('ProcessOutboundComplianceEnforcement', {
                            cjscppuid: inputs.cjscppuid,
                            stagingEnforcementRequests: stagingEnforcementRequests
                        }));
                }
            }

            const courtRegister = yield context.df.callActivity('SetCourtRegister', {
                hearingResultedObj: hearingResultedObj.hearing,
                sharedTime: hearingResultedObj.sharedTime,
            });

            const courtRegisterSubscriptions = yield context.df.callActivity('CourtRegisterSubscriptions', {
                    cjscppuid: inputs.cjscppuid,
                    courtRegister: courtRegister
                });

            const outboundCourtRegister = yield context.df.callActivity('OutboundCourtRegister', {
                hearingResultedObj: hearingResultedObj.hearing,
                courtRegisterSubscriptions: courtRegisterSubscriptions
            });

            if (outboundCourtRegister) {
                output.push(yield context.df.callActivity('ProcessOutboundCourtRegister', {
                    outboundCourtRegister: outboundCourtRegister,
                    cjscppuid: inputs.cjscppuid
                }));
            }

            const informantRegisters = yield context.df.callActivity('SetInformantRegister', {
                hearingResultedObj: hearingResultedObj.hearing,
                sharedTime: hearingResultedObj.sharedTime,
            });

            const informantRegisterSubscriptions = yield context.df.callActivity('InformantRegisterSubscriptions', {
                    cjscppuid: inputs.cjscppuid,
                    informantRegisters: informantRegisters
                });

            const outboundInformantRegisters = yield context.df.callActivity('OutboundInformantRegister', {
                    hearingResultedObj: hearingResultedObj.hearing,
                    informantRegisterSubscriptions: informantRegisterSubscriptions
                });

            output.push(yield context.df.callActivity('ProcessOutboundInformantRegister', {
                cjscppuid: inputs.cjscppuid,
                outboundInformantRegisters: outboundInformantRegisters
            }));

            const prisonCourtRegisters = yield context.df.callActivity('SetPrisonCourtRegister', {
                hearingResultedObj: hearingResultedObj.hearing
            });

            if (prisonCourtRegisters.length) {
                const prisonCourtRegisterSubscriptions = yield context.df.callActivity('PrisonCourtRegisterSubscriptions', {
                        cjscppuid: inputs.cjscppuid,
                        prisonCourtRegisters: prisonCourtRegisters
                    });

                const outboundPrisonCourtRegisters = yield context.df.callActivity('OutboundPrisonCourtRegister', {
                        cjscppuid: inputs.cjscppuid,
                        hearingResultedObj: hearingResultedObj.hearing,
                        prisonCourtRegisterSubscriptions: prisonCourtRegisterSubscriptions
                    });

                output.push(yield context.df.callActivity('ProcessOutboundPrisonCourtRegister', {
                    cjscppuid: inputs.cjscppuid,
                    outboundPrisonCourtRegisters: outboundPrisonCourtRegisters
                }));
            }

            return {
                Success: true,
                Output: output
            };

        } return {
            Success: true,
            Output: "Orchestrator not executed completely, hearing not found for id " + inputs.hearingId
        }

    } catch (err) {
        context.log.error('Error :', err);
        return {
            Success: false,
            Error: err
        };
    }
});
