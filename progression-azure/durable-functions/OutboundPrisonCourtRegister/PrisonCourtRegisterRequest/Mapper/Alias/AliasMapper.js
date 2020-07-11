const Alias = require('../../Model/Alias');

class AliasMapper {
    constructor(defendantAliases) {
        this.defendantAliases = defendantAliases;
    }

    build() {
        if (this.defendantAliases) {
            return this.defendantAliases.map(defendantAlias => {
                const alias = new Alias();
                alias.title = defendantAlias.title;
                alias.firstName = defendantAlias.firstName;
                alias.lastName = defendantAlias.lastName;
                alias.middleName = defendantAlias.middleName;
                return alias;
            });
        }
    }
}


module.exports = AliasMapper;