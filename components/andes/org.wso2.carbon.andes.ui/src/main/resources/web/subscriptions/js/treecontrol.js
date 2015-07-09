function addQueue(createdFrom) {
    var topic = document.getElementById("queue");

    var error = "";

    if (topic.value == "") {
        error = "Queue can not be empty.\n";
    } else if (!isValidQueueName(topic.value)) {
        error = "Queue can not contain any of following symbols ~!@#;%^*()+={}|\<>\"',\n";
    }
    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    addQueueToBackEnd(topic.value, createdFrom)
}

function isValidQueueName(queueName){
    return !/[~!@#;%^*()+={}|\<>"',]/g.test(queueName);
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
 
 
 function doDeleteDLC(nameOfQueue) {
	 var checkedValues = getCheckedValues();
	 if(checkedValues == null || checkedValues == "" ){
		  var msg = org_wso2_carbon_andes_ui_jsi18n["info.zero.items.selected"]+ " " + org_wso2_carbon_andes_ui_jsi18n["delete"];
		  CARBON.showInfoDialog(msg);
		  return;
	 }
	 CARBON.showConfirmationDialog(org_wso2_carbon_andes_ui_jsi18n["confirmation.delete"], function(){
		 $.ajax({
	 				url:'../queues/dlc_message_delete_ajaxprocessor.jsp?nameOfQueue=' + nameOfQueue + '&msgList=' + checkedValues,
	 				async:true,
	 				dataType:"html",
	 				success: function() {
       	                	CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.successful.delete"], function(){
       	                		location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
	                		 });
       	                   
       	                },
	
       	             failure: function(transport) {
       	                 CARBON.showErrorDialog(trim(transport.responseText),function(){
       	                	location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
       	                	 return;
	                	      });
       	                }
       	            });
			 });
}
 
 function deRestoreMessages(nameOfQueue){
	 var checkedValues = getCheckedValues();
	 if(checkedValues == null || checkedValues == ""){
		  var msg = org_wso2_carbon_andes_ui_jsi18n["info.zero.items.selected"]+ " " + org_wso2_carbon_andes_ui_jsi18n["restore"];
		  CARBON.showInfoDialog(msg);
		  return;
	 }
	 CARBON.showConfirmationDialog(org_wso2_carbon_andes_ui_jsi18n["confirmation.restore"], function(){
		 $.ajax({
	 				url:'../queues/dlc_message_restore_ajaxprocessor.jsp?nameOfQueue=' + nameOfQueue + '&msgList=' + checkedValues,
	 				async:true,
	 				dataType:"html",
	 				success: function() {
       	                	CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.successful.restore"], function(){
       	                		location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
	                		 });
       	                   
       	                },
	
       	             failure: function(transport) {
       	                 CARBON.showErrorDialog(trim(transport.responseText),function(){
       	                	location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
       	                	 return;
	                	      });
       	                }
       	            });
			 });
 }

function doReRouteMessages() {
    var checkedValues = getCheckedValues();
    if (checkedValues == null || checkedValues == "") {
        var msg = org_wso2_carbon_andes_ui_jsi18n["info.zero.items.selected"] + " " + org_wso2_carbon_andes_ui_jsi18n["reRoute"];
        CARBON.showInfoDialog(msg);
        return;
    }
    else if (nameOfQueue.contains(":")){
        CARBON.showInfoDialog("Cannot re-route from a durable subscription");
        return;
    }
    jQuery.ajax({
        url: "../queues/queue_list_retrieve_ajaxprocessor.jsp?nameOfQueue=" + nameOfQueue,
        type: "POST",
        success: function (data) {
            //Let's say data is something like the following string
            // data = "queue1#queue2";
            //data = data.split("#");
            data = jQuery.trim(data);
            console.info(data);
            data  = data.split("#");

            var selectElement = '<select id="allQueues" style="font-size: 14px; display: block; margin: 0 auto; margin-top: 10px;">';
            for (var i = 0; i < data.length; i++) {
                selectElement += '<option value="' + data[i] + '">' + data[i] + '</option>';
            }
            selectElement += '</select>';

            CARBON.showPopupDialog(selectElement, "Select a queue to route messages ", 100, true,
                function () {
                    var selectedQueue = jQuery('#allQueues').val();

                    CARBON.showConfirmationDialog(org_wso2_carbon_andes_ui_jsi18n["confirmation.reRoute"], function () {
                        $.ajax({
                            url: '../queues/dlc_message_reroute_ajaxprocessor.jsp?newQueueName=' + selectedQueue + '&msgList=' + checkedValues,
                            async: true,
                            dataType: "html",
                            success: function () {
                                CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.successful.reRoute"], function () {
                                    location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                });

                            },

                            failure: function (transport) {
                                CARBON.showErrorDialog(trim(transport.responseText), function () {
                                    location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                    return;
                                });
                            }
                        });
                    });

                } , 300
            );
        }
    });
}

 
 function getCheckedValues(){
	 	return $('input[name="checkbox"]:checked').map(
	 			function() {
	 				return this.value;
	 				}).get().join(',');
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
