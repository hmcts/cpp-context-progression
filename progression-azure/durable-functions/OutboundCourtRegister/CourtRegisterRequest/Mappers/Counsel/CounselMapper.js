const Counsel = require('../../Models/Counsel');

class CounselMapper {
    constructor(counsels) {
        this.counsels = counsels;
    }

    build() {
        if (this.counsels && this.counsels.length) {
            return this.counsels.map((counselInfo) => {
                const counsel = new Counsel();
                counsel.name = [counselInfo.firstName, counselInfo.middleName, counselInfo.lastName].filter(item => item).join(' ').trim();
                counsel.status = counselInfo.status;
                return counsel;
            });
        }
    }
}

module.exports = CounselMapper;
