//noinspection JSUnusedGlobalSymbols
var onRequest = function (context) {
    //Getting supported protocols
    var protocols = callOSGiService("org.wso2.carbon.andes.core.Andes", "getSupportedProtocols", []);
    var protocolStrings = [];
    for each (var item in protocols) {
        protocolStrings.push(item.toString());
    }

    //Getting subscriptions
    var destinations = [];
    var HashMap = Java.type('java.util.HashMap');
    var messageCountPerStorageQueue = new HashMap();
    var queryParamProtocol = context.request.queryParams["protocol"];
    if (queryParamProtocol != null && queryParamProtocol != "") {
        var andesResourceManager = callOSGiService("org.wso2.carbon.andes.core.Andes", "getAndesResourceManager", []);
        var protocolClass = Java.type("org.wso2.carbon.andes.core.ProtocolType");
        var protocolInstance = new protocolClass(queryParamProtocol);
        var destinationTypeEnum = Java.type("org.wso2.carbon.andes.core.DestinationType");
        destinations = andesResourceManager.getDestinations(protocolInstance, destinationTypeEnum.QUEUE, "*", 0, 1000);
        print("destinations : " + JSON.stringify(destinations));
        // Getting pending message counts for queues
        for each(var destination in destinations) {
            messageCountPerStorageQueue.put(destination.queueName, andesResourceManager.getMessageCountForStorageQueue(destination.queueName));
        }
    }
    return {"protocols" : protocolStrings,
            "destinations" : destinations,
            "messageCountPerStorageQueue" : messageCountPerStorageQueue,
           	"alert": {
           		"success": {},
           		"danger": {}
           	}
           };
};