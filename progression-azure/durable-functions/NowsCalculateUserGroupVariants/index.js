const { default: axiosStatic } = require("axios");

module.exports = async function(context) {
  const cjscppuid = process.env.cjscppuid;
  let json = context.bindings.unfilteredJson;
  let nowMetadata = await getNowMetadata(cjscppuid, context);
  console.log("metadata json respone -->>" + JSON.stringify(nowMetadata));
};

async function getNowMetadata(cjscppuid, context) {
  const nowMetadataQueryEndpoint =
    process.env.REFERENCE_DATA_CONTEXT_API_BASE_URI +
    `/referencedata-query-api/query/api/rest/referencedata/nows-metadata`;

  context.log(`Querying ${nowMetadataQueryEndpoint}`);

  try {
    const response = await axiosStatic.get(nowMetadataQueryEndpoint, {
      headers: {
        CJSCPPUID: cjscppuid,
        Accept: "application/vnd.referencedata.get-nows-metadata+json"
      }
    });

    return response.data;
  } catch (err) {
    console.log("Error retrieving nows metadata -->>", err);
    return null;
  }
}


