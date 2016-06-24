//noinspection JSUnusedGlobalSymbols
var onRequest = function () {
    //Getting supported protocols
    var protocols = callOSGiService("org.wso2.carbon.andes.core.Andes", "getSupportedProtocols", []);
    var protocolStrings = [];
    for each (var item in protocols) {
        protocolStrings.push(item.toString());
    }

    return {"protocols" : protocolStrings,
                "alert": {
                    "danger": {}
                }
            };
};