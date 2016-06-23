//noinspection JSUnusedGlobalSymbols
var onRequest = function () {
    var coordinatorNodeAddress = callOSGiService("org.wso2.carbon.andes.core.Andes", "getCoordinatorNodeAddress", []);
    var nodeAddressesAsStrings = callOSGiService("org.wso2.carbon.andes.core.Andes", "getAllClusterNodeAddresses", []);
    var nodeAddresses = [];
    var counter = 0;
    for each(var nodeAddressAsString in nodeAddressesAsStrings) {
        var nodeDetails = nodeAddressAsString.split(",");=
        if (nodeDetails[1] == coordinatorNodeAddress.split(",")[0] && nodeDetails[2] == coordinatorNodeAddress.split(",")[1]) {
            nodeDetails.push("Yes");
        } else {
            nodeDetails.push("No");
        }
        nodeAddresses[counter] = nodeDetails;
        counter++;
    }

    return {"isClusteringEnabled" : callOSGiService("org.wso2.carbon.andes.core.Andes", "isClusteringEnabled", []),
            "nodeID" : callOSGiService("org.wso2.carbon.andes.core.Andes", "getLocalNodeID", []),
             "coordinatorNodeAddress" : coordinatorNodeAddress,
             "nodeAddresses" : nodeAddresses,
             "storeHealth" : callOSGiService("org.wso2.carbon.andes.core.Andes", "getStoreHealth", []),
             "brokerDetails" : callOSGiService("org.wso2.carbon.andes.core.Andes", "getBrokerDetails", [])};
};