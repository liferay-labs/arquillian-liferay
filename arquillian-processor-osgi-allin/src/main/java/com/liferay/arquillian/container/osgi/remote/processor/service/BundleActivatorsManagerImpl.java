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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;

/**
 * @author Cristina González
 */
public class BundleActivatorsManagerImpl implements BundleActivatorsManager {

	@Override
	public List<String> getBundleActivators(Archive archive, String fileName)
		throws IOException {

		Node node = archive.get(fileName);

		List<String> bundleActivators = new ArrayList<>();

		if (node != null) {
			Asset asset = node.getAsset();

			bundleActivators.addAll(_getBundleActivators(asset.openStream()));
		}

		return bundleActivators;
	}

	@Override
	public void replaceBundleActivatorsFile(
			Archive archive, String fileName, List<String> bundleActivators)
		throws IOException {

		ByteArrayOutputStream bundleActivatorAsOutputStream =
			_getBundleActivatorAsOutputStream(bundleActivators);

		ByteArrayAsset byteArrayAsset = new ByteArrayAsset(
			bundleActivatorAsOutputStream.toByteArray());

		archive.delete(fileName);

		archive.add(byteArrayAsset, fileName);
	}

	private ByteArrayOutputStream _getBundleActivatorAsOutputStream(
			List<String> bundleActivators)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		for (String bundleActivator : bundleActivators) {
			sb.append(bundleActivator);
			sb.append("\n");
		}

		if (!bundleActivators.isEmpty()) {
			sb.setLength(sb.length() - 1);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		outputStream.write(sb.toString().getBytes());

		return outputStream;
	}

	private List<String> _getBundleActivators(InputStream is)
		throws IOException {

		List<String> bundleActivators = new ArrayList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;

		while ((line = reader.readLine()) != null) {
			bundleActivators.add(line);
		}

		reader.close();

		return bundleActivators;
	}

}