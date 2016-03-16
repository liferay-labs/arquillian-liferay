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

package com.liferay.arquillian.containter.remote.wait;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.core.spi.context.ApplicationContext;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Cristina González
 */
public class LiferayWaitForServiceObserver {

	public void execute(@Observes(precedence = Integer.MAX_VALUE)
		EventContext<BeforeSuite> event) throws InvalidSyntaxException {

		Bundle bundle = FrameworkUtil.getBundle(getClass());

		BundleContext bundleContext = bundle.getBundleContext();

		Filter filter = FrameworkUtil.createFilter(
			"(&(objectClass=org.springframework.context.ApplicationContext)" +
				"(org.springframework.context.service.name=" +
					bundleContext.getBundle().getSymbolicName() + "))");

		ServiceTracker<ApplicationContext, ApplicationContext> serviceTracker =
			new ServiceTracker<>(bundleContext, filter, null);

		serviceTracker.open();

		try {
			serviceTracker.waitForService(30 * 1000L);
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}

		serviceTracker.close();

		event.proceed();
	}

}