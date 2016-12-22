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

import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ManifestManagerImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Cristina González
 */
public class ManifestManagerTest {

	@Test
	public void testGetManifest() throws IOException {
		//given:

		JavaArchive javaArchive = ShrinkWrap.create(
			JavaArchive.class, "dummy-jar.jar");

		javaArchive.addPackage(ImportPackageManagerTest.class.getPackage());

		Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "1.0");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "1");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		manifest.write(baos);

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(baos.toByteArray());

		javaArchive.add(byteArrayAsset, JarFile.MANIFEST_NAME);

		//when:
		Manifest actualManifest = _manifestManager.getManifest(javaArchive);

		//then:
		Attributes mainAttributes = actualManifest.getMainAttributes();

		Assert.assertEquals(
			"1.0", mainAttributes.get(new Attributes.Name("Manifest-Version")));

		Assert.assertEquals(
			"Test", mainAttributes.get(new Attributes.Name("Bundle-Name")));

		Assert.assertEquals(
			"1",
			mainAttributes.get(new Attributes.Name("Bundle-ManifestVersion")));
	}

	@Test
	public void testPutAttribute() throws IOException {
		//given:
		Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "1.0");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "1");

		//when:
		Manifest actualManifest = _manifestManager.putAttributeValue(
			manifest, "Import-Package", "com.import.example");

		//then:
		Attributes mainAttributes = actualManifest.getMainAttributes();

		Assert.assertEquals(
			"com.import.example",
			mainAttributes.get(new Attributes.Name("Import-Package")));
	}

	@Test
	public void testPutAttributeList() throws IOException {
		//given:
		Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "1.0");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "1");

		String importPackage1 = "com.import.example1";
		String importPackage2 = "com.import.example2";

		//when:
		Manifest actualManifest = _manifestManager.putAttributeValue(
			manifest, "Import-Package", importPackage1, importPackage2);

		//then:
		Attributes mainAttributes = actualManifest.getMainAttributes();

		String importPackageHeader = (String)mainAttributes.get(
			new Attributes.Name("Import-Package"));

		Assert.assertNotNull(
			"The header Import-Package is empty", importPackageHeader);

		String[] importPackages = importPackageHeader.split(",");

		Assert.assertEquals(2, importPackages.length);

		List<String> importPackagesList = Arrays.asList(importPackages);

		Assert.assertTrue(
			"The import package " + importPackage1 +
				" is not present in the header Import-Package " +
					importPackagesList,
			importPackagesList.contains("com.import.example1"));

		Assert.assertTrue(
			"The import package " + importPackage2 +
				" is not present in the header Import-Package " +
					importPackagesList,
			importPackagesList.contains(importPackage2));
	}

	@Test
	public void testReplaceManifest() throws IOException {
		//given:

		JavaArchive javaArchive = ShrinkWrap.create(
			JavaArchive.class, "dummy-jar.jar");

		javaArchive.addPackage(ImportPackageManagerTest.class.getPackage());

		Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "1.0");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "1");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		manifest.write(baos);

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(baos.toByteArray());

		javaArchive.add(byteArrayAsset, JarFile.MANIFEST_NAME);

		//when:
		Manifest updateManifest = new Manifest();

		updateManifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "2.0");
		updateManifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test2");
		updateManifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "2");

		_manifestManager.replaceManifest(javaArchive, updateManifest);

		//then:
		Manifest actualManifest = _manifestManager.getManifest(javaArchive);

		Attributes mainAttributes = actualManifest.getMainAttributes();

		Assert.assertEquals(
			"2.0", mainAttributes.get(new Attributes.Name("Manifest-Version")));

		Assert.assertEquals(
			"Test2", mainAttributes.get(new Attributes.Name("Bundle-Name")));

		Assert.assertEquals(
			"2",
			mainAttributes.get(new Attributes.Name("Bundle-ManifestVersion")));
	}

	private static final ManifestManager _manifestManager =
		new ManifestManagerImpl();

}