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

package com.liferay.arquillian.deploymentscenario.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.liferay.arquillian.deploymentscenario.BndDeploymentScenarioGenerator;
import com.liferay.arquillian.test.ATest;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

import org.junit.Test;

/**
 * @author Carlos Sierra Andrés
 */
public class BndDeploymentScenarioGeneratorTest {

	@Test
	public void testBundleGeneration() throws IOException {
		BndDeploymentScenarioGenerator bndDeploymentScenarioGenerator =
			new BndDeploymentScenarioGenerator() {

				@Override
				protected DeploymentScenarioGenerator
					getDefaultDeploymentScenarioGenerator() {

					return null;
				}

			};

		bndDeploymentScenarioGenerator.setBndFile(
			new File("target/test-classes/test.bnd"));

		List<DeploymentDescription> deploymentDescriptions =
			bndDeploymentScenarioGenerator.generate(new TestClass(ATest.class));

		assertEquals(1, deploymentDescriptions.size());

		DeploymentDescription deploymentDescription =
			deploymentDescriptions.get(0);

		Archive<?> archive = deploymentDescription.getArchive();

		Node object = archive.get("META-INF/MANIFEST.MF");

		assertNotNull("We must have a MANIFEST.MF", object);

		Manifest manifest = new Manifest(object.getAsset().openStream());

		Attributes mainAttributes = manifest.getMainAttributes();

		String importPackageValue = mainAttributes.getValue("Import-Package");

		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.a"));
		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.b"));

		assertTrue(
			"Should contain org.osgi.framework",
			importPackageValue.contains("org.osgi.framework"));

		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/a/A.class"));
		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/b/B.class"));
	}

	@Test
	public void testBundleGenerationWithCommonBnd() throws IOException {
		BndDeploymentScenarioGenerator bndDeploymentScenarioGenerator =
			new BndDeploymentScenarioGenerator() {

				@Override
				protected DeploymentScenarioGenerator
					getDefaultDeploymentScenarioGenerator() {

					return null;
				}

			};

		bndDeploymentScenarioGenerator.setBndFile(
			new File("target/test-classes/test.bnd"));

		File commonBndFile = new File("target/test-classes/common.bnd");

		bndDeploymentScenarioGenerator.setCommonBndFile(commonBndFile);

		List<DeploymentDescription> deploymentDescriptions =
			bndDeploymentScenarioGenerator.generate(new TestClass(ATest.class));

		assertEquals(1, deploymentDescriptions.size());

		DeploymentDescription deploymentDescription =
			deploymentDescriptions.get(0);

		Archive<?> archive = deploymentDescription.getArchive();

		Node object = archive.get("META-INF/MANIFEST.MF");

		assertNotNull("We must have a MANIFEST.MF", object);

		Manifest manifest = new Manifest(object.getAsset().openStream());

		Attributes mainAttributes = manifest.getMainAttributes();

		String importPackageValue = mainAttributes.getValue("Import-Package");

		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.a"));
		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.b"));

		String fooProperty = mainAttributes.getValue("Foo-Property");

		assertEquals("test", fooProperty);

		String fooBadProperty = mainAttributes.getValue("Foo-Bad-Property");

		assertEquals("${a.non.existant.property}", fooBadProperty);

		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/a/A.class"));
		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/b/B.class"));
	}

	@Test
	public void testBundleGenerationWithCommonBndFromSystemProperty()
		throws IOException {

		System.setProperty("sdk.dir", "target/test-classes");

		BndDeploymentScenarioGenerator bndDeploymentScenarioGenerator =
			new BndDeploymentScenarioGenerator() {

				@Override
				protected DeploymentScenarioGenerator
					getDefaultDeploymentScenarioGenerator() {

					return null;
				}

			};

		bndDeploymentScenarioGenerator.setBndFile(
			new File("target/test-classes/test.bnd"));

		List<DeploymentDescription> deploymentDescriptions =
			bndDeploymentScenarioGenerator.generate(new TestClass(ATest.class));

		assertEquals(1, deploymentDescriptions.size());

		DeploymentDescription deploymentDescription =
			deploymentDescriptions.get(0);

		Archive<?> archive = deploymentDescription.getArchive();

		Node object = archive.get("META-INF/MANIFEST.MF");

		assertNotNull("We must have a MANIFEST.MF", object);

		Manifest manifest = new Manifest(object.getAsset().openStream());

		Attributes mainAttributes = manifest.getMainAttributes();

		String importPackageValue = mainAttributes.getValue("Import-Package");

		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.a"));
		assertFalse(
			"Package from the classes must not be imported",
			importPackageValue.contains(
				"com.liferay.arquillian.test.extras.b"));

		String fooProperty = mainAttributes.getValue("Foo-Property");

		assertEquals("test", fooProperty);

		String fooBadProperty = mainAttributes.getValue("Foo-Bad-Property");

		assertEquals("${a.non.existant.property}", fooBadProperty);

		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/a/A.class"));
		assertNotNull(
			"Classes must be included",
			archive.get("com/liferay/arquillian/test/extras/b/B.class"));
	}

}