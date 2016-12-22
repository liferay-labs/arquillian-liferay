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

package com.liferay.arquillian.containter.remote.enricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.arquillian.test.spi.TestEnricher;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

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
					_injectField(declaredField, null, testCase);
				}
				else {
					_injectField(declaredField, inject.value(), testCase);
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
					parameters[i] = _resolve(
						parameterTypes[i], null, method.getDeclaringClass());
				}
				else {
					parameters[i] = _resolve(
						parameterTypes[i], injectAnnotation.value(),
						method.getDeclaringClass());
				}
			}
		}

		return parameters;
	}

	private Bundle _getBundle(Class<?> testCaseClass) {
		ClassLoader classLoader = testCaseClass.getClassLoader();

		if (classLoader instanceof BundleReference) {
			return ((BundleReference)classLoader).getBundle();
		}

		throw new RuntimeException("Test is not running inside BundleContext");
	}

	private void _injectField(
		Field declaredField, String filterString, Object testCase) {

		Class<?> componentClass = declaredField.getType();

		Object service = _resolve(
			componentClass, filterString, testCase.getClass());

		_setField(declaredField, testCase, service);
	}

	private Object _resolve(
		Class<?> componentClass, String filterString, Class<?> testCaseClass) {

		Bundle bundle = _getBundle(testCaseClass);

		BundleContext bundleContext = bundle.getBundleContext();

		ServiceReference<?>[] serviceReferences;

		try {
			serviceReferences = bundleContext.getServiceReferences(
				componentClass.getName(), filterString);
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(
				"Bad Syntax for the filter: " + filterString, ise);
		}

		return bundleContext.getService(serviceReferences[0]);
	}

	private void _setField(
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