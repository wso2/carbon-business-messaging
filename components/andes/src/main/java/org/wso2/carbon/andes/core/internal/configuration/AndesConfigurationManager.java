/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.internal.configuration;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperty;
import org.wso2.carbon.kernel.utils.Utils;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * This class acts as a singleton access point for all config parameters used within MB.
 */
public class AndesConfigurationManager {

    private static Log log = LogFactory.getLog(AndesConfigurationManager.class);

    /**
     * Reserved Suffixes that activate different processing logic.
     */
    private static final String PORT_TYPE = "_PORT";

    /**
     * Reserved Prefixes that activate different processing logic.
     */
    private static final String LIST_TYPE = "LIST_";

    /**
     * Namespace and attribute used by secure vault to identify encrypted properties.
     */
    private static final QName SECURE_VAULT_QNAME =
            new QName("http://org.wso2.securevault/configuration", "secretAlias");

    /**
     * Package which contains custom classes that can be parsed by the AndesConfigurationManager.
     */
    private static final String CONFIG_MODULE_PACKAGE = "org.wso2.andes.configuration.modules";

    /**
     * Common Error states
     */
    private static final String GENERIC_CONFIGURATION_PARSE_ERROR = "Error occurred when trying to parse " +
            "configuration value {0}.";

    private static final String NO_CHILD_FOR_KEY_IN_PROPERTY = "There was no child at the given key {0} for the " +
            "parent property {1}.";

    private static final String PROPERTY_NOT_A_LIST = "The input property {0} does not contain a list of child " +
            "properties.";

    private static final String PROPERTY_NOT_A_PORT = "The input property {0} is not defined as an integer value. " +
            "Therefore it is not a port property.";

    /**
     * File name of the main configuration file.
     */
    private static final String ROOT_CONFIG_FILE_NAME = "broker.xml";

    /**
     * Apache commons composite configuration is used to collect and maintain properties from multiple configuration
     * sources.
     */
    private static CompositeConfiguration compositeConfiguration;

    /**
     * This hashmap is used to maintain any properties that were encrypted with ciphertool. key would be the
     * secretAlias, and the value would be the decrypted value. This will be cross-referenced when reading properties
     * from broker.xml.
     */
    private static ConcurrentHashMap<String, String> cipherValueMap;

    /**
     * Decisive configurations coming from carbon.xml that affect the MB configs. e.g port Offset
     * These are injected as custom logic when reading the configurations.
     */
    private static int carbonPortOffset;

    /**
     * initialize the configuration manager. this MUST be called at application startup.
     * (QpidServiceComponent bundle -> activate event)
     *
     * @throws AndesException
     */
    public static void initialize(int portOffset) throws AndesException {

        String brokerConfigFilePath = Utils.getCarbonConfigHome() + "/" + ROOT_CONFIG_FILE_NAME;
        log.info("Main andes configuration located at : " + brokerConfigFilePath);

        try {

            compositeConfiguration = new CompositeConfiguration();
            compositeConfiguration.setDelimiterParsingDisabled(true);

            XMLConfiguration rootConfiguration = new XMLConfiguration();
            rootConfiguration.setDelimiterParsingDisabled(true);
            rootConfiguration.setFileName(brokerConfigFilePath);
            rootConfiguration.setExpressionEngine(new XPathExpressionEngine());
            rootConfiguration.load();
            compositeConfiguration.addConfiguration(rootConfiguration);

            //Decrypt and maintain secure vault property values in a map for cross-reference.
            decryptConfigurationFromFile(brokerConfigFilePath);

            // Derive certain special properties that are not simply specified in the configuration files.
            addDerivedProperties();

            // set carbonPortOffset coming from carbon
            AndesConfigurationManager.carbonPortOffset = portOffset;

            //set delivery timeout for a message
            int deliveryTimeoutForMessage = AndesConfigurationManager.readValue(
                            AndesConfiguration.PERFORMANCE_TUNING_TOPIC_MESSAGE_DELIVERY_TIMEOUT);
            AndesContext.getInstance().setDeliveryTimeoutForMessage(deliveryTimeoutForMessage);

        } catch (ConfigurationException e) {
            String error = "Error occurred when trying to construct configurations from file at path : "
                    + brokerConfigFilePath;
            log.error(error, e);
            throw new AndesException(error, e);

        } catch (UnknownHostException e) {
            String error = "Error occurred when trying to derive the bind address for messaging from configurations.";
            log.error(error, e);
            throw new AndesException(error, e);
        } catch (FileNotFoundException e) {
            String error = "Error occurred when trying to read the configuration file : " + brokerConfigFilePath;
            log.error(error, e);
            throw new AndesException(error, e);
//        } catch (JaxenException e) {
//            String error = "Error occurred when trying to process cipher text in file : " + brokerConfigFilePath;
//            log.error(error, e);
//            throw new AndesException(error, e);
        } catch (XMLStreamException e) {
            String error = "Error occurred when trying to process cipher text in file : " + brokerConfigFilePath;
            log.error(error, e);
            throw new AndesException(error, e);
        }
    }


