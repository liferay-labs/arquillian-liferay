/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.arquillian.container.osgi.remote.processor.service;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.container.spi.client.protocol.metadata.JMXContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor;
import org.jboss.arquillian.protocol.jmx.JMXProtocolConfiguration;
import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.arquillian.protocol.osgi.JMXOSGiProtocol;

/**
 * @author Gregory Amerson
 */
public class MultiDeploymentJMXOSGiProtocol extends JMXOSGiProtocol {

	@Override
	public ContainerMethodExecutor getExecutor(
		JMXProtocolConfiguration config, ProtocolMetaData metaData,
		CommandCallback callback) {

		if (metaData.hasContext(JMXContext.class)) {
			MBeanServerConnection mbeanServer = metaData.getContext(
				JMXContext.class).getConnection();

			Map<String, String> protocolProps = new HashMap<>();

			try {
				BeanInfo beanInfo = Introspector.getBeanInfo(config.getClass());

				for (PropertyDescriptor propertyDescriptor :
						beanInfo.getPropertyDescriptors()) {

					String key = propertyDescriptor.getName();
					Object value =
						propertyDescriptor.getReadMethod().invoke(config);

					if (value != null) {
						protocolProps.put(key, "" + value);
					}
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(
					"Cannot obtain protocol config");
			}

			BSNContext bsnContext = _getBsnContext(metaData);

			String objectName = JMXTestRunnerMBean.OBJECT_NAME;

			if (bsnContext != null) {
				objectName = objectName + "-" + bsnContext.getName();
			}

			return new JMXMethodExecutor(
				mbeanServer, callback, objectName, protocolProps);
		}
		else {
			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder.append("No " + JMXContext.class.getName());
			stringBuilder.append(" was found in ");
			stringBuilder.append(ProtocolMetaData.class.getName());
			stringBuilder.append(". The JMX Protocol can not be ");
			stringBuilder.append("used without a connection, please ");
			stringBuilder.append("verify your protocol configuration ");
			stringBuilder.append("or contact the DeployableContainer ");
			stringBuilder.append("developer");

			throw new IllegalStateException(stringBuilder.toString());
		}
	}

	private BSNContext _getBsnContext(ProtocolMetaData metaData) {
		Collection<BSNContext> contexts = metaData.getContexts(
			BSNContext.class);

		if (contexts != null) {
			Iterator<BSNContext> iterator = contexts.iterator();

			return iterator.next();
		}

		return null;
	}

}