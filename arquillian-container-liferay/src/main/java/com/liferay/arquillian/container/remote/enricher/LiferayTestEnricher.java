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

package com.liferay.arquillian.container.remote.enricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.arquillian.test.spi.TestEnricher;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Carlos Sierra Andrés
 */
public class LiferayTestEnricher implements TestEnricher {

	@Override
	public void enrich(Object testCase) {
		Class<?> testClass = testCase.getClass();

		Field[] declaredFields = testClass.getDeclaredFields();

		for (Field declaredField : declaredFields) {
			if (declaredField.isAnnotationPresent(Inject.class)) {
				Inject inject = declaredField.getAnnotation(Inject.class);

				if (inject.value().equals("")) {
					injectField(declaredField, null, testCase);
				}
				else {
					injectField(declaredField, inject.value(), testCase);
				}
			}
		}
	}

	public Annotation getAnnotation(
		Annotation[] annotations, Class<?> annotationClass) {

		for (Annotation current : annotations) {
			if (annotationClass.isAssignableFrom(current.annotationType())) {
				return current;
			}
		}

		return null;
	}

	@Override
	public Object[] resolve(Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] parametersAnnotations = method.getParameterAnnotations();

		Object[] parameters = new Object[parameterTypes.length];

		for (int i = 0; i < parameterTypes.length; i++) {
			Annotation[] parameterAnnotations = parametersAnnotations[i];

			Inject injectAnnotation = (Inject)getAnnotation(
				parameterAnnotations, Inject.class);

			if (injectAnnotation != null) {
				if (injectAnnotation.value().equals("")) {
					parameters[i] = resolve(
						parameterTypes[i], null, method.getDeclaringClass());
				}
				else {
					parameters[i] = resolve(
						parameterTypes[i], injectAnnotation.value(),
						method.getDeclaringClass());
				}
			}
		}

		return parameters;
	}

	private Bundle getBundle(Class<?> testCaseClass) {
		ClassLoader classLoader = testCaseClass.getClassLoader();

		if (classLoader instanceof BundleReference) {
			return ((BundleReference)classLoader).getBundle();
		}

		throw new RuntimeException("Test is not running inside BundleContext");
	}

	private void injectField(
		Field declaredField, String filterString, Object testCase) {

		Class<?> componentClass = declaredField.getType();

		Object service = resolve(
			componentClass, filterString, testCase.getClass());

		setField(declaredField, testCase, service);
	}

	private Object resolve(
		Class<?> componentClass, String filterString, Class<?> testCaseClass) {

		Bundle bundle = getBundle(testCaseClass);

		BundleContext bundleContext = bundle.getBundleContext();

		ServiceTracker<?, ?> serviceTracker = null;

		try {
			Filter filter;

			if (filterString != null) {
				filter = bundleContext.createFilter(
					String.format(
						"(&(objectClass=%s)%s)", componentClass.getName(),
						filterString));
			}
			else {
				filter = bundleContext.createFilter(
					String.format(
						"(objectClass=%s)", componentClass.getName()));
			}

			serviceTracker = new ServiceTracker<>(bundleContext, filter, null);

			serviceTracker.open();

			Object service = serviceTracker.waitForService(5 * 1000L);

			if (service == null) {
				throw new RuntimeException(
					"Timeout waiting for : " + filterString);
			}

			return service;
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(
				"Bad Syntax for the filter: " + filterString, ise);
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
		finally {
			if (serviceTracker != null) {
				serviceTracker.close();
			}
		}
	}

	private void setField(
		Field declaredField, Object testCase, Object service) {

		boolean accessible = declaredField.isAccessible();

		declaredField.setAccessible(true);

		try {
			declaredField.set(testCase, service);
		}
		catch (IllegalAccessException iae) {
			iae.printStackTrace();
		}

		declaredField.setAccessible(accessible);
	}

}