    /**
     * The sole method exposed to everyone accessing configurations. We can use the relevant
     * enums (e.g.- config.enums.BrokerConfiguration) to pass the required property and
     * its meta information.
     *
     * @param <T>                   Expected data type of the property
     * @param configurationProperty relevant enum value (e.g.- config.enums.AndesConfiguration)
     * @return Value of config in the expected data type.
     */
    public static <T> T readValue(ConfigurationProperty configurationProperty) {

        // If the property requests a port value, we need to apply the carbon offset to it.
        if (configurationProperty.get().getName().endsWith(PORT_TYPE)) {
            return (T) readPortValue(configurationProperty);
        }

        try {
            // The cast to T is unavoidable. Even though the function returns the same data type,
            // compiler doesn't know about it. We could add the data type as a parameter,
            // but that only complicates the method call.
            return (T) deriveValidConfigurationValue(configurationProperty);
        } catch (ConfigurationException e) {

            log.error(e); // Since the descriptive message is wrapped in exception itself

            // Return the parsed default value. This path will be met if a user adds an invalid value to a property.
            // Assuming we always have proper default values defined, this will rescue us from exploding due to a
            // small mistake.
            try {
                return (T) deriveValidConfigurationValue(configurationProperty);
            } catch (ConfigurationException e1) {
                // It is highly unlikely that this will throw an exception (if defined default values are also invalid).
                // But if it does, the method will return null.
                // Exception is not propagated to avoid unnecessary clutter of config related exception handling.

                log.error(e); // Since the descriptive message is wrapped in exception itself
                return null;
            }
        }
    }

    /**
     * Using this method, you can access a singular property of a child.
     * <p/>
     * example,
     * <p/>
     * <users>
     * <user userName="testuser1">password1</user>
     * <user userName="testuser2">password2</user>
     * </users> scenario.
     *
     * @param configurationProperty relevant enum value (e.g.- above scenario -> config
     *                              .enums.AndesConfiguration.TRANSPORTS_MQTT_PASSWORD)
     * @param key                   key of the child of whom you seek the value (e.g. above scenario -> "testuser2")
     */
    public static <T> T readValueOfChildByKey(AndesConfiguration configurationProperty, String key) {

        String constructedKey = configurationProperty.get().getKeyInFile().replace("{key}",
                                                                                   key);

        // The cast to T is unavoidable. Even though the function returns the same data type,
        // compiler doesn't know about it. We could add the data type as a parameter,
        // but that only complicates the method call.
        try {
            return (T) deriveValidConfigurationValue(constructedKey,
                                                     configurationProperty.get().getDataType(),
                                                     configurationProperty.get().getDefaultValue());
        } catch (ConfigurationException e) {
            // This means that there is no child by the given key for the parent property.
            log.error(MessageFormat.format(NO_CHILD_FOR_KEY_IN_PROPERTY, key, configurationProperty), e);
            return null;
        }
    }


    /**
     * Use this method when you need to acquire a list of properties of same group.
     *
     * @param configurationProperty relevant enum value (e.g.- config.enums.AndesConfiguration
     *                              .LIST_TRANSPORTS_MQTT_USERNAMES)
     * @return String list of required property values
     */
    public static List<String> readValueList(AndesConfiguration configurationProperty) {

        if (configurationProperty.toString().startsWith(LIST_TYPE)) {
            return Arrays.asList(compositeConfiguration.getStringArray(configurationProperty.get().getKeyInFile()));
        } else {
            log.error(MessageFormat.format(PROPERTY_NOT_A_LIST, configurationProperty));
            return new ArrayList<String>();
        }
    }


