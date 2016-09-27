package org.wso2.carbon.messaging.extensions.dynamicdiscovery;


import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.andes.server.cluster.IpAddressRetriever;


/**
 * @scr.component
 *              name="org.wso2.carbon.extensions.dynamicdiscovery.ActivatorDynamicDiscovery"
 *              immediate="true"
 */
public class ActivatorDynamicDisvovery {

    private IpAddressRetriever ipAddressRetriever;
    protected final Logger logger = LoggerFactory.getLogger(ActivatorDynamicDisvovery.class);


    protected void activate(ComponentContext ctx) {


            AwsCluster awsCluster = new AwsCluster();
            ctx.getBundleContext().registerService(IpAddressRetriever.class.getName(),awsCluster,null);

    }



    /**
     * Unregister OSGi services that were registered at the time of activation
     * @param ctx component context
     * @throws BundleException
     */
    protected void deactivate(ComponentContext ctx) throws BundleException {
            ctx.getBundleContext().getBundle().stop();

    }

}
