$(document).ready(function(){
    $.ajax({
        type: 'GET',
        url: getBaseUrl() + "/mb/v1.0.0/information/broker",
        dataType: 'json',
        success:function(data){
            console.log(data);
        },
        error: function( req, status, err ) {
            console.log( 'something went wrong', status, err );
        }
    });
});