    /**
     * Retrieves value from configuration file (i.e. broker.xml) for the
     * specified config-definition.
     * <p>
     * This method has the support for deprecated properties
     * </p>
     * Behavior is: <br/>
     * <ol>
     * <li>Check if the configuration is defined in file.</li>
     * <li>If configuration exists then return the value provided in file</li>
     * <li>if configuration is non-existent then check whether this
     * configuration is
     * associated with a another deprecated config parameter</li>
     * <li>If user has specified deprecated configuration it will be read.
     * </ol>
     *
     * @param <T> Expected data type of the property
     * @return Value of config in the expected data type.
     * @throws ConfigurationException an error
     */
    public static <T> T deriveValidConfigurationValue(ConfigurationProperty configurationProperty)
            throws ConfigurationException {

        if (compositeConfiguration.containsKey(configurationProperty.get().getKeyInFile())) {
            return (T) deriveValidConfigurationValue(configurationProperty.get().getKeyInFile(),
                                                     configurationProperty.get().getDataType(),
                                                     configurationProperty.get().getDefaultValue());
        } else {

            // Check whether a deprecated configuration associated with current
            // config definition.
            // If that's true; check whether the deprecated configuration is
            // defined by the user.
            if (configurationProperty.hasDeprecatedProperty() &&
                    compositeConfiguration.containsKey(configurationProperty.getDeprecated().get().getKeyInFile())) {
                // User is still using the old configuration parameter instead
                // of new one. therefore a warning will be logged.
                log.warn("configuration [" + configurationProperty.getDeprecated().get().getKeyInFile() +
                                 "] is deprecated. please use: [" +
                                 configurationProperty.get().getKeyInFile() + "]");

                return (T) deriveValidConfigurationValue(configurationProperty.getDeprecated().get().getKeyInFile(),
                                                         configurationProperty.getDeprecated().get().getDataType(),
                                                         configurationProperty.getDeprecated().get().getDefaultValue());
            } else {
                // go with the default value of original configuration parameter
                return (T) deriveValidConfigurationValue(configurationProperty.get().getKeyInFile(),
                                                         configurationProperty.get().getDataType(),
                                                         configurationProperty.get().getDefaultValue());
            }

        }

    }

