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

package com.liferay.arquillian.portal.remote;

import com.liferay.arquillian.portal.activator.PortalURLBundleActivator;
import com.liferay.arquillian.portal.servlet.PortalURLServlet;

import java.io.InputStream;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Cristina González
 */
public class PortalURLAuxiliaryAppender implements AuxiliaryArchiveAppender {

	@Override
	public Archive<?> createAuxiliaryArchive() {
		JavaArchive archive = ShrinkWrap.create(
			JavaArchive.class, "arquillian-install-portlet-in-liferay.jar");

		archive.addClass(PortalURLBundleActivator.class);
		archive.addClass(PortalURLServlet.class);
		archive.addClass(PortalURLAuxiliaryAppender.class);

		archive.setManifest(
			new Asset() {

				@Override
				public InputStream openStream() {
					OSGiManifestBuilder builder =
						OSGiManifestBuilder.newInstance();

					builder.addBundleManifestVersion(2);
					builder.addBundleSymbolicName(
						"arquillian-install-portlet-in-liferay");
					builder.addImportPackages(
						"com.liferay.portal.kernel.exception",
						"com.liferay.portal.kernel.util",
						"com.liferay.portal.kernel.model",
						"com.liferay.portal.kernel.service",
						"javax.servlet.http", "javax.portlet", "javax.servlet",
						"org.osgi.framework");
					builder.addBundleActivator(PortalURLBundleActivator.class);

					return builder.openStream();
				}

			});

		return archive;
	}

}