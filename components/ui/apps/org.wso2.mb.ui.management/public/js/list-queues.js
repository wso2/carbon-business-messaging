'use strict'

var table;

$(document).ready(function() {
//    getQueuesToTable();
//    table.on('length.dt', function (e, settings, len) {
//        getQueuesToTable();
//    } );
});

/**
 * Sets data to queue list table.
 */
function getQueuesToTable() {
  table = $("#queue-list-table").DataTable( {
    destroy : true,
    "dom": '<"top">rt<"bottom"flpi><"clear">',
    "searching": false,
    "bSort" : false,
    "infoCallback": function(settings, start, end, max, total, pre){
          return "Showing " + start + " to " + end + " of " + total + " entries"
             + ((total !== max) ? " (filtered from " + max + " total entries)" : "");
    },
    "fnRowCallback": function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
          var index = iDisplayIndexFull + 1;
          $('td:eq(0)', nRow).html(index);
          return nRow;
    },
    "ajax": {
            "url": getBaseUrl() + "/mb/v1.0.0/amqp/destination-type/queue?limit=" + getLimitValue() ,
            "dataSrc": ""
        },
    columns: [
        { "data" : "id" },
        { "data": "destinationName" },
        { "data": "messageCount" },
        { "data": "subscriptionCount" },
        { "data": "owner" }
    ],
    "columnDefs": [
        {
          "render": function ( data, type, row ) {
              var queueName = row.destinationName;
              return getPurgeAndDeleteButtons(queueName);
          },
          // 5 = 6th column
          "targets": 5
        }
    ]
  });
}

function getTotalNumberOfQueues() {
    $.ajax({
        type: 'GET',
        url: getBaseUrl() + "/mb/v1.0.0/amqp/destination-type/queue?limit=1000",
        success:function(data) {
            $("#danger-alert-id").hide();
            $("#modal-message-id").text("Queue '" + $("#queue-name").val() + "' was successfully created.");
            $("#modal-primary-button-id").text("Go to Queue List");
            $("#modal-primary-button-id").click(function() {
                window.location.href = "/mb/queues";
            });
            $("#modal-primary-button-id").focus();
            $("#modal-id").modal('show');
            $("#queue-name").val("");
        },
        error: function( req, status, err ) {
            var cause = req.responseText.split(":");
            $("#danger-alert-message").text("Error creating queue : " + cause[1]);
            $("#danger-alert-id").show();
            $("#modal-id").modal('hide');
        }
    });

}

/**
 * Get limit value for searching.
 */
function getLimitValue() {
    var dtDropdownValue = $("#queue-list-table_length option:selected").val();
    if (dtDropdownValue === undefined || dtDropdownValue === null) {
         return getDefaultTableLimit();
    }

    return dtDropdownValue;
}

/**
 * Gets the template for purge and delete buttons.
 */
function getPurgeAndDeleteButtons(queueName) {
    return `<a href="#" onclick="showPurgeConfirmation(\'` + queueName + `\');" data-click-event="remove-form"
    class="btn padding-reduce-on-grid-view">
    <span class="fw-stack">
        <i class="fw fw-ring fw-stack-2x"></i>
        <i class="fw fw-delete fw-stack-1x"></i>
    </span>
    <span class="hidden-xs">Purge</span></a><a href="#"
    onclick="showDeleteConfirmation(\'` + queueName + `\');" data-click-event="remove-form"
    class="btn padding-reduce-on-grid-view">
    <span class="fw-stack">
        <i class="fw fw-ring fw-stack-2x"></i>
        <i class="fw fw-delete fw-stack-1x"></i>
    </span>
    <span class="hidden-xs">Delete</span></a>`;
}

/**
 * Shows the purge confirmation modal.
 */
function showPurgeConfirmation(queueName) {
    $("#modal-primary-button-id").unbind();
    $("#success-alert-id").hide();
    $("#danger-alert-id").hide();
    $("#modal-message-id").text("Are you sure you want to purge '" + queueName +"' queue ?");
    $("#modal-primary-button-text").text("Purge");

    // Binding purge ok click
    $("#modal-primary-button-id").click(function() {
        purgeQueue(queueName);
    });

    // Showing the modal at the end.
    $("#modal-id").modal("show");
}

/**
 * Shows the delete confirmation modal.
 */
function showDeleteConfirmation(queueName) {
    $("#modal-primary-button-id").unbind();
    $("#success-alert-id").hide();
    $("#danger-alert-id").hide();
    $("#modal-message-id").text("Are you sure you want to delete '" + queueName +"' queue ?");
    $("#modal-primary-button-text").text("Delete");

    // Binding purge ok click
    $("#modal-primary-button-id").click(function() {
        deleteQueue(queueName);
    });

    // Showing the modal at the end.
    $("#modal-id").modal("show");
}

/**
 * Purge a queue.
 */
function purgeQueue(queueName) {
    $.ajax({
            type: 'DELETE',
            url: getBaseUrl() + "/mb/v1.0.0/amqp/destination-type/queue/name/" + queueName + "/messages",
            dataType: 'json',
            success:function() {
                $("#success-alert-id").show();
                $("#success-alert-message").text("Queue '" + queueName + "' was successfully purged.");
                $("#modal-id").modal('hide');
            },
            error: function(req, status, err) {
                var cause = req.responseText.split(":");
                $("#danger-alert-message").text("Error purging queue '" + queueName + "'. : " + cause);
                $("#danger-alert-id").show();
                $("#modal-id").modal('hide');
            },
            complete: function(data) {
                table.ajax.reload();
            }
    });
}

/**
 * Delete a queue.
 */
function deleteQueue(queueName) {
    $.ajax({
            type: 'DELETE',
            url: getBaseUrl() + "/mb/v1.0.0/amqp/destination-type/queue/name/" + queueName,
            dataType: 'json',
            success:function() {
                $("#success-alert-id").show();
                $("#success-alert-message").text("Queue '" + queueName + "' was successfully deleted.");
                getQueuesToTable();
                $("#modal-id").modal('hide');
            },
            error: function(req, status, err) {
                var cause = req.responseText.split(":");
                $("#danger-alert-message").text("Error deleting queue '" + queueName + "'. : " + cause);
                $("#danger-alert-id").show();
                $("#modal-id").modal('hide');
            },
            complete: function(data) {
                table.ajax.reload();
            }
    });
}
