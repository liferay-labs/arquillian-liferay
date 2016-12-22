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

package org.arquillian.container.osgi.remote.deploy.processor.test.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Cristina González
 */
public class ManifestUtil {

	public static void createManifest(JavaArchive javaArchive)
		throws IOException {

		createManifest(javaArchive, new ArrayList<String>());
	}

	public static void createManifest(
			JavaArchive javaArchive, List<String> imports)
		throws IOException {

		createManifest(javaArchive, imports, null);
	}

	public static void createManifest(
			JavaArchive javaArchive, List<String> imports, String activator)
		throws IOException {

		Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(
			new Attributes.Name("Manifest-Version"), "1.0");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-Name"), "Test");
		manifest.getMainAttributes().put(
			new Attributes.Name("Bundle-ManifestVersion"), "1");

		if ((activator != null) && !activator.isEmpty()) {
			manifest.getMainAttributes().put(
				new Attributes.Name("Bundle-Activator"), activator);
		}

		if ((imports != null) && !imports.isEmpty()) {
			StringBuilder sb = new StringBuilder();

			for (String importValue : imports) {
				sb.append(importValue);
				sb.append(",");
			}

			int length = sb.length();

			if (length > 0) {
				sb.setLength(length - 1);
			}

			manifest.getMainAttributes().put(
				new Attributes.Name("Import-Package"), sb.toString());
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		manifest.write(baos);

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(baos.toByteArray());

		javaArchive.delete(JarFile.MANIFEST_NAME);

		javaArchive.add(byteArrayAsset, JarFile.MANIFEST_NAME);
	}

}