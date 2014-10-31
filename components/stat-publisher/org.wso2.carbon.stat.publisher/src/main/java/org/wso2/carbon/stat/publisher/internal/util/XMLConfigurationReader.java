/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.stat.publisher.internal.util;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.stat.publisher.conf.GeneralConfiguration;
import org.wso2.carbon.stat.publisher.conf.JMXConfiguration;
import org.wso2.carbon.stat.publisher.conf.StreamConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XMLConfigurationReader {

    private static final Logger logger = Logger.getLogger(XMLConfigurationReader.class);

    /**
     *Load xml files and read JMX configurations
     */
    public static JMXConfiguration readJMXConfiguration() throws StatPublisherConfigurationException {

        JMXConfiguration jmxConfiguration=new JMXConfiguration();
        try {
            //Load jmx.xml file
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document jmxDocument;
            String jmxFilePath = StatPublisherConstants.JMX_DIRECTORY_PATH+StatPublisherConstants.JMX_XML;

            File jmxFile= new File(jmxFilePath);
            if (!jmxFile.exists()) {
                logger.error("jmx.xml does not exists in "+jmxFile.getPath());
                throw new StatPublisherConfigurationException("jmx.xml does not exists in "+
                                                              jmxFile.getPath());
            } else {
                jmxDocument = docBuilder.parse(jmxFilePath);
                jmxDocument.getDocumentElement().normalize();
                String jmxRootNode = jmxDocument.getDocumentElement().getNodeName();
                NodeList jmxDataList = jmxDocument.getElementsByTagName(jmxRootNode);

                String hostNameValue =
                        ((Element) jmxDataList.item(0)).getElementsByTagName("HostName").
                                item(0).getChildNodes().item(0).getTextContent();
                jmxConfiguration.setHostName(hostNameValue.trim());

                //Load carbon.xml file
                Document carbonDocument;
                String carbonFilePath = StatPublisherConstants.CONF_DIRECTORY_PATH+StatPublisherConstants.CARBON_XML;

                File carbonFile = new File(carbonFilePath);
                if (!carbonFile.exists()) {
                    logger.error("carbon.xml does not exists in "+carbonFile.getPath());
                    throw new StatPublisherConfigurationException("carbon.xml does not exists in "+
                                                                  carbonFile.getPath());
                } else {
                    carbonDocument = docBuilder.parse(carbonFilePath);
                    carbonDocument.getDocumentElement().normalize();
                    String carbonRootNode = carbonDocument.getDocumentElement().getNodeName();
                    NodeList carbonDataList = carbonDocument.getElementsByTagName(carbonRootNode);

                    String rmiRegistryPortValue =
                            ((Element) carbonDataList.item(0)).getElementsByTagName("RMIRegistryPort").
                                    item(0).getChildNodes().item(0).getTextContent();
                    jmxConfiguration.setRmiRegistryPort(rmiRegistryPortValue.trim());

                    String offSetValue =
                            ((Element) carbonDataList.item(0)).getElementsByTagName("Offset").
                                    item(0).getChildNodes().item(0).getTextContent();
                    jmxConfiguration.setOffSet(offSetValue.trim());
                }
            }
        } catch (ParserConfigurationException e) {
            throw new StatPublisherConfigurationException("Indicate configuration error!", e);
        } catch (SAXException e) {
            throw new StatPublisherConfigurationException("Indicate a general SAX error or warning!", e);
        } catch (IOException e) {
            throw new StatPublisherConfigurationException("Indicate file loading error!", e);
        }
        return jmxConfiguration;
    }

    /**
     * Load mbStatConfiguration.xml and read stream configuration values
     */
    public static StreamConfiguration readStreamConfiguration() throws StatPublisherConfigurationException {

        StreamConfiguration streamConfiguration = new StreamConfiguration();
        try {
            //Load mbStatConfiguration.xml file
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document;
            String filePath = StatPublisherConstants.CONF_DIRECTORY_PATH+StatPublisherConstants.STAT_CONF_XML;
            File file = new File(filePath);

            if (!file.exists()) {
                logger.error("mbStatConfiguration.xml does not exists in "+file.getPath());
                throw new StatPublisherConfigurationException("mbStatConfiguration.xml does not exists in "+
                                                              file.getPath());
            } else {
                document = docBuilder.parse(filePath);
                document.getDocumentElement().normalize();
                String rootNode = document.getDocumentElement().getNodeName();
                NodeList dataList = document.getElementsByTagName(rootNode);

                String messageStreamVersionValue =
                        ((Element) dataList.item(0)).getElementsByTagName("messageStreamVersion").
                                item(0).getChildNodes().item(0).getTextContent();
                streamConfiguration.setMessageStreamVersion(messageStreamVersionValue.trim());

                String acknowledgeStreamVersionValue =
                        ((Element) dataList.item(0)).getElementsByTagName("acknowledgeStreamVersion").
                                item(0).getChildNodes().item(0).getTextContent();
                streamConfiguration.setAcknowledgeStreamVersion(acknowledgeStreamVersionValue.trim());

                String systemStatisticStreamVersionValue =
                        ((Element) dataList.item(0)).getElementsByTagName("systemStatisticStreamVersion").
                                item(0).getChildNodes().item(0).getTextContent();
                streamConfiguration.setSystemStatisticStreamVersion(systemStatisticStreamVersionValue.trim());

                String mbStatisticStreamVersionValue =
                        ((Element) dataList.item(0)).getElementsByTagName("mbStatisticStreamVersion").
                                item(0).getChildNodes().item(0).getTextContent();
                streamConfiguration.setMbStatisticStreamVersion(mbStatisticStreamVersionValue.trim());

            }
        } catch (ParserConfigurationException e) {
            throw new StatPublisherConfigurationException("Indicate configuration error!", e);
        } catch (SAXException e) {
            throw new StatPublisherConfigurationException("Indicate a general SAX error or warning!", e);
        } catch (IOException e) {
            throw new StatPublisherConfigurationException("Indicate file loading error!", e);
        }
        return  streamConfiguration;
    }

    /**
     * Load mbStatConfiguration.xml and read general configuration values
     */
    public static GeneralConfiguration readGeneralConfiguration() throws StatPublisherConfigurationException {

        GeneralConfiguration generalConfiguration = new GeneralConfiguration();
        try {
            //Load mbStatConfiguration.xml file
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document;
            String filePath = StatPublisherConstants.CONF_DIRECTORY_PATH+StatPublisherConstants.STAT_CONF_XML;
            File file = new File(filePath);

            if (!file.exists()) {
                logger.error("mbStatConfiguration.xml does not exists in "+file.getPath());
                throw new StatPublisherConfigurationException("mbStatConfiguration.xml does not exists in "+
                                                              file.getPath());
            } else {
                document = docBuilder.parse(filePath);
                document.getDocumentElement().normalize();
                String rootNode = document.getDocumentElement().getNodeName();
                NodeList dataList = document.getElementsByTagName(rootNode);

                String asyncMessagePublisherBufferTimeValue =
                        ((Element) dataList.item(0)).getElementsByTagName("asyncMessagePublisherBufferTime").
                                item(0).getChildNodes().item(0).getTextContent();
                generalConfiguration.setAsyncMessagePublisherBufferTime(Integer.parseInt(asyncMessagePublisherBufferTimeValue.trim()));

                String timeIntervalValue =
                        ((Element) dataList.item(0)).getElementsByTagName("timeInterval").
                                item(0).getChildNodes().item(0).getTextContent();
                generalConfiguration.setTimeInterval(Integer.parseInt(timeIntervalValue.trim()));
            }
        } catch (ParserConfigurationException e) {
            throw new StatPublisherConfigurationException("Indicate configuration error!", e);
        } catch (SAXException e) {
            throw new StatPublisherConfigurationException("Indicate a general SAX error or warning!", e);
        } catch (IOException e) {
            throw new StatPublisherConfigurationException("Indicate file loading error!", e);
        }
        return  generalConfiguration;
    }
}
