var onRequest = function (context) {
    //Getting supported protocols
    var protocols = callOSGiService("org.wso2.carbon.andes.core.Andes", "getSupportedProtocols", []);
    var protocolStrings = [];
    for each (var item in protocols) {
        protocolStrings.push(item.toString());
    }

    //Getting subscriptions
    var activeDurableSubscriptions = [];
    var inactiveDurableSubscriptions = [];
    var HashMap = Java.type('java.util.HashMap');
    var messageCountPerStorageQueue = new HashMap();
    var queryParamProtocol = context.request.queryParams["protocol"];
    if (queryParamProtocol != null && queryParamProtocol != "") {
        var andesResourceManager = callOSGiService("org.wso2.carbon.andes.core.Andes", "getAndesResourceManager", []);
        var protocolClass = Java.type("org.wso2.carbon.andes.core.ProtocolType");
        var protocolInstance = new protocolClass(queryParamProtocol);
        var destinationTypeEnum = Java.type("org.wso2.carbon.andes.core.DestinationType");
        activeDurableSubscriptions = andesResourceManager.getSubscriptions(protocolInstance, destinationTypeEnum.DURABLE_TOPIC, "*", "*", true, 0, 1000);
        // Getting pending message counts for queues
        for each(var subscription in activeDurableSubscriptions) {
            messageCountPerStorageQueue.put(subscription.storageQueueName, andesResourceManager.getMessageCountForStorageQueue(subscription.storageQueueName));
        }
        inactiveDurableSubscriptions = andesResourceManager.getSubscriptions(protocolInstance, destinationTypeEnum.DURABLE_TOPIC, "*", "*", false, 0, 1000);
        // Getting pending message counts for queues
        for each(var subscription in inactiveDurableSubscriptions) {
            messageCountPerStorageQueue.put(subscription.storageQueueName, andesResourceManager.getMessageCountForStorageQueue(subscription.storageQueueName));
        }
    }

    print("MAP : " + messageCountPerStorageQueue);
    // Returning content
    return {"protocols" : protocolStrings,
            "activeDurableSubscriptions" : activeDurableSubscriptions,
            "inactiveDurableSubscriptions" : inactiveDurableSubscriptions,
            "messageCountPerStorageQueue" : messageCountPerStorageQueue};
};
