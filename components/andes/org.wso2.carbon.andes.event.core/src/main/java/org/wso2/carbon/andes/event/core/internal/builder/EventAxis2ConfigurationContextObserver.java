package org.wso2.carbon.andes.event.core.internal.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.event.core.EventBroker;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

import java.util.HashSet;
import java.util.Set;

/**
 * Axis2 configuration context for eventing
 */
public class EventAxis2ConfigurationContextObserver extends AbstractAxis2ConfigurationContextObserver {
	
    private static Log log = LogFactory.getLog(EventAxis2ConfigurationContextObserver.class);
    private EventBroker eventBroker;
    private Set<Integer> loadedTenants;

    public EventAxis2ConfigurationContextObserver() {
        this.loadedTenants = new HashSet<>();
    }

    /**
     * Initialize configuration context
     *
     * @param tenantId tenant to init config
     */
    @Override
    public void creatingConfigurationContext(int tenantId) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId, true);
            if (!this.loadedTenants.contains(tenantId)) {
                this.eventBroker.initializeTenant();
                this.loadedTenants.add(tenantId);
            }
        } catch (Exception e) {
            log.error("Error in setting tenant information", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Event broker setter method
     *
     * @param eventBroker event broker object
     */
    public void setEventBroker(EventBroker eventBroker) {
        this.eventBroker = eventBroker;
    }
    
}
