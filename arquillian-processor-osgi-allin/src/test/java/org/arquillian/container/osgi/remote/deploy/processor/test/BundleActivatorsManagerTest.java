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

import com.liferay.arquillian.container.osgi.remote.processor.service.BundleActivatorsManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.BundleActivatorsManagerImpl;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.arquillian.container.osgi.remote.deploy.processor.test.util.ManifestUtil;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;

import org.osgi.framework.BundleActivator;

/**
 * @author Cristina González
 */
public class BundleActivatorsManagerTest {

	@Test
	public void testGetBundleActivators() throws IOException {
		//given:
		JavaArchive javaArchive = _createJavaArchive();

		List<String> initialBundleActivators = new ArrayList<>();

		initialBundleActivators.add("Activator1");
		initialBundleActivators.add("Activator2");
		initialBundleActivators.add("Activator3");

		StringBuilder sb = new StringBuilder();

		for (String bundleActivator : initialBundleActivators) {
			sb.append(bundleActivator);
			sb.append("\n");
		}

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(
			sb.toString().getBytes());

		javaArchive.add(byteArrayAsset, _ACTIVATORS_FILE);

		//when:
		List<String> actualBundleActivators =
			_bundleActivatorsManager.getBundleActivators(
				javaArchive, _ACTIVATORS_FILE);

		//then:
		Assert.assertEquals(
			actualBundleActivators.size(), initialBundleActivators.size());

		for (String bundleActivator : initialBundleActivators) {
			Assert.assertTrue(actualBundleActivators.contains(bundleActivator));
		}
	}

	@Test
	public void testReplaceBundleActivatorsFile() throws IOException {
		//given:
		JavaArchive javaArchive = _createJavaArchive();

		List<String> initialBundleActivators = new ArrayList<>();

		initialBundleActivators.add("Activator1");
		initialBundleActivators.add("Activator2");
		initialBundleActivators.add("Activator3");

		StringBuilder sb = new StringBuilder();

		for (String bundleActivator : initialBundleActivators) {
			sb.append(bundleActivator);
			sb.append("\n");
		}

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(
			sb.toString().getBytes());

		javaArchive.add(byteArrayAsset, _ACTIVATORS_FILE);

		//when:
		List<String> updateBundleActivators = new ArrayList<>();

		updateBundleActivators.add("Activator4");
		updateBundleActivators.add("Activator5");

		_bundleActivatorsManager.replaceBundleActivatorsFile(
			javaArchive, _ACTIVATORS_FILE, updateBundleActivators);

		//then:
		List<String> actualBundleActivators =
			_bundleActivatorsManager.getBundleActivators(
				javaArchive, _ACTIVATORS_FILE);

		Assert.assertEquals(
			updateBundleActivators.size(), updateBundleActivators.size());

		for (String bundleActivator : updateBundleActivators) {
			Assert.assertTrue(actualBundleActivators.contains(bundleActivator));
		}
	}

	private JavaArchive _createJavaArchive() {
		JavaArchive javaArchive = ShrinkWrap.create(
			JavaArchive.class, "dummy-jar.jar");

		javaArchive.addPackage(BundleActivatorsManagerTest.class.getPackage());

		try {
			ManifestUtil.createManifest(javaArchive);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		return javaArchive;
	}

	private static final String _ACTIVATORS_FILE =
		"/META-INF/services/" + BundleActivator.class.getCanonicalName();

	private static final BundleActivatorsManager _bundleActivatorsManager =
		new BundleActivatorsManagerImpl();

}