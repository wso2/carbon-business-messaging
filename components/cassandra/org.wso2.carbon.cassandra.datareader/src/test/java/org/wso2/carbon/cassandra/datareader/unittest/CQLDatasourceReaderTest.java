/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.cassandra.datareader.unittest;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.wso2.carbon.cassandra.datareader.cql.CassandraConfiguration;
import org.wso2.carbon.cassandra.datareader.cql.CassandraDataSourceReader;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import junit.framework.TestCase;
import org.testng.annotations.Test;

public class CQLDatasourceReaderTest extends TestCase {

	@Test
	public void testCQLDataSourceReader() throws IOException, DataSourceException {
		String content = "";
		ClassLoader classLoader = getClass().getClassLoader();
		content = IOUtils.toString(classLoader.getResourceAsStream("cql-datasource-config.xml"));
		CassandraConfiguration configuration = CassandraDataSourceReader.loadConfig(content);
		assertNotNull(configuration);
		assertEquals("TestCluster", configuration.getClusterName());
	}
}
