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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;

import java.util.*;





public class DataSourceConfiguration {

    private HashMap dataSourceConfiguration = new HashMap();
    private static final Log log = LogFactory.getLog(DataSourceConfiguration.class);

    XPath xPath =  XPathFactory.newInstance().newXPath();


    /**
     * Populate this configuration by reading an XML file at the given location. This method
     * can be executed only once on a given DataSourceConfiguration instance. Once invoked and
     * successfully populated, it will ignore all subsequent invocations.
     *
     * @param filePath Path of the XML descriptor file
     * @throws ConfigurationException If an error occurs while reading the XML descriptor
     */
    public void loadDbConfiguration(String filePath) throws ConfigurationException {

        String jndiConfigName = AndesContext.getInstance().getStoreConfiguration().getMessageStoreProperties()
                .getProperty(StoreConfiguration.DATA_SOURCE);

        String xpathExpression = "//datasources-configuration/datasources/datasource/definition/" +
                "configuration[ ../../jndiConfig/name/text()='"+jndiConfigName+"']";

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;

        if (log.isDebugEnabled()) {
            log.debug("load database configurations from data source xml file");
            log.debug("file path : " + filePath);
        }

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage());
            throw new ConfigurationException("Unexpected error occurred while parsing configuration: " + filePath, e);
        }

        try{

            Document xmlDocument = builder.parse(new FileInputStream(filePath));

            //read an xml node using xpath
            Node configurationNode = (Node) xPath.compile(xpathExpression).evaluate(xmlDocument, XPathConstants.NODE);

            addToConfigurationMap(configurationNode);

        } catch (Exception e){
            log.error("Unexpected error occurred while parsing configuration: " + e.getMessage());
            throw new ConfigurationException("Unexpected error occurred while parsing configuration: " + filePath, e);
        }

    }


    public HashMap getConfigurationMap() {

        return dataSourceConfiguration;
    }


    private void addToConfigurationMap(Node node) {

        NodeList childNodeList;

        if(null != node) {
            childNodeList = node.getChildNodes();
            for (int i = 0;null!=childNodeList && i < childNodeList.getLength(); i++) {
                Node nod = childNodeList.item(i);
                if(nod.getNodeType() == Node.ELEMENT_NODE) {
                    dataSourceConfiguration.put(childNodeList.item(i).getNodeName(),nod.getFirstChild().getNodeValue());
                }
            }
        }
    }



}
