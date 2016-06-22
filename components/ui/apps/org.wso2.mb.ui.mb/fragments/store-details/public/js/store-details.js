$(document).ready(function(){
    $.ajax({
        type: 'GET',
        url: getBaseUrl() + "/mb/v1.0.0/information/store",
        dataType: 'json',
        success:function(data){
            $("#store-health").append(data.healthy);
        },
        error: function( req, status, err ) {
            console.log( 'something went wrong', status, err );
        }
    });
});