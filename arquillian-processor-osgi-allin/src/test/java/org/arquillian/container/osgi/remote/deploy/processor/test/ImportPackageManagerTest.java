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

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Packages;

import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManager;
import com.liferay.arquillian.container.osgi.remote.processor.service.ImportPackageManagerImpl;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.arquillian.container.osgi.remote.deploy.processor.test.util.ManifestUtil;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;

import org.osgi.framework.Constants;

/**
 * @author Cristina González
 */
public class ImportPackageManagerTest {

	@Test
	public void testCleanRepeatedImportsWithoutRepeatedImports()
		throws Exception {

		//given:

		JavaArchive javaArchive = createJavaArchive();

		ManifestUtil.createManifest(javaArchive);

		List<Archive<?>> archives = new ArrayList<>();

		archives.add(javaArchive);

		Analyzer analyzer = new Analyzer();

		try {
			List<File> files = new ArrayList<>();

			File archiveFile = getFileFromArchive(javaArchive);

			files.add(archiveFile);

			analyzer.setJar(archiveFile);

			Properties properties = new Properties();

			properties.setProperty(Constants.IMPORT_PACKAGE, "*,dummy.package");

			analyzer.setProperties(properties);

			for (Archive<?> classPathArchive : archives) {
				File classPathFile = getFileFromArchive(classPathArchive);

				analyzer.addClasspath(classPathFile);

				files.add(archiveFile);
			}

			analyzer.analyze();

			//when:
			_importPackageManager.cleanImports(
				analyzer.getImports(), analyzer.getClasspathExports());

			//then:
			Packages importsPackages = analyzer.getImports();

			int cont = countPaths(importsPackages, "dummy/package");

			Assert.assertEquals(1, cont);

			for (File file : files) {
				Files.deleteIfExists(Paths.get(file.toURI()));
			}
		}
		finally {
			analyzer.close();
		}
	}

	@Test
	public void testCleanRepeatedImportsWithRepeatedImports() throws Exception {
		//given:

		JavaArchive javaArchive = createJavaArchive();

		ManifestUtil.createManifest(javaArchive);

		List<Archive<?>> archives = new ArrayList<>();
		archives.add(javaArchive);

		Analyzer analyzer = new Analyzer();

		try {
			List<File> files = new ArrayList<>();

			File archiveFile = getFileFromArchive(javaArchive);

			files.add(archiveFile);

			analyzer.setJar(archiveFile);

			Properties properties = new Properties();

			properties.setProperty(
				Constants.IMPORT_PACKAGE,
				"*,dummy.package,another.package,*,dummy.package");

			analyzer.setProperties(properties);

			for (Archive<?> classPathArchive : archives) {
				File classPathFile = getFileFromArchive(classPathArchive);

				analyzer.addClasspath(classPathFile);

				files.add(archiveFile);
			}

			analyzer.analyze();

			//when:
			_importPackageManager.cleanImports(
				analyzer.getImports(), analyzer.getClasspathExports());

			//then:
			Packages importsPackages = analyzer.getImports();

			int cont = countPaths(importsPackages, "dummy/package");

			Assert.assertEquals(1, cont);

			for (File file : files) {
				Files.deleteIfExists(Paths.get(file.toURI()));
			}
		}
		finally {
			analyzer.close();
		}
	}

	protected static File getFileFromArchive(Archive<?> archive)
		throws Exception {

		File archiveFile = File.createTempFile(
			archive.getName() + UUID.randomUUID(), ".jar");

		archive.as(ZipExporter.class).exportTo(archiveFile, true);

		return archiveFile;
	}

	private int countPaths(Packages importsPackages, String needle) {
		int cont = 0;

		for (
			Map.Entry<Descriptors.PackageRef, Attrs> packageRefAttrsEntry :
				importsPackages.entrySet()) {

			Descriptors.PackageRef packageRef = packageRefAttrsEntry.getKey();

			String path = packageRef.getPath();

			if (path.equals(needle)) {
				cont++;
			}
		}

		return cont;
	}

	private JavaArchive createJavaArchive() {
		JavaArchive javaArchive = ShrinkWrap.create(
			JavaArchive.class, "dummy-jar.jar");

		javaArchive.addPackage(ImportPackageManagerTest.class.getPackage());

		try {
			ManifestUtil.createManifest(javaArchive);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		return javaArchive;
	}

	private static final ImportPackageManager _importPackageManager =
		new ImportPackageManagerImpl();

}