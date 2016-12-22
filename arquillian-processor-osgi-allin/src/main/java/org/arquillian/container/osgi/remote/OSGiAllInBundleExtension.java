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

package org.arquillian.container.osgi.remote;

import com.liferay.arquillian.container.osgi.remote.bundleclasspath.BundleClassPathAuxiliaryAppender;
import com.liferay.arquillian.container.osgi.remote.commandservice.CommandServiceAuxiliaryAppender;
import com.liferay.arquillian.container.osgi.remote.instanceproducer.OSGiAllInBundleInstanceProducer;
import com.liferay.arquillian.container.osgi.remote.processor.OSGiAllInProcessor;
import com.liferay.arquillian.container.osgi.remote.processor.service.BundleActivatorsManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.BundleActivatorsManagerImpl;
import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManagerImpl;
import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManagerImpl;
import com.liferay.arquillian.container.osgi.remote.processor.service.MultiDeploymentJMXOSGiProtocol;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandService;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.protocol.jmx.JMXCommandService;
import org.jboss.arquillian.protocol.osgi.JMXOSGiProtocol;

/**
 * @author Cristina González
 */
public class OSGiAllInBundleExtension implements LoadableExtension {

	@Override
	public void register(ExtensionBuilder builder) {
		builder.service(
			ApplicationArchiveProcessor.class, OSGiAllInProcessor.class);

		builder.service(
			AuxiliaryArchiveAppender.class,
			BundleClassPathAuxiliaryAppender.class);

		builder.service(
			AuxiliaryArchiveAppender.class,
			CommandServiceAuxiliaryAppender.class);

		builder.service(
			ImportPackageManager.class, ImportPackageManagerImpl.class);

		builder.service(ManifestManager.class, ManifestManagerImpl.class);

		builder.service(
			BundleActivatorsManager.class, BundleActivatorsManagerImpl.class);

		builder.observer(OSGiAllInBundleInstanceProducer.class);

		builder.service(CommandService.class, JMXCommandService.class);

		builder.override(
			Protocol.class, JMXOSGiProtocol.class,
			MultiDeploymentJMXOSGiProtocol.class);
	}

}