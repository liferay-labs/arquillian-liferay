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

package org.arquillian.container.osgi.remote.deploy.processor.test;

import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManagerImpl;
import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManagerImpl;

import java.io.IOException;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.arquillian.container.osgi.remote.deploy.processor.test.mock.DummyInstanceProducerImpl;
import org.arquillian.container.osgi.remote.deploy.processor.test.util.ManifestUtil;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Cristina González
 */
public class ImportPackageManagerTest {

	@Before
	public void setUp() throws IllegalAccessException, NoSuchFieldException {
		initImportPackageManager();
	}

	@Test
	public void testCleanRepeatedImportsWithoutRepeatedImports()
		throws IOException {

		//given:

		JavaArchive javaArchive = createJavaArchive();

		List<String> imports = new ArrayList<>();
		imports.add("dummy.package");

		ManifestUtil.createManifest(javaArchive, imports);

		Manifest manifest = _manifestManager.getManifest(javaArchive);

		List<Archive<?>> archives = new ArrayList<>();
		archives.add(javaArchive);

		//when:
		Manifest actualManifest = _importPackageManager.cleanRepeatedImports(
			manifest, archives);

		Attributes mainAttributes = actualManifest.getMainAttributes();

		//then:
		Assert.assertEquals(
			"dummy.package", mainAttributes.get(
				new Attributes.Name("Import-Package")));
	}

	@Test
	public void testCleanRepeatedImportsWithRepeatedImports()
		throws IOException {

		//given:

		JavaArchive javaArchive = createJavaArchive();

		List<String> imports = new ArrayList<>();
		imports.add(ImportPackageManagerTest.class.getPackage().getName());

		ManifestUtil.createManifest(javaArchive, imports);

		Manifest manifest = _manifestManager.getManifest(javaArchive);

		List<Archive<?>> archives = new ArrayList<>();
		archives.add(javaArchive);

		//when:
		Manifest actualManifest = _importPackageManager.cleanRepeatedImports(
			manifest, archives);

		Attributes mainAttributes = actualManifest.getMainAttributes();

		//then:
		Assert.assertEquals(
			"", mainAttributes.get(new Attributes.Name("Import-Package")));
	}

	private JavaArchive createJavaArchive() {
		JavaArchive javaArchive = ShrinkWrap.create(
			JavaArchive.class, "dummy-jar.jar");

		javaArchive.addPackage(ImportPackageManagerTest.class.getPackage());

		try {
			ManifestUtil.createManifest(javaArchive);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		return javaArchive;
	}

	private void initImportPackageManager()
		throws IllegalAccessException, NoSuchFieldException {

		Field manifestManagerInstance =
			ImportPackageManagerImpl.class.getDeclaredField(
				"_manifestManagerInstance");
		manifestManagerInstance.setAccessible(true);

		DummyInstanceProducerImpl manifestManagerDummyInstance =
			new DummyInstanceProducerImpl();

		manifestManagerDummyInstance.set(new ManifestManagerImpl());

		manifestManagerInstance.set(
			_importPackageManager, manifestManagerDummyInstance);
	}

	private static final ImportPackageManager _importPackageManager =
		new ImportPackageManagerImpl();
	private static final ManifestManager _manifestManager =
		new ManifestManagerImpl();

}