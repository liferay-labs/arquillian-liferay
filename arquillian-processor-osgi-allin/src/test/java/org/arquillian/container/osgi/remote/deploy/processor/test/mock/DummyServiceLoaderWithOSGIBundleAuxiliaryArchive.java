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

package org.arquillian.container.osgi.remote.deploy.processor.test.mock;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.arquillian.container.osgi.remote.deploy.processor.test.util.ManifestUtil;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Cristina González
 */
public class DummyServiceLoaderWithOSGIBundleAuxiliaryArchive
	extends DummyServiceLoaderWithoutAuxiliaryArchive {

	public DummyServiceLoaderWithOSGIBundleAuxiliaryArchive(
		List<String> imports) {

		_imports = imports;
	}

	@Override
	public <T> Collection<T> all(Class<T> aClass) {
		Collection<T> all = super.all(aClass);

		if ((all != null) && !all.isEmpty()) {
			return all;
		}

		if (aClass.isAssignableFrom(DummyAuxiliaryArchiveAppender.class)) {
			ArrayList<DummyAuxiliaryArchiveAppender> dummyAuxiliaryArchives =
				new ArrayList<>();

			dummyAuxiliaryArchives.add(new DummyAuxiliaryArchiveAppender());

			return (Collection<T>)dummyAuxiliaryArchives;
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T onlyOne(Class<T> aClass) {
		T onlyOne = super.onlyOne(aClass);

		if (onlyOne != null) {
			return onlyOne;
		}

		if (aClass.isAssignableFrom(DummyAuxiliaryArchiveAppender.class)) {
			return (T)new DummyAuxiliaryArchiveAppender();
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T onlyOne(Class<T> aClass, Class<? extends T> aClass1) {
		T onlyOne = super.onlyOne(aClass, aClass1);

		if (onlyOne != null) {
			return onlyOne;
		}

		if (aClass.isAssignableFrom(DummyAuxiliaryArchiveAppender.class)) {
			return (T)new DummyAuxiliaryArchiveAppender();
		}

		throw new UnsupportedOperationException();
	}

	private final List<String> _imports;

	private class DummyAuxiliaryArchiveAppender
		implements AuxiliaryArchiveAppender {

		@Override
		public Archive<?> createAuxiliaryArchive() {
			JavaArchive javaArchive = ShrinkWrap.create(
				JavaArchive.class, "dummy-jar.jar");

			javaArchive.addPackage(
				DummyServiceLoaderWithJarAuxiliaryArchive.class.getPackage());

			try {
				ManifestUtil.createManifest(javaArchive, _imports);
			}
			catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}

			return javaArchive;
		}

	}

}