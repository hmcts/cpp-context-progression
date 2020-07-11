class NowDistribution {
    constructor() {
        this.firstClassLetter = undefined;
        this.email = undefined;
        this.emailTemplateName = undefined;
        this.bilingualEmailTemplateName = undefined;
        this.emailContent = undefined;
        this.secondClassLetter = undefined;
    }
}

class EmailRenderingVocabulary {
    constructor(label, value) {
        this.label = label;
        this.value = value;
    }
}

module.exports = { NowDistribution, EmailRenderingVocabulary };