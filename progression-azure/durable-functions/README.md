# Durable Functions

#### Prerequisites:
* [Azure Functions Core Tools](https://github.com/Azure/azure-functions-core-tools)

#### Environment Variables

The following environment variables need to be set:

| Variable                     |
|------------------------------|
| RESULTS_CONTEXT_API_BASE_URI |
| REDIS_HOST                   | 
| REDIS_KEY                    | 
| REDIS_PORT                   |
    
## Running the code

In the 'progression-azure-functions' directory:

    npm install
    func start
    
### Endpoints

When running locally, the base URI will be

    http://localhost:8080
    
|Endpoint|Description|
|--------|-----------|
|/api/LAAGetHearingHttpTrigger||
|/api/orchestrators/LAAHearingResultedPublishHandler||
