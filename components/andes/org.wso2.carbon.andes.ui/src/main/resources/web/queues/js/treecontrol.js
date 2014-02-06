function addQueue(createdFrom) {
    var topic = document.getElementById("queue");

    var error = "";

    if (topic.value == "") {
        error = "Queue can not be empty.\n";
    }
    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    addQueueToBackEnd(topic.value, createdFrom)
}
function addQueueToBackEnd(queue, createdFrom) {
    var callback =
    {
        success:function(o) {
            if (o.responseText !== undefined) {
                if (o.responseText.indexOf("Error") > -1) {
                    CARBON.showErrorDialog("" + o.responseText, function() {
                    });
                } else {
                    CARBON.showInfoDialog("" + o.responseText, function() {
                        location.href = "../queues/queue_details.jsp";
                    });
                }

            }
        },
        failure:function(o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "add_queue_to_backend_ajaxprocessor.jsp", callback, "queue=" + queue + "&type=input");

}
 function doDelete(queueName) {
        var theform = document.getElementById('deleteForm');
        theform.queueName.value = queueName;
        theform.submit();
 }

function validateForm(){
    var msg_count = document.getElementById("num_of_msgs");
    var error = "";

    if(msg_count.value == ""){
        error = "Number of messages field can not be empty. \n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error,function(){
            location.href = "../queues/queue_message_sender.jsp";
        },function(){
            location.href = "../queues/queue_message_sender.jsp";
        });
    }
}