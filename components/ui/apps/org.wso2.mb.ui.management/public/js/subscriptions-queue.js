$(document).ready(function() {
    $("#active-subscription-filter").val(getParameterByName("active"));

    $('#active-subscription-filter').change(function(){
        var activeFilter = $("#active-subscription-filter option:selected").val();
        var redirectURL = window.location.href.split('?')[0] + "?protocol=" + getParameterByName("protocol") + "&active=" + activeFilter;
        window.location.href = redirectURL;
    })
});

function getParameterByName(name) {
    var url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"), results = regex.exec(url);
    if (!results) {
        return null;
    }
    if (!results[2]) {
        return '';
    }
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}