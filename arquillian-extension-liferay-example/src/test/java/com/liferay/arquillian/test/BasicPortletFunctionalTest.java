/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.arquillian.test;

import com.liferay.arquillian.containter.remote.enricher.Inject;
import com.liferay.arquillian.portal.bundle.annotation.PortalURL;
import com.liferay.arquillian.sample.service.SampleService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.shrinkwrap.osgi.api.BndProjectBuilder;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Cristina González
 */
@RunAsClient
@RunWith(Arquillian.class)
public class BasicPortletFunctionalTest {

	@Deployment
	public static JavaArchive create() {
		BndProjectBuilder bndProjectBuilder = ShrinkWrap.create(
			BndProjectBuilder.class);

		bndProjectBuilder.setBndFile(new File("bnd-basic-portlet-test.bnd"));

		bndProjectBuilder.generateManifest(true);

		return bndProjectBuilder.as(JavaArchive.class);
	}

	@Test
	public void testAdd() throws IOException, PortalException {
		browser.get(_portlerURL.toExternalForm());

		firstParamter.clear();

		firstParamter.sendKeys("2");

		secondParameter.clear();

		secondParameter.sendKeys("3");

		add.click();

		Assert.assertEquals("5", result.getText());
	}

	@Test
	public void testInstallPortlet() throws IOException, PortalException {
		browser.get(_portlerURL.toExternalForm());

		String bodyText = browser.findElement(By.tagName("body")).getText();

		Assert.assertTrue(
			"The portlet is not well deployed",
			bodyText.contains("Sample Portlet is working!"));
	}

	@PortalURL("arquillian_sample_portlet")
	private URL _portlerURL;

	@Inject
	private SampleService _sampleService;

	@FindBy(css = "button[type=submit]")
	private WebElement add;

	@Drone
	private WebDriver browser;

	@FindBy(css = "input[id$='firstParameter']")
	private WebElement firstParamter;

	@FindBy(css = "span[class='result']")
	private WebElement result;

	@FindBy(css = "input[id$='secondParameter']")
	private WebElement secondParameter;

}