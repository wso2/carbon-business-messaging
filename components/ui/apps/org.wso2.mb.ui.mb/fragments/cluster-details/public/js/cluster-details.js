$(document).ready(function(){
    $.ajax({
        type: 'GET',
        url: getBaseUrl() + "/mb/v1.0.0/information/cluster",
        dataType: 'json',
        success:function(data){
            $("#node-id").append(data.nodeID);
            if (data.isClusteringEnabled) {
                $("#cluster-mode").show();
                $("#standalone-mode").hide();
            } else {
                $("#cluster-mode").hide();
                $("#standalone-mode").show();
            }

            $.each(data.nodeAddresses, function(i, node) {
                $("#cluster-details").find('tbody')
                                .append($('<tr>')
                                    .append($('<td>')
                                        .append(node.nodeID)
                                    )
                                    .append($('<td>')
                                        .append(node.hostname)
                                    )
                                    .append($('<td>')
                                        .append(node.port)
                                    )
                                    .append($('<td>')
                                        .append(node.isCoordinator ? "Yes" : "No")
                                    )
                                );
            });
        },
        error: function( req, status, err ) {
            console.log( 'something went wrong', status, err );
        }
    });
});