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

package com.liferay.arquillian.container.remote.installdependency;

import com.liferay.arquillian.containter.remote.LiferayRemoteContainerConfiguration;
import com.liferay.arquillian.portal.bundle.PortalURLBundleActivator;
import com.liferay.arquillian.portal.bundle.servlet.PortalURLServlet;
import com.liferay.hot.deploy.jmx.listener.mbean.manager.PluginMBeanManager;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;

import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.FrameworkMBean;

/**
 * @author Cristina González
 */
public class LiferayInstallDependenciesObserver {

	public static final boolean IS_WINDOWS =
		System.getProperty("os.name") != null &&
			StringUtil.toLowerCase(
				System.getProperty("os.name")
			).contains("windows");

	public void startContainer(@Observes StartContainer context)
		throws Exception {

		LiferayRemoteContainerConfiguration config =
			_configurationInstance.get();

		String dependencyPropertyFile = config.getDependencyPropertyFile();

		_installedBundles = new ArrayList<>();

		_initOSGiJMXAttributes(config);

		_initLiferayJMXAttributes();

		_installPortalDependencies();

		if (dependencyPropertyFile != null) {
			Path dependencyPropertyFilePath = Paths.get(dependencyPropertyFile);

			Charset charset = Charset.forName("UTF-8");

			try {
				List<String> dependencies = Files.readAllLines(
					dependencyPropertyFilePath, charset);

				String dependencyPath = "";

				for (String dependency : dependencies) {
					if (dependency.startsWith(_MAVEN_PREFIX)) {
						String mavenDependency = dependency.substring(
							_MAVEN_PREFIX.length() + 1);

						dependencyPath = _getMavenDependencyPath(
							mavenDependency);
					}
					else if (dependency.startsWith(_FILE_PREFIX)) {
						dependencyPath = dependency.substring(
							_FILE_PREFIX.length() + 1);
					}

					_installBundle(
						Paths.get(dependencyPath).toAbsolutePath().toString());
				}
			}
			catch (IOException ioe) {
				throw new LifecycleException(
					"Can't find file " +
						dependencyPropertyFilePath.toAbsolutePath(),
					ioe);
			}
		}
	}

	public void stopContainer(@Observes StopContainer context)
		throws LifecycleException {

		for (long bundleId : _installedBundles) {
			try {
				_frameworkMBean.uninstallBundle(bundleId);
			}
			catch (IOException ioe) {
				throw new LifecycleException("Can't uninstall bundle", ioe);
			}
		}
	}

	private void _awaitUntilBundleActive(long bundleId)
		throws InterruptedException, IOException, TimeoutException {

		long timeoutMillis = System.currentTimeMillis() + 3000;

		while (System.currentTimeMillis() < timeoutMillis) {
			if ("ACTIVE".equals(this._bundleStateMBean.getState(bundleId))) {
				return;
			}

			Thread.sleep(100L);
		}

		throw new TimeoutException(
			"The bundle with bundleId [" + bundleId + "] is not Active");
	}

	private void _awaitUntilLegacyPluginDeployed(String contextName)
		throws InterruptedException, IOException, TimeoutException {

		long timeoutMillis = System.currentTimeMillis() + 3000;

		while (System.currentTimeMillis() < timeoutMillis) {
			List<String> legacyPluginsList =
				_pluginsManagerMBean.listLegacyPlugins();

			if (legacyPluginsList.contains(contextName)) {
				return;
			}

			Thread.sleep(500L);
		}

		throw new TimeoutException(
			"The plugin [" + contextName + "] is not Well Deployed");
	}

	private String _getMavenDependencyPath(String mavenDependency) {
		String userHome = System.getProperty("user.home");

		ConfigurableMavenResolverSystem resolver = Maven.configureResolver();

		ConfigurableMavenResolverSystem resolverWithLocalRepo =
			resolver.withRemoteRepo(
				"local-m2", "file://" + userHome + "/.m2/repository",
				"default");

		ConfigurableMavenResolverSystem
			resolverWithLocalRepoAndLiferayRepo =
				resolverWithLocalRepo.withRemoteRepo(
					"liferay-public",
					"http://cdn.repository.liferay.com/" +
						"nexus/content/groups/public",
					"default");

		MavenStrategyStage resolve =
			resolverWithLocalRepoAndLiferayRepo.resolve(mavenDependency);

		MavenFormatStage mavenFormatStage = resolve.withoutTransitivity();

		File[] resolved = mavenFormatStage.asFile();

		return resolved[0].getAbsolutePath();
	}

