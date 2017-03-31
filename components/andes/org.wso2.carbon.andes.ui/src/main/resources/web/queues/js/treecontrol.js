/**
 * Adding queues, when creating queues from the user interface
 */
function addQueue(createdFrom) {
    var queue = document.getElementById("queue");

    var error = "";

    if ("" == queue.value) {
        error = "Queue name cannot be empty.\n";
    } else if (!isValidQueueName(queue.value)) {
        error = "Queue name cannot contain any of following symbols ~!@#;%^*()+={}|\<>\"'/, and space \n";
    } else if (isContainTmpPrefix(queue.value)) {
        error = "Queue name cannot start with tmp_ prefix.\n";
    }
    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    addQueueAndAssignPermissions(queue.value, createdFrom);
}

/**
 * Validating queue names, when creating queues from user interface
 */
function isValidQueueName(queueName){
    return !/[~!@#;%^*()+={}|\<>"'/,\s]/g.test(queueName);
}

function isContainTmpPrefix(queueName) {
    return queueName.startsWith("tmp_");
}

function addQueueAndAssignPermissions(queue, createdFrom) {
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
    var request = YAHOO.util.Connect.asyncRequest('POST', "add_queue_and_assign_role_permissions_ajaxprocessor.jsp", callback, "queue=" + encodeURIComponent(queue) + "&type=input");
}

function updatePermissions() {
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
    var request = YAHOO.util.Connect.asyncRequest('POST', "update_queue_role_permissions_ajaxprocessor.jsp", callback, "permissions=" + parameters + "&type=input");
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
    var request = YAHOO.util.Connect.asyncRequest('POST', "load_queue_details_from_bEnd_ajaxprocessor.jsp", callback, "queueName=" + encodeURIComponent(queueName) + "&type=input");
}

function doDelete(queueName) {

    CARBON.showConfirmationDialog("Are you sure you want to delete " + queueName + " ?", function() {
        $.ajax({
            url: '../queues/queue_delete_ajaxprocessor.jsp?nameOfQueue=' + queueName,
            async: true,
            type: "POST",
            success: function(o) {
                if (o.indexOf("Error") > -1) {
                    CARBON.showErrorDialog("" + o, function() {
                        location.href = 'queue_details.jsp';
                    });
                } else {
                    CARBON.showInfoDialog('Queue ' + queueName + ' successfully deleted.', function() {
                        location.href = 'queue_details.jsp';
                    });
                }
            },
            failure: function(o) {
                if (o.responseText !== undefined) {
                    alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
                }
            }
        });
    });

}

function doPurge(queueName) {

    CARBON.showConfirmationDialog("Are you sure you want to purge " + queueName + " ?", function() {
        $.ajax({
            url: '../queues/queue_purge_ajaxprocessor.jsp?nameOfQueue=' + queueName,
            async: true,
            type: "POST",
            success: function(o) {
                if (o.indexOf("Error") > -1) {
                    CARBON.showErrorDialog('<%=e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>', function() {
                        location.href = 'queue_details.jsp';
                    });
                } else {
                    CARBON.showInfoDialog('Queue ' + queueName + ' successfully purged.', function() {
                        location.href = 'queue_details.jsp';
                    });
                }
            },
            failure: function(o) {
                if (o.responseText !== undefined) {
                    alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
                }
            }
        });
    });

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
	 				url:'../queues/dlc_message_delete_ajaxprocessor.jsp?nameOfQueue=' + nameOfQueue,
	 				async:true,
	 				dataType:"html",
	 				data : { msgList : checkedValues },
	 				type: "POST",
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
	 				url:'../queues/dlc_message_restore_ajaxprocessor.jsp?nameOfQueue=' + nameOfQueue,
	 				async:true,
	 				dataType:"html",
	 				data : { msgList : checkedValues },
	 				type: "POST",
	 				success: function(data) {
       	                	data = data.trim();
       	                	var unavailableMessageCount = parseFloat(data);
       	                	if(unavailableMessageCount > 0) {
                                if(unavailableMessageCount == checkedValues.split(",").length) {
                                    CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.fail.restore"],
                                    function(){
                                        location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                     });
                                }else {
                                     CARBON.showInfoDialog(unavailableMessageCount + " " +org_wso2_carbon_andes_ui_jsi18n[
                                     "info.partial.successful.restore"],function(){
                                        location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                     });
                                }
       	                	}else {
       	                	    CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.successful.restore"], function(){
                                       	location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                });
       	                	}
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
        url: "../queues/queue_list_retrieve_ajaxprocessor.jsp?nameOfQueue=" + nameOfQueue,
        type: "POST",
        success: function (data) {
            //Let's say data is something like the following string
            // data = "queue1#queue2";
            //data = data.split("#");
            data = jQuery.trim(data);
            data  = data.split("#");

            var selectElement = '<select id="allQueues" style="font-size: 14px; display: block; margin: 0 auto; margin-top: 10px;">';
            for (var i = 0; i < data.length; i++) {
                selectElement += '<option value="' + data[i] + '">' + data[i] + '</option>';
            }
            selectElement += '</select>';

            CARBON.showPopupDialog("", "Select a queue to route messages ", 100, true,
                function () {
                    var selectedQueue = jQuery('#allQueues').val();

                    CARBON.showConfirmationDialog(org_wso2_carbon_andes_ui_jsi18n["confirmation.reRoute"], function () {
                        $.ajax({
                            url: '../queues/dlc_message_reroute_ajaxprocessor.jsp',
                            async: true,
                            dataType: "html",
                            data : {    nameOfQueue : nameOfQueue,
                                        newQueueName : selectedQueue,
                                        msgList : checkedValues },
                            type: "POST",
                            success: function (data) {
                                data = data.trim();
                                var unavailableMessageCount = parseFloat(data);
                                if(unavailableMessageCount > 0) {
                                       if(unavailableMessageCount == checkedValues.split(",").length) {
                                            CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.fail.reRoute"],
                                            function(){
                                                location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                             });
                                       }else {
                                            CARBON.showInfoDialog(unavailableMessageCount + " "
                                            + org_wso2_carbon_andes_ui_jsi18n["info.partial.successful.reRoute"],
                                            function(){
                                                location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                            });
                                       }
                                }else {
                                    CARBON.showInfoDialog(org_wso2_carbon_andes_ui_jsi18n["info.successful.reRoute"],
                                    function(){
                                        location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                                    });
                                }
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
            $("#popupDialog").after(selectElement);
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

/**
 * Removes the 'first' and 'last' links in pagination.
 */
function removeFirstAndLastPaginations(){
    $('#workArea').find('table:not([class]) a').each(function () {
        $(this).text($(this).text().replace(/<<first/g, ""));
    });
    $('#workArea').find('table:not([class]) a').each(function () {
        $(this).text($(this).text().replace(/last.>>/g, ""));
    });
    $('#workArea').find('table:not([class]) span').each(function () {
        $(this).text($(this).text().replace(/last>>/g, ""));
    });
    $('#workArea').find('table:not([class]) span').each(function () {
        $(this).text($(this).text().replace(/<<.first/g, ""));
    });
}

/**
 * Gets all the queues in MB.
 * @returns An array of queue names.
 */
function getAllQueues() {
    var queueList = {};
    jQuery.ajax({
        url: "../queues/queue_list_retrieve_ajaxprocessor.jsp?nameOfQueue=" + nameOfQueue,
        type: "POST",
        async: false,
        success: function (data) {
            data = jQuery.trim(data);
            queueList = data.split("#");
        },
        failure: function (transport) {
            CARBON.showErrorDialog(trim(transport.responseText), function () {
                location.href = "../queues/dlc_messages_list.jsp?nameOfQueue=" + nameOfQueue;
                return queueList;
            });
        }
    });

    return queueList;
}
