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

package com.liferay.arquillian.container.osgi.remote.processor.service;

import aQute.bnd.osgi.Analyzer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Cristina González
 */
public class ManifestManagerImpl implements ManifestManager {

	public void generateManifest(
			JavaArchive archive, List<Archive<?>> classPath,
			Properties properties)
		throws Exception {

		Analyzer analyzer = new Analyzer();

		Manifest manifest = getManifest(archive);

		Attributes mainAttributes = manifest.getMainAttributes();

		for (Map.Entry<Object, Object> attribute : mainAttributes.entrySet()) {
			Attributes.Name attributeName = (Attributes.Name)attribute.getKey();

			if (properties.get(attributeName.toString()) == null) {
				properties.put(attributeName.toString(), attribute.getValue());
			}
		}

		try {
			List<File> files = new ArrayList<>();

			File archiveFile = getFileFromArchive(archive);

			files.add(archiveFile);

			analyzer.setJar(archiveFile);

			analyzer.setProperties(properties);

			for (Archive<?> classPathArchive : classPath) {
				File classPathFile = getFileFromArchive(classPathArchive);

				analyzer.addClasspath(classPathFile);

				files.add(archiveFile);
			}

			analyzer.analyze();

			ImportPackageManager importPackageManager =
				_importPackageManagerInstance.get();

			importPackageManager.cleanImports(
				analyzer.getImports(), analyzer.getClasspathExports());

			replaceManifest(archive, analyzer.calcManifest());

			for (File file : files) {
				file.deleteOnExit();
			}
		}
		finally {
			analyzer.close();
		}
	}

	@Override
	public Manifest getManifest(JavaArchive javaArchive) throws IOException {
		Node manifestNode = javaArchive.get(JarFile.MANIFEST_NAME);

		Asset manifestAsset = manifestNode.getAsset();

		return new Manifest(manifestAsset.openStream());
	}

	@Override
	public Manifest putAttributeValue(
			Manifest manifest, String attributeName, String... attributeValue)
		throws IOException {

		Attributes mainAttributes = manifest.getMainAttributes();

		String attributeValues = mainAttributes.getValue(attributeName);

		Set<String> attributeValueSet = new HashSet<>();

		if (attributeValues != null) {
			attributeValueSet.addAll(Arrays.asList(attributeValues.split(",")));
		}

		attributeValueSet.addAll(Arrays.asList(attributeValue));

		StringBuilder sb = new StringBuilder();

		for (String value : attributeValueSet) {
			sb.append(value);
			sb.append(",");
		}

		if (!attributeValueSet.isEmpty()) {
			sb.setLength(sb.length() - 1);
		}

		attributeValues = sb.toString();

		mainAttributes.putValue(attributeName, attributeValues);

		return manifest;
	}

	@Override
	public void replaceManifest(Archive archive, Manifest manifest)
		throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		manifest.write(baos);

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(baos.toByteArray());

		archive.delete(JarFile.MANIFEST_NAME);

		archive.add(byteArrayAsset, JarFile.MANIFEST_NAME);
	}

	protected static File getFileFromArchive(Archive<?> archive)
		throws Exception {

		File archiveFile = File.createTempFile(
			archive.getName() + UUID.randomUUID(), ".jar");

		archive.as(ZipExporter.class).exportTo(archiveFile, true);

		return archiveFile;
	}

	@Inject
	private Instance<ImportPackageManager> _importPackageManagerInstance;

}