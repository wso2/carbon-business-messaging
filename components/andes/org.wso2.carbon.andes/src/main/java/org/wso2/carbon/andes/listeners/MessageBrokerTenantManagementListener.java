package org.wso2.carbon.andes.listeners;

import org.apache.log4j.Logger;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

/**
 * Tenant manager listener for WSO2 Message Broker.
 * After a management event in tenant realm has happened, it should be handled in Message Broker via this class.
 */
public class MessageBrokerTenantManagementListener implements TenantMgtListener {

    private static final Logger logger = Logger.getLogger(MessageBrokerTenantManagementListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        String tenantName = tenantInfoBean.getTenantDomain();
        String tenantOwner = tenantInfoBean.getAdmin();

        try {
            DLCQueueUtils.createDLCQueue(tenantName, tenantOwner);
        } catch (AndesException e) {
            logger.error("Error creating DLC Queue for tenant", e);
        }
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantDelete(int i) {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantRename(int i, String s, String s2) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantInitialActivation(int i) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantActivation(int i) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onTenantDeactivation(int i) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onSubscriptionPlanChange(int i, String s, String s2) throws StratosException {
        // Do Nothing
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public int getListenerOrder() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Not implemented on MessageBrokerTenantManagementListener.
     */
    @Override
    public void onPreDelete(int i) throws StratosException {
        // Do nothing
    }
}
