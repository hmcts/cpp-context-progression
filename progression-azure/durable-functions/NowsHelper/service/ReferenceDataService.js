const axios = require('axios');

class ReferenceDataService {

    async getNowMetadata(context, on) {
        if(on === undefined) {
            on = new Date().toISOString().slice(0, 10);
        }

        const orderDate = new Date(on).toISOString().slice(0, 10);

        const nowMetadataQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + '/referencedata-query-api/query/api/rest/referencedata/nows-metadata?on='+orderDate;

        try {
            const response = await axios.get(nowMetadataQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.referencedata.get-nows-metadata+json'
                }
            });
            return response.data;
        } catch (err) {
            context.log.error('Error retrieving nows metadata -->>', err);
            return null;
        }
    }

    async getSubscriptionsMetadata(context, on) {
        if(on === undefined) {
            on = new Date().toISOString().slice(0, 10);
        }

        const orderDate = new Date(on).toISOString().slice(0, 10);

        const nowSubscriptionsMetadataQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + '/referencedata-query-api/query/api/rest/referencedata/now-subscriptions?on='+orderDate;
        try {
            const response = await axios.get(nowSubscriptionsMetadataQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.referencedata.query.get-now-subscriptions+json'
                }
            });
            return response.data;
        } catch (err) {
            context.log.error('Error retrieving nows subscription metadata -->>', err);
            return null;
        }
    }

    async getOrganisationUnit(id, context) {

        const organisationUnitsQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + `/referencedata-query-api/query/api/rest/referencedata/organisation-units/${id}`;

        context.log('getOrganisationUnit :' +organisationUnitsQueryEndpoint+ ' : ' + context.bindings.params.cjscppuid);

        try {
            const response = await axios.get(organisationUnitsQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.referencedata.query.organisation-unit.v2+json'
                }
            });
            return response.data;
        } catch (err) {
            context.log.error('Error retrieving organisation units -->>', err);
            return null;
        }
    }

    async getEnforcementAreaByPostcode(postcode, context) {

        const enforcementAreaQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + `/referencedata-query-api/query/api/rest/referencedata/enforcement-area?postcode=${postcode}`;

        context.log('getEnforcementAreaByPostcode :' +enforcementAreaQueryEndpoint + ' : ' + context.bindings.params.cjscppuid);

        try {
            const response = await axios.get(enforcementAreaQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.referencedata.query.enforcement-area+json'
                }
            });

            return response.data;
        } catch (err) {
            context.log.error('Error retrieving enforcement area '+ postcode +' -->>' + err);
            return null;
        }
    }

    async getEnforcementAreaByLja(ljaCode, context) {
        const enforcementAreaQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + `/referencedata-query-api/query/api/rest/referencedata/enforcement-area?localJusticeAreaNationalCourtCode=${ljaCode}`;


        context.log('getEnforcementArea :' +enforcementAreaQueryEndpoint + ' ' + context.bindings.params.cjscppuid);

        try {
            const response = await axios.get(enforcementAreaQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.referencedata.query.enforcement-area+json'
                }
            });
            return response.data;
        } catch (err) {
            context.log.error('Error retrieving enforcement area '+ ljaCode + ' -->>', err);
            return null;
        }
    }

    async getPrisonsCustodySuites(context) {
        const prisonsCustodySuitesQueryEndpoint = process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI + '/referencedata-query-api/query/api/rest/referencedata/prisons-custody-suites';

        context.log('getPrisonsCustodySuites :' +prisonsCustodySuitesQueryEndpoint + ' ' + context.bindings.params.cjscppuid);

        try {
            const response = await axios.get(prisonsCustodySuitesQueryEndpoint, {
                headers: {
                    CJSCPPUID: context.bindings.params.cjscppuid,
                    Accept: 'application/vnd.reference-data.prisons-custody-suites+json'
                }
            });
            return response.data;
        } catch (err) {
            context.log.error('Error retrieving prisons custody suites ', err);
            return null;
        }
    }
}

module.exports = ReferenceDataService;
