function addQueue(createdFrom) {
    var topic = document.getElementById("queue");
    //exclusive consumer value
    var exclusiveValue = document.getElementById("isExclusiveConsumer");

    if(exclusiveValue.checked){
         var exclusiveConsumer = exclusiveValue.value;
         }
    else {
         var exclusiveConsumer = "";
         }
    var error = "";

    if (topic.value == "") {
        error = "Queue name cannot be empty.\n";
    } else if (!isValidQueueName(topic.value)) {
        error = "Queue name cannot contain any of following symbols ~!@#;%^*()+={}|\<>\"',\n";
    }
    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    addQueueToBackEnd(topic.value, exclusiveConsumer, createdFrom)
}

function isValidQueueName(queueName){
    return !/[~!@#;%^*()+={}|\<>"',]/g.test(queueName);
}

function addQueueToBackEnd(queue, exclusiveConsumer, createdFrom) {
    var callback =
    {
        success:function(o) {
            if (o.responseText !== undefined) {
                if (o.responseText.indexOf("Error") > -1) {
                    CARBON.showErrorDialog("" + o.responseText, function() {
                    });
                } else {
                    addPermissions();
                }

            }
        },
        failure:function(o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };

    //sending queue name with exclusive consumer
    var request = YAHOO.util.Connect.asyncRequest('POST', "add_queue_to_backend_ajaxprocessor.jsp", callback, "queue="+
                                                queue + "&exclusiveConsumer="+ exclusiveConsumer + "&type=input");
}

function addPermissions() {
    var callback =
    {
        success:function(o) {
            if (o.responseText !== undefined) {
                if (o.responseText.indexOf("Error") > -1) {
                    CARBON.showErrorDialog("" + o.responseText, function() {
                        location.href = "../queues/queue_details.jsp"
                    });
                } else {
                    CARBON.showInfoDialog("" + o.responseText, function() {
                        location.href = "../queues/queue_details.jsp"
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
    var request = YAHOO.util.Connect.asyncRequest('POST', "update_queue_role_permissions_from_session_ajaxprocessor.jsp", callback, "type=input");
}

function updatePermissions() {
        //exclusive consumer value of the queue
      var exclusiveConsumer = document.getElementById("isExclusiveConsumer");

      if(exclusiveConsumer.checked){
          var exclusiveValue = exclusiveConsumer.value;
          }
      else{
          var exclusiveValue = "";
          }

    var permissionTable = document.getElementById("permissionsTable");
    var rowCount = permissionTable.rows.length;
    var parameters = "";
    for (var i = 1; i < rowCount; i++) {
        /* since there can be special characters in roleNames we need to encode them before send the parameters to backend*/
        var roleName = encodeURIComponent(permissionTable.rows[i].cells[0].innerHTML.replace(/^\s+|\s+$/g, ""));
        var consumeAllowed = permissionTable.rows[i].cells[1].getElementsByTagName("input")[0].checked;
        var publishAllowed = permissionTable.rows[i].cells[2].getElementsByTagName("input")[0].checked;
        if (i == 1) {
            parameters = roleName + "," + consumeAllowed + "," + publishAllowed + ",";
        } else {
            parameters = parameters + roleName + "," + consumeAllowed + "," + publishAllowed + ",";
        }
    }

    var callback =
    {
        success:function(o) {
            if (o.responseText !== undefined) {
                if (o.responseText.indexOf("Error") > -1) {
                    CARBON.showErrorDialog("" + o.responseText, function() {
                        location.href = "../queues/queue_details.jsp"
                    });
                } else {
                    CARBON.showInfoDialog("" + o.responseText, function() {
                        location.href = "../queues/queue_details.jsp"
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
    //sending along with exclusive consumer value
    var request = YAHOO.util.Connect.asyncRequest('POST', "update_queue_role_permissions_ajaxprocessor.jsp", callback, "permissions=" +
                                parameters + "&isExclusiveConsumer="+ exclusiveValue + "&type=input");
}

function showManageQueueWindow(queueName) {
    var callback =
    {
        success:function(o) {
            if (o.responseText !== undefined) {
                location.href = "../queues/queue_manage.jsp";
            }
        },
        failure:function(o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "load_queue_details_from_bEnd_ajaxprocessor.jsp", callback, "queueName=" + queueName + "&type=input");
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

function doReRouteMessages(nameOfQueue) {
    var checkedValues = getCheckedValues();
    if (checkedValues == null || checkedValues == "") {
        var msg = org_wso2_carbon_andes_ui_jsi18n["info.zero.items.selected"] + " " + org_wso2_carbon_andes_ui_jsi18n["reRoute"];
        CARBON.showInfoDialog(msg);
        return;
    }
    jQuery.ajax({
        url: "../queues/queue_list_retrieve_ajaxprocessor.jsp",
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
                            url: '../queues/dlc_message_reroute_ajaxprocessor.jsp?nameOfQueue=' + nameOfQueue + '&newQueueName=' + selectedQueue + '&msgList=' + checkedValues,
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
        },
        failure: function(transport) {
            CARBON.showErrorDialog(trim(transport.responseText),function(){
                location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                return;
            });
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
        error = "Number of messages field cannot be empty. \n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error,function(){
            location.href = "../queues/queue_message_sender.jsp";
        },function(){
            location.href = "../queues/queue_message_sender.jsp";
        });
    }
}

//checking the status of the checkbox
function checkBoxStatus(queueName)
{
    var callback =
    {
        success:function(o) {
            if (o.responseText.equals("true")) // if the queue has subscribers
            {
                 if(document.getElementById("isExclusiveConsumer").checked)
                       {
                           document.getElementById("isExclusiveConsumer").checked=true;
                       }
                 else  {
                           document.getElementById("isExclusiveConsumer").checked=false;
                       }
             }
            else { // queue has no subscribers
                      if(document.getElementById("isExclusiveConsumer").checked)
                      {
                        document.getElementById("isExclusiveConsumer").checked=false;
                      }
                      else
                      {
                        document.getElementById("isExclusiveConsumer").checked=true;
                      }
                }
        },
        failure:function(o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "check_subscription_from_backend_ajaxprocessor.jsp", callback, "queueName=" + queueName + "&type=input");

}