	private <U> U _getMBeanProxy(
			final MBeanServerConnection mbeanServer, final ObjectName oname,
			final Class<U> type, final long timeout, final TimeUnit unit)
		throws TimeoutException {

		Callable<U> callable = new Callable<U>() {

			@Override
			public U call() throws Exception {
				IOException lastException = null;

				long timeoutMillis =
					System.currentTimeMillis() + unit.toMillis(timeout);

				while (System.currentTimeMillis() < timeoutMillis) {
					Set<ObjectName> names = mbeanServer.queryNames(oname, null);

					if (names.size() == 1) {
						ObjectName instanceName = names.iterator().next();
						return MBeanServerInvocationHandler.newProxyInstance(
							mbeanServer, instanceName, type, false);
					}
					else {
						Thread.sleep(500);
					}
				}

				_log.log(
					Level.WARNING, "Cannot get MBean proxy for type: " + oname,
					lastException);
				throw new TimeoutException();
			}

		};
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<U> future = executor.submit(callable);

		try {
			return future.get(timeout, unit);
		}
		catch (TimeoutException te) {
			throw te;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private MBeanServerConnection _getMBeanServerConnection(
			LiferayRemoteContainerConfiguration configuration)
		throws IOException {

		String[] credentials = new String[] {
			configuration.getJmxUsername(), configuration.getJmxPassword()
		};

		Map<String, ?> env = Collections.singletonMap(
			JMXConnector.CREDENTIALS, credentials);

		JMXServiceURL serviceURL = new JMXServiceURL(
			configuration.getJmxServiceURL());

		JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

		return connector.getMBeanServerConnection();
	}

	private MBeanServerConnection _getMBeanServerConnection(
			final LiferayRemoteContainerConfiguration configuration,
			final long timeout, final TimeUnit unit)
		throws TimeoutException {

		Callable<MBeanServerConnection> callable =
			new Callable<MBeanServerConnection>() {

				@Override
				public MBeanServerConnection call() throws Exception {
					Exception lastException = null;

					long timeoutMillis =
						System.currentTimeMillis() + unit.toMillis(timeout);

					while (System.currentTimeMillis() < timeoutMillis) {
						try {
							return _getMBeanServerConnection(configuration);
						}
						catch (Exception e) {
							lastException = e;
							Thread.sleep(500);
						}
					}

					TimeoutException timeoutException = new TimeoutException();
					timeoutException.initCause(lastException);
					throw timeoutException;
				}

			};

		ExecutorService executor = Executors.newSingleThreadExecutor();

		Future<MBeanServerConnection> future = executor.submit(callable);

		try {
			return future.get(timeout, unit);
		}
		catch (TimeoutException te) {
			throw te;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void _initLiferayJMXAttributes() throws LifecycleException {
		_installBundle(_getMavenDependencyPath(_HOT_DEPLOY_JMX_LISTENER_MVN));

		try {

			// Get the PluginsMBean

			ObjectName oname = new ObjectName(
				"com.liferay.portal.monitoring:classification=" +
					"plugin_statistics,name=PluginsManager");

			_pluginsManagerMBean = _getMBeanProxy(
				mbeanServerInstance.get(), oname, PluginMBeanManager.class, 30,
				TimeUnit.SECONDS);
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
			throw new LifecycleException(
				"Cannot get a Liferay JMX connection", e);
		}
	}

	private void _initOSGiJMXAttributes(
			LiferayRemoteContainerConfiguration configuration)
		throws LifecycleException {

		MBeanServerConnection mbeanServer = null;

		// Try to connect to an already running server

		try {
			mbeanServer = _getMBeanServerConnection(
				configuration, 30, TimeUnit.SECONDS);

			mbeanServerInstance.set(mbeanServer);
		}
		catch (TimeoutException te) {
			throw new LifecycleException(
				"Error connecting to Karaf MBeanServer: ", te);
		}

		try {

			// Get the FrameworkMBean

			ObjectName oname = new ObjectName("osgi.core:type=framework,*");
			_frameworkMBean = _getMBeanProxy(
				mbeanServer, oname, FrameworkMBean.class, 30, TimeUnit.SECONDS);

			// Get the BundleStateMBean

			oname = new ObjectName("osgi.core:type=bundleState,*");
			_bundleStateMBean = _getMBeanProxy(
				mbeanServer, oname, BundleStateMBean.class, 30,
				TimeUnit.SECONDS);
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
			throw new LifecycleException("Cannot start Karaf container", e);
		}
	}

	private void _installBundle(String filePath) throws LifecycleException {
		try {
			if (IS_WINDOWS) {
				filePath = filePath.replaceFirst("^[a-zA-Z]:", "/$0");
			}

			String pathWithProtocol = "file://" + filePath;

			String contextName = "";

			if (filePath.endsWith(".war")) {
				int x = filePath.lastIndexOf("/");
				int y = filePath.lastIndexOf(".war");

				contextName = filePath.substring(x + 1, y);

				Matcher matcher = _pattern.matcher(contextName);

				if (matcher.matches()) {
					contextName = matcher.group(1);
				}

				String pathWithQueryString =
					filePath + "?Web-ContextPath=/" + contextName;

				URL url = new URL("file", null, pathWithQueryString);

				pathWithProtocol = "webbundle:" + url.toString();
			}

			long bundleId = _frameworkMBean.installBundle(pathWithProtocol);

			_installedBundles.add(bundleId);

			_frameworkMBean.startBundle(bundleId);

			_awaitUntilBundleActive(bundleId);

			if (!contextName.isEmpty()) {
				_awaitUntilLegacyPluginDeployed(contextName);
			}
		}
		catch (IOException ioe) {
			if (ioe.getMessage().contains("A bundle is already installed")) {
				_log.warning("The bundle was already installed " + filePath);
			}
			else {
				throw new LifecycleException(
					"The bundle in the path " + filePath +
						" can't be found, so it can't be installed",
					ioe);
			}
		}
		catch (InterruptedException ie) {
			throw new LifecycleException("InterruptedException", ie);
		}
		catch (TimeoutException te) {
			throw new LifecycleException("Timeout exception", te);
		}
	}

	private void _installPortalDependencies() throws LifecycleException {
		JavaArchive archive = ShrinkWrap.create(
			JavaArchive.class, "arquillian-install-portlet-in-liferay.jar");

		archive.addClass(PortalURLBundleActivator.class);
		archive.addClass(PortalURLServlet.class);

		archive.setManifest(
			new Asset() {

				@Override
				public InputStream openStream() {
					OSGiManifestBuilder builder =
						OSGiManifestBuilder.newInstance();

					builder.addBundleManifestVersion(2);
					builder.addBundleSymbolicName(
						"arquillian-install-portlet-in-liferay");
					builder.addImportPackages(
						"com.liferay.portal.kernel.exception",
						"com.liferay.portal.kernel.util",
						"com.liferay.portal.kernel.model",
						"com.liferay.portal.kernel.service",
						"javax.servlet.http", "javax.portlet", "javax.servlet",
						"org.osgi.framework");
					builder.addBundleActivator(PortalURLBundleActivator.class);

					return builder.openStream();
				}

			});

		UUID uuid = UUID.randomUUID();

		File tmpfile = new File("tmpfiles" + uuid.toString() + ".jar");

		ZipExporter exporter = archive.as(ZipExporter.class);

		exporter.exportTo(tmpfile);

		_installBundle(tmpfile.getAbsolutePath());

		tmpfile.deleteOnExit();
	}

	private static final String _FILE_PREFIX = "file";

	private static final String _HOT_DEPLOY_JMX_LISTENER_MVN =
		"com.liferay:com.liferay.hot.deploy.jmx.listener:1.0.1";

	private static final String _MAVEN_PREFIX = "mvn";

	private static final Logger _log = Logger.getLogger(
		LiferayInstallDependenciesObserver.class.getName());

	private static final Pattern _pattern = Pattern.compile(
		"(.*?)(-\\d+\\.\\d+\\.\\d+\\.\\d+)?");

	private BundleStateMBean _bundleStateMBean;

	@ApplicationScoped
	@Inject
	private Instance<LiferayRemoteContainerConfiguration>
		_configurationInstance;

	@Inject
	private Instance<ContainerRegistry> _containerRegistryInstance;

	private FrameworkMBean _frameworkMBean;
	private List<Long> _installedBundles;
	private PluginMBeanManager _pluginsManagerMBean;

	@ContainerScoped
	@Inject
	private InstanceProducer<MBeanServerConnection> mbeanServerInstance;

}