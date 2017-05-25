package org.wso2.carbon.business.messaging.identity.connector.config;

import java.util.Collections;
import java.util.Map;

/**
 * Store connector config representation
 */
public class StoreConnectorConfig {

    private String connectorId;

    private String connectorType;

    private boolean readOnly;

    private Map<String, String> properties;

    public StoreConnectorConfig() {

    }

    public StoreConnectorConfig(String connectorId, String connectorType, boolean readOnly,
                                Map<String, String> properties) {

        this.connectorId = connectorId;
        this.connectorType = connectorType;
        this.readOnly = readOnly;
        this.properties = properties;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Map<String, String> getProperties() {

        if (properties == null) {
            return Collections.emptyMap();
        }
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
