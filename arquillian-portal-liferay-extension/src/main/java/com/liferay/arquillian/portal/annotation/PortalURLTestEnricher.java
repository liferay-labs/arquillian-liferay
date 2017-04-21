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

package com.liferay.arquillian.portal.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.net.URI;
import java.net.URL;

import java.util.Collection;
import java.util.Iterator;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * @author Cristina González
 */
public class PortalURLTestEnricher implements TestEnricher {

	public boolean contains(
		Annotation[] annotations, Class<?> annotationClass) {

		for (Annotation current : annotations) {
			if (annotationClass.isAssignableFrom(current.annotationType())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void enrich(Object testCase) {
		Class<?> testClass = testCase.getClass();

		Field[] declaredFields = testClass.getDeclaredFields();

		for (Field declaredField : declaredFields) {
			if (declaredField.isAnnotationPresent(PortalURL.class)) {
				PortalURL annotation = declaredField.getAnnotation(
					PortalURL.class);

				try {
					_injectField(declaredField, testCase, annotation.value());
				}
				catch (IllegalAccessException iae) {
					throw new RuntimeException(
						"Exception when injecting the field", iae);
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

			if (contains(parameterAnnotations, PortalURL.class)) {
				PortalURL annotation = (PortalURL)getAnnotation(
					parameterAnnotations, PortalURL.class);

				parameters[i] = _resolve(annotation.value());
			}
		}

		return parameters;
	}

	private void _injectField(
			Field declaredField, Object testCase, String portletId)
		throws IllegalAccessException {

		_setField(declaredField, testCase, _resolve(portletId));
	}

	private URL _resolve(String portletId) {
		ProtocolMetaData metaData = _protocolMetadata.get();

		if (metaData == null) {
			return null;
		}

		if (metaData.hasContext(HTTPContext.class)) {
			Collection<HTTPContext> contexts = metaData.getContexts(
				HTTPContext.class);

			Iterator<HTTPContext> iterator = contexts.iterator();

			HTTPContext context = iterator.next();

			try {
				URL url = new URI(
					"http", null, context.getHost(), context.getPort(), null,
					null, null).toURL();

				return new URL(
					url, "/o/install-portlet-servlet?portlet-id=" + portletId);
			}
			catch (Exception e) {
				throw new RuntimeException("Can't obtain URL, " + context, e);
			}
		}

		return null;
	}

	private void _setField(Field declaredField, Object testCase, URL service)
		throws IllegalAccessException {

		boolean accessible = declaredField.isAccessible();

		declaredField.setAccessible(true);

		declaredField.set(testCase, service);

		declaredField.setAccessible(accessible);
	}

	@org.jboss.arquillian.core.api.annotation.Inject
	private Instance<ProtocolMetaData> _protocolMetadata;

}