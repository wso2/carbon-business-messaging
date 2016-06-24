function getBaseUrl() {
    return location.protocol + "//" + location.hostname + (location.port && ":" + location.port) + "/management/";
}

function getRESTBaseUrl() {
    return location.protocol + "//" + location.hostname + (location.port && ":" + location.port) + "/" + "mb/v1.0.0/";
}