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

import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Packages;

import java.io.IOException;

/**
 * @author Cristina González
 */
public class ImportPackageManagerImpl implements ImportPackageManager {

	@Override
	public void cleanImports(Packages imports, Packages classpathExports)
		throws IOException {

		Packages importsFiltered = new Packages();

		for (Descriptors.PackageRef importPackage : imports.keySet()) {
			if (classpathExports.containsKey(importPackage)) {
				importsFiltered.put(importPackage);
			}
		}

		//Clean imports in classpath

		for (Descriptors.PackageRef packageRef : importsFiltered.keySet()) {
			imports.remove(packageRef);
		}

		importsFiltered = new Packages();

		for (Descriptors.PackageRef importPackage : imports.keySet()) {
			if (importPackage.getPath().endsWith("~")) {
				importsFiltered.put(importPackage);
			}
		}

		//Clean repeated imports

		for (Descriptors.PackageRef packageRef : importsFiltered.keySet()) {
			imports.remove(packageRef);
		}
	}

}