    /**
     * Given the data type and the value read from a config, this returns the parsed value
     * of the property.
     *
     * @param key          The Key to the property being read (n xpath format as contained in file.)
     * @param dataType     Expected data type of the property
     * @param defaultValue This parameter should NEVER be null since we assign a default value to
     *                     every config property.
     * @param <T>          Expected data type of the property
     * @return Value of config in the expected data type.
     * @throws ConfigurationException
     */
    public static <T> T deriveValidConfigurationValue(String key, Class<T> dataType,
                                                      String defaultValue) throws ConfigurationException {

        if (log.isDebugEnabled()) {
            log.debug("Reading andes configuration value " + key);
        }

        String readValue = compositeConfiguration.getString(key);

        String validValue = defaultValue;

        // If the dataType is a Custom Config Module class, the readValue will be null (since the child properties
        // are the ones with values.). Therefore the warning is printed only in other situations.
        if (StringUtils.isBlank(readValue) && !CONFIG_MODULE_PACKAGE.equals(dataType.getPackage().getName())) {
            log.warn("Error when trying to read property : " + key + ". Switching to " + "default value : " +
                             defaultValue);
        } else {
            validValue = overrideWithDecryptedValue(key, readValue);
        }

        if (log.isDebugEnabled()) {
            log.debug("Valid value read for andes configuration property " + key + " is : " + validValue);
        }

        try {

            if (Boolean.class.equals(dataType)) {
                return dataType.cast(Boolean.parseBoolean(validValue));

            } else if (Date.class.equals(dataType)) {
                // Sample date : "Sep 28 20:29:30 JST 2000"
                DateFormat df = new SimpleDateFormat("MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
                return dataType.cast(df.parse(validValue));

            } else if (dataType.isEnum()) {
                // this will indirectly forces programmer to define enum values in upper case
                return (T) Enum.valueOf((Class<? extends Enum>) dataType, validValue.toUpperCase(Locale.ENGLISH));

            } else if (CONFIG_MODULE_PACKAGE.equals(dataType.getPackage().getName())) {
                // Custom data structures defined within this package only need the root Xpath to extract the other
                // required child properties to construct the config object.
                return dataType.getConstructor(String.class).newInstance(key);

            } else {
                return dataType.getConstructor(String.class).newInstance(validValue);
            }

        } catch (NoSuchMethodException e) {
            throw new ConfigurationException(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, key), e);
        } catch (ParseException e) {
            throw new ConfigurationException(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, key), e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, key), e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, key), e);
        } catch (InstantiationException e) {
            throw new ConfigurationException(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, key), e);
        }
    }

    /**
     * This method is used to derive certain special properties that are not simply specified in
     * the configuration files.
     */
    private static void addDerivedProperties() throws AndesException, UnknownHostException {

        // For AndesConfiguration.TRANSPORTS_MQTT_BIND_ADDRESS
        if ("*".equals(readValue(AndesConfiguration.TRANSPORTS_MQTT_BIND_ADDRESS))) {

            InetAddress host = InetAddress.getLocalHost();
            compositeConfiguration.setProperty(AndesConfiguration.TRANSPORTS_MQTT_BIND_ADDRESS.get().getKeyInFile(),
                                               host.getHostAddress());
        }

        // For AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS
        if ("*".equals(readValue(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS))) {

            InetAddress host = InetAddress.getLocalHost();
            compositeConfiguration.setProperty(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS.get().getKeyInFile(),
                                               host.getHostAddress());
        }
    }

    /**
     * This method is used when reading a port value from configuration. It is intended to abstract the port offset
     * logic.If the enum contains keyword "_PORT", this will be called
     *
     * @param configurationProperty relevant enum value (e.g.- above scenario -> config.enums.AndesConfiguration
     *                              .TRANSPORTS_MQTT_PORT)
     * @return port with carbon port offset
     */
    private static Integer readPortValue(ConfigurationProperty configurationProperty) {

        if (!Integer.class.equals(configurationProperty.get().getDataType())) {
            log.error(MessageFormat.format(AndesConfigurationManager.PROPERTY_NOT_A_PORT, configurationProperty));
            return 0; // 0 can never be a valid port. therefore, returning 0 in the error path will keep code
            // predictable.
        }

        try {
            Integer portFromConfiguration = (Integer) deriveValidConfigurationValue(configurationProperty.get()
                                                                                            .getKeyInFile(),
                                                                                    configurationProperty.get()
                                                                                            .getDataType(),
                                                                                    configurationProperty.get()
                                                                                            .getDefaultValue());

            return portFromConfiguration + carbonPortOffset;

        } catch (ConfigurationException e) {
            log.error(MessageFormat.format(GENERIC_CONFIGURATION_PARSE_ERROR, configurationProperty), e);

            //recover and return default port with offset value.
            return Integer.parseInt(configurationProperty.get().getDefaultValue()) + carbonPortOffset;
        }
    }

    /**
     * Decrypt properties with secure vault and maintain on a separate hashmap for cross-reference.
     *
     * @param filePath File path to the configuration file in question
     * @throws FileNotFoundException //     * @throws JaxenException
     * @throws XMLStreamException
     */
    private static void decryptConfigurationFromFile(String filePath)
            throws FileNotFoundException, /*JaxenException,*/ XMLStreamException {
//
        cipherValueMap = new ConcurrentHashMap<String, String>();
//
//        StAXOMBuilder stAXOMBuilder = new StAXOMBuilder(new FileInputStream(new File(filePath)));
//        OMElement dom = stAXOMBuilder.getDocumentElement();
//
//        //Initialize the SecretResolver providing the configuration element.
//        SecretResolver secretResolver = SecretResolverFactory.create(dom, false);
//
//        AXIOMXPath xpathExpression = new AXIOMXPath ("//*[@*[local-name() = 'secretAlias']]");
//        List nodeList = xpathExpression.selectNodes(dom);
//
//        for (Object o : nodeList) {
//
//            String secretAlias = ((OMElement)o).getAttributeValue(SECURE_VAULT_QNAME);
//            String decryptedValue = "";
//
//            if (secretResolver != null && secretResolver.isInitialized()) {
//                if (secretResolver.isTokenProtected(secretAlias)) {
//                    decryptedValue = secretResolver.resolve(secretAlias);
//                }
//            } else {
//                log.warn("Error while trying to decipher secure property with secretAlias : " + secretAlias);
//            }
//
//            cipherValueMap.put(secretAlias,decryptedValue);
//        }

    }

    /**
     * If the property is contained in the cipherValueMap, replace the raw value with that value.
     *
     * @param keyInFile xpath expression used to extract the value from file.
     * @param rawValue  The value read from the file without any processing.
     * @return the decrypted value if the property was encrypted with ciphertool.
     */
    private static String overrideWithDecryptedValue(String keyInFile, String rawValue) {

        if (keyInFile.contains("@")) {
            if (log.isDebugEnabled()) {
                log.debug("Ciphertool does not operate on xml attributes or lists. ( input key : " + keyInFile + " )");
            }
        }

        if (!StringUtils.isBlank(keyInFile)) {

            // The alias is inferred from the xpath of the property.
            // If the xpath = "transports/amqp/sslConnection/keyStore/password",
            // secretAlias should be "transports.amqp.sslConnection.keyStore.password"
            String secretAlias = keyInFile.replaceAll("/", ".");

            if (cipherValueMap.containsKey(secretAlias)) {
                return cipherValueMap.get(secretAlias);
            }
        }

        return rawValue;
    }

}
