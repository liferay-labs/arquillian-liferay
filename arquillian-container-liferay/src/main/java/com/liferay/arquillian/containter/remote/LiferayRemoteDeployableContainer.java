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

package com.liferay.arquillian.containter.remote;

import com.liferay.arquillian.container.osgi.remote.processor.service.BSNContext;
import com.liferay.arquillian.containter.osgi.allin.remote.KarafWithoutBundleRemoteDeployableContainer;

import java.io.IOException;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.osgi.spi.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * @author Carlos Sierra Andrés
 */
public class LiferayRemoteDeployableContainer
	<T extends LiferayRemoteContainerConfiguration>
		extends KarafWithoutBundleRemoteDeployableContainer<T> {

	@Override
	public ProtocolMetaData deploy(Archive<?> archive)
		throws DeploymentException {

		LiferayRemoteContainerConfiguration config =
			_configurationInstance.get();

		ProtocolMetaData protocolMetaData = super.deploy(archive);

		protocolMetaData.addContext(
			new HTTPContext(config.getHttpHost(), config.getHttpPort()));

		try {
			BundleInfo info = BundleInfo.createBundleInfo(
				_toVirtualFile(archive));

			String bsn = info.getSymbolicName();

			protocolMetaData.addContext(new BSNContext(bsn));
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return protocolMetaData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getConfigurationClass() {
		return (Class<T>)LiferayRemoteContainerConfiguration.class;
	}

	@Override
	public void setup(T config) {
		_configurationInstanceProducer.set(config);

		super.setup(config);
	}

	@ApplicationScoped
	@Inject
	protected Instance<LiferayRemoteContainerConfiguration>
		_configurationInstance;

	@ApplicationScoped
	@Inject
	protected InstanceProducer<LiferayRemoteContainerConfiguration>
		_configurationInstanceProducer;

	private VirtualFile _toVirtualFile(Archive<?> archive) throws IOException {
		ZipExporter exporter = archive.as(ZipExporter.class);

		return AbstractVFS.toVirtualFile(
			archive.getName(), exporter.exportAsInputStream());
	}

}