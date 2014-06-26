package org.wso2.carbon.andes.admin.internal;

public class QueueRolePermission {

    private String roleName;
    private boolean isAllowedToConsume;
    private boolean isAllowedToPublish;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isAllowedToConsume() {
        return isAllowedToConsume;
    }

    public void setAllowedToConsume(boolean isAllowedToConsume) {
        this.isAllowedToConsume = isAllowedToConsume;
    }

    public boolean isAllowedToPublish() {
        return isAllowedToPublish;
    }

    public void setAllowedToPublish(boolean isAllowedToPublish) {
        this.isAllowedToPublish = isAllowedToPublish;
    }
}
