'use strict'

$(document).ready(function() {
    $("#new-queue-form").submit(function(e){
        $("#modal-primary-button-id").unbind();
        e.preventDefault(); // this will prevent from submitting the form.
        $.ajax({
            type: 'POST',
            url: getBaseUrl() + "/mb/v1.0.0/amqp/destination-type/queue/name/" + $("#queue-name").val(),
            success:function(data){
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
    });
});