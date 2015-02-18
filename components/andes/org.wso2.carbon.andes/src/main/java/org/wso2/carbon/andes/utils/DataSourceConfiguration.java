/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.andes.configuration.StoreConfiguration;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * <h1>Extract data source configurations from given XML descriptor</h1>
 * This class contain methods to read data source configurations from given xml source.
 * It will also populate these configurations to a hash map as key value pair for later
 * use when required.
 */
public class DataSourceConfiguration {

    /**
     * log variable for logging.
     */
    private static final Log log =
            LogFactory.getLog(DataSourceConfiguration.class);
    /**
     * xpath instance to traverse through given xml.
     */
    XPath xPath = XPathFactory.newInstance().newXPath();
    /**
     * hash map to hold db configurations as key value pairs.
     */
    private HashMap<Object, String> messageStoreConfiguration =
            new HashMap<Object, String>();

    private HashMap<Object, String> contextStoreConfiguration =
            new HashMap<Object, String>();

    /**
     * This method will populate data source configurations by reading a XML file at the given
     * location. First it'll get the jndi configuration name from andes context instance and
     * compare that configuration name against given rdbms data sources.
     * Once relevant configuration node found it will call addToConfigurationMap method to
     * store configurations in hash maps.
     *
     * @param filePath Path of the XML descriptor file
     * @throws org.wso2.carbon.andes.service.exception.ConfigurationException
     * throws exception if error occurs while reading the XML descriptor.
     */
    public void loadDbConfiguration(String filePath) throws ConfigurationException {

        String[] dataSourceNameArray = new String[2];

        dataSourceNameArray[MessageBrokerDBUtil.MESSAGE_STORE_DATA_SOURCE] = AndesContext.
                getInstance().getStoreConfiguration().getMessageStoreProperties().
                getProperty(StoreConfiguration.DATA_SOURCE);

        dataSourceNameArray[MessageBrokerDBUtil.CONTEXT_STORE_DATA_SOURCE] = AndesContext.
                getInstance().getStoreConfiguration().getContextStoreProperties().
                getProperty(StoreConfiguration.DATA_SOURCE);

        // if both data source configurations are equal only one data source configuration
        // will sourced to database
        if(dataSourceNameArray[MessageBrokerDBUtil.MESSAGE_STORE_DATA_SOURCE].
                equals(dataSourceNameArray[MessageBrokerDBUtil.CONTEXT_STORE_DATA_SOURCE])) {
            dataSourceNameArray[MessageBrokerDBUtil.CONTEXT_STORE_DATA_SOURCE] = "";
        }

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder;

            if (log.isDebugEnabled()) {
                log.debug("load database configurations from data source xml file." +
                          " file path : " + filePath);
            }

            try {
                builder = builderFactory.newDocumentBuilder();

                Document xmlDocument = builder.parse(new FileInputStream(filePath));

                if(!dataSourceNameArray[MessageBrokerDBUtil.MESSAGE_STORE_DATA_SOURCE].isEmpty()) {
                    String xpathQuery = "//datasources-configuration/datasources/datasource/"+
                                        "definition/configuration[ ../../jndiConfig/name/text()='"+
                                        dataSourceNameArray[MessageBrokerDBUtil.
                                                MESSAGE_STORE_DATA_SOURCE] +
                                        "' and ../@type='RDBMS']";
                    //read an xml node using xpath
                    Node configurationNode = (Node) xPath.compile(xpathQuery).
                            evaluate(xmlDocument, XPathConstants.NODE);

                    addToConfigurationMap(configurationNode, messageStoreConfiguration);
                }

                if(!dataSourceNameArray[MessageBrokerDBUtil.CONTEXT_STORE_DATA_SOURCE].isEmpty()) {
                    String xpathQuery = "//datasources-configuration/datasources/datasource/"+
                                        "definition/configuration[ ../../jndiConfig/name/text()='"+
                                        dataSourceNameArray[MessageBrokerDBUtil.
                                                CONTEXT_STORE_DATA_SOURCE] +
                                        "' and ../@type='RDBMS']";
                    //read an xml node using xpath
                    Node configurationNode = (Node) xPath.compile(xpathQuery).
                            evaluate(xmlDocument, XPathConstants.NODE);

                    addToConfigurationMap(configurationNode, contextStoreConfiguration);
                }

            } catch (ParserConfigurationException e) {
                log.error("Unexpected error occurred while parsing configuration.", e);
                throw new ConfigurationException("Unexpected error occurred while parsing" +
                                                 " configuration: ", e);
            } catch (XPathException e) {
                log.error("Unexpected error occurred while parsing xml file content.", e);
                throw new ConfigurationException("Unexpected error occurred while parsing xml" +
                                                 " file content: ", e);
            } catch (SAXException e) {
                log.error("Unexpected error occurred in XML parser.", e);
                throw new ConfigurationException("Unexpected error occurred in XML parser.", e);
            } catch (IOException e) {
                log.error("master data source xml file not found.", e);
                throw new ConfigurationException("master data source xml file not found.", e);
            }


    }


    /**
     * This method will return configurationList Array list
     *
     * @return configurationList which contain list of configurations (context store configurations
     * and message store configurations)
     */
    public ArrayList<HashMap<Object, String>> getConfigurationMap() {

        ArrayList<HashMap<Object, String>> configurationList =
                new ArrayList<HashMap<Object, String>>();
        if(!messageStoreConfiguration.isEmpty()) {
            configurationList.add(messageStoreConfiguration);
        }
        if(!contextStoreConfiguration.isEmpty()) {
            configurationList.add(contextStoreConfiguration);
        }
        return configurationList;
    }


    /**
     * This method will populate messageStoreConfiguration hash map as key value pairs
     * from given parent node.
     *
     * @param node contain relevant configuration  parameters.
     */
    private void addToConfigurationMap(Node node,HashMap<Object, String> databaseConfiguration ) {

        NodeList childNodeList;

        if (null != node) {
            childNodeList = node.getChildNodes();
            for (int i = 0; null != childNodeList && i < childNodeList.getLength(); i++) {
                Node nod = childNodeList.item(i);
                if (nod.getNodeType() == Node.ELEMENT_NODE) {
                    databaseConfiguration.put(childNodeList.item(i).getNodeName(), nod.
                            getFirstChild().getNodeValue());
                }
            }
        }
    }


}
