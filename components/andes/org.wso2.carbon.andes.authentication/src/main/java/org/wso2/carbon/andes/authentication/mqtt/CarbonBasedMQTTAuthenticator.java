package org.wso2.carbon.andes.authentication.mqtt;

import org.apache.log4j.Logger;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.wso2.carbon.andes.authentication.internal.AuthenticationServiceDataHolder;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class CarbonBasedMQTTAuthenticator implements IAuthenticator {

    private static final Logger logger = Logger.getLogger(CarbonBasedMQTTAuthenticator.class);
    
    @Override
    public boolean checkValid(String username, String password) {
       
        boolean isAuthenticated = false;
        
        try {
            int tenantId = getTenantIdOFUser(username);
            
            if (MultitenantConstants.INVALID_TENANT_ID != tenantId){
                UserRealm userRealm  = AuthenticationServiceDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId);
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                isAuthenticated = userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(username), password);
            } else {
                logger.error(String.format("access denied, unable to find a tenant for user name : %s", username));
            }
          
        } catch (UserStoreException e) {
            String errorMsg = String.format("unable to authenticate user : %s", username);
            logger.error(errorMsg, e);
        }
         
        
        return isAuthenticated;
    }

    
    
    public static int getTenantIdOFUser(String username) throws UserStoreException {
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        String domainName = MultitenantUtils.getTenantDomain(username);
        if (domainName != null) {
                TenantManager tenantManager = AuthenticationServiceDataHolder.getInstance().getRealmService().getTenantManager();
                tenantId = tenantManager.getTenantId(domainName);
        }
        return tenantId;
    }
    
    

}