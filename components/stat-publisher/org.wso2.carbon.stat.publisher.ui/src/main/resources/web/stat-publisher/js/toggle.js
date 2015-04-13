
function alertMessage(value) {


    CARBON.showInfoDialog(value);

}

function alertError(value) {


    CARBON.showErrorDialog(value);

}


function DoValidation() {

    var password = document.forms["details_form"]["password"].value;
    var username = document.forms["details_form"]["user_name"].value;
    var url_address = document.forms["details_form"]["url_address"].value;

    if (password == null || password == "" || username == null || username == "" || url_address == null || url_address == "") {
        alertError("Please make sure that all properties are filled");
        return false;
    }
    else {

        return true;
    }
}


function testServer() {

    var xmlhttp;
    var urls = document.getElementById("url_address").value;

    if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
        xmlhttp = new XMLHttpRequest();
    }
    else {// code for IE6, IE5
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            if (xmlhttp.responseText) {


                if (xmlhttp.responseText.trim() == "true") {

                    alertMessage("Successfully connected to Server");

                } else {

                    alertError("Server cannot be connected!");

                }
            } else {

                alertError(xmlhttp.responseText);

            }
        }
    };


    xmlhttp.open("GET", "/carbon/stat-publisher/test_server_ajaxprocessor.jsp?url_address=" + urls, true);
    xmlhttp.send();

}

