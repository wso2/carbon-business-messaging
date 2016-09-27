package org.wso2.carbon.messaging.extensions.dynamicdiscovery;

import com.amazonaws.util.EC2MetadataUtils;
import org.wso2.andes.server.cluster.IpAddressRetriever;
import org.wso2.andes.server.cluster.MultipleAddressDetails;

import java.util.ArrayList;
import java.util.List;


public class AwsCluster implements IpAddressRetriever {

    private List<MultipleAddressDetails> multipleAddressDetailsList = new ArrayList<>();

    public List<MultipleAddressDetails> getLocalAddress() {

            String publicAddress = EC2MetadataUtils.getData("/latest/meta-data/public-ipv4");
        MultipleAddressDetails multipleAddressDetails = new MultipleAddressDetails("public",publicAddress);
        multipleAddressDetailsList.add(multipleAddressDetails);


        String privateAddress = EC2MetadataUtils.getPrivateIpAddress();
        MultipleAddressDetails multipleAddressDetails1 = new MultipleAddressDetails("private",privateAddress);
        multipleAddressDetailsList.add(multipleAddressDetails1);

        return multipleAddressDetailsList;

    }


}
