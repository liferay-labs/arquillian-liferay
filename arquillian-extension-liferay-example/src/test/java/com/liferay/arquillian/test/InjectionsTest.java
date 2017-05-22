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

package com.liferay.arquillian.test;

import com.liferay.arquillian.containter.remote.enricher.Inject;
import com.liferay.arquillian.sample.service4injection.Service;
import com.liferay.arquillian.sample.service4injection.ServiceFirstImpl;
import com.liferay.arquillian.sample.service4injection.ServiceSecondImpl;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.service.ReleaseLocalService;
import com.liferay.shrinkwrap.osgi.api.BndProjectBuilder;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author Cristina González
 */
@RunWith(Arquillian.class)
public class InjectionsTest {

	@Deployment
	public static JavaArchive create() {
		BndProjectBuilder bndProjectBuilder = ShrinkWrap.create(
			BndProjectBuilder.class);

		bndProjectBuilder.setBndFile(new File("bnd-basic-portlet-test.bnd"));

		bndProjectBuilder.generateManifest(true);

		return bndProjectBuilder.as(JavaArchive.class);
	}

	@Test
	public void testInjectBundle() {
		Assert.assertNotNull(_bundle);
		Assert.assertEquals(
			"com.liferay.arquillian.sample", _bundle.getSymbolicName());
	}

	@Test
	public void testInjectBundleContext() {
		Assert.assertNotNull(_bundleContext);
		Assert.assertEquals(
			_bundle.getBundleContext().getBundle(0),
			_bundleContext.getBundle());
	}

	@Test
	public void testInjectReleaseLocalService() {
		Assert.assertNotNull(_releaseLocalService);

		Release releasePortal = _releaseLocalService.fetchRelease("portal");

		Assert.assertEquals(7002, releasePortal.getBuildNumber());
	}

	@Test
	public void testInjectServiceInOrden() {
		Assert.assertNotNull(_service);

		Assert.assertTrue(_service instanceof ServiceFirstImpl);
	}

	@Test
	public void testInjectServiceWithFilter() {
		Assert.assertNotNull(_secondService);

		Assert.assertTrue(_secondService instanceof ServiceSecondImpl);
	}

	@ArquillianResource
	private Bundle _bundle;

	@ArquillianResource
	private BundleContext _bundleContext;

	@Inject
	private ReleaseLocalService _releaseLocalService;

	@Inject("(name=ServiceSecond)")
	private Service _secondService;

	@Inject
	private Service _service;

}