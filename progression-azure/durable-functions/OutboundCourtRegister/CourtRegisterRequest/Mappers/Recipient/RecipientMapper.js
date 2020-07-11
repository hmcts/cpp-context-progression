const Recipient = require('../../Models/Recipient');

class RecipientMapper {
    constructor(context, courtRegisterFragment) {
        this.context = context;
        this.courtRegisterFragment = courtRegisterFragment;
    }

    build() {
        const recipients = [];
        if (this.courtRegisterFragment && this.courtRegisterFragment.matchedSubscriptions) {
            this.courtRegisterFragment.matchedSubscriptions.map((subscription) => {
                if (subscription.emailDelivery && subscription.forDistribution && subscription.recipient) {
                    const recipient = new Recipient();
                    recipient.recipientName = subscription.recipient.organisationName;
                    recipient.emailAddress1 = subscription.recipient.emailAddress1;
                    recipient.emailAddress2 =
                        subscription.recipient.emailAddress2 ? subscription.recipient.emailAddress2
                                                             : undefined;
                    recipient.emailTemplateName =
                        subscription.emailTemplateName ? subscription.emailTemplateName
                                                       : 'cr_standard';

                    if (recipient.emailAddress1 !== undefined) {
                        recipients.push(recipient);
                    }
                }
            });
        }

        if (recipients.length) {
            return recipients;
        }
    }
}

module.exports = RecipientMapper;
