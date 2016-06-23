var onRequest = function (context) {
    //Getting supported protocols
    var protocols = callOSGiService("org.wso2.carbon.andes.core.Andes", "getSupportedProtocols", []);
    var protocolStrings = [];
    for each (var item in protocols) {
        protocolStrings.push(item.toString());
    }

    //Getting subscriptions
    var subscriptions = [];
    var messageCountPerSubscription = [];
    var queryParamProtocol = context.request.queryParams["protocol"];
    var queryActiveFilter = context.request.queryParams["active"];
    if (queryParamProtocol != null && queryParamProtocol != "") {
        var andesResourceManager = callOSGiService("org.wso2.carbon.andes.core.Andes", "getAndesResourceManager", []);
        var protocolClass = Java.type("org.wso2.carbon.andes.core.ProtocolType");
        var protocolInstance = new protocolClass(queryParamProtocol);
        var destinationTypeEnum = Java.type("org.wso2.carbon.andes.core.DestinationType");
        subscriptions = andesResourceManager.getSubscriptions(protocolInstance, destinationTypeEnum.QUEUE, "*", "*", queryActiveFilter, 0, 1000);
        // Getting pending message counts for queues
        for each(var subscription in subscriptions) {
            messageCountPerSubscription.push(andesResourceManager.getMessageCountForStorageQueue(subscription.storageQueueName));
            print(subscription.storageQueueName + " : " + messageCountPerSubscription);
        }
    }

    // Returning content
    return {"protocols" : protocolStrings,
            "subscriptions" : subscriptions,
            "messageCountPerSubscription" : messageCountPerSubscription};
};
