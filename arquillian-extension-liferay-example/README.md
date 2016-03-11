# Arquillian Liferay Extension Example

##What is this?

This is an example of how to use the Arquillian Liferay Extension.

This example will be executed in the following environment:

* Tomcat Server 7.0.62
  * JMX enabled and configured.
  * Tomcat Manager installed and configured.
* Liferay 7.0.0
* JUnit 4.12

##Creating a Liferay Portlet for testing

###Add Liferay, Portlet and OSGi dependencies to pom.xml

```xml
...
	<dependencies>
	....
		<dependency>
			<groupId>com.liferay.portal</groupId>
			<artifactId>portal-service</artifactId>
			<version>${liferay.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>com.liferay.portal</groupId>
			<artifactId>util-bridges</artifactId>
			<version>${liferay.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>com.liferay.portal</groupId>
			<artifactId>util-taglib</artifactId>
			<version>${liferay.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>com.liferay.portal</groupId>
			<artifactId>util-java</artifactId>
			<version>${liferay.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>org.osgi</groupId>
			<artifactId>org.osgi.core</artifactId>
			<version>${osgi.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>javax.portlet</groupId>
			<artifactId>portlet-api</artifactId>
			<version>2.0</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<version>3.0.1</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>javax.servlet.jsp</groupId>
			<artifactId>jsp-api</artifactId>
			<version>2.0</version>
			<scope>provided</scope>
		</dependency>
		....
	</dependencies>
...	
```

###Create a OSGi Service

For testing purpouse we are going to create a new OSGI Service that add two numbers.

First of all, we need to create a new Interface

```java
package com.liferay.arquillian.sample.service;

public interface SampleService {

	public int add(int a, int b);

}
```

And a new implementation for the interface

```java
package com.liferay.arquillian.sample.service;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = SampleService.class)
public class SampleServiceImpl implements SampleService {

	@Override
	public int add(int a, int b) {
		return a + b;
	}

}
```

###Create a Liferay MVC Portlet

Create a MVC Portlet that call the previous service

```java
package com.liferay.arquillian.sample.portlet;

import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.theme.ThemeDisplay;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;

import com.liferay.arquillian.sample.service.SampleService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.instanceable=false",
		"javax.portlet.display-name=Arquillian Sample Portlet",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=arquillian_sample_portlet",
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user"
	},
	service = Portlet.class
)
public class SamplePortlet extends MVCPortlet {

	public void add(ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		int firstParameter = ParamUtil.getInteger(
			actionRequest, "firstParameter");
		int secondParameter = ParamUtil.getInteger(
			actionRequest, "secondParameter");

		int result = _sampleService.add(firstParameter, secondParameter);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			actionRequest, "arquillian_sample_portlet", themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		portletURL.setParameter(
			"firstParameter", String.valueOf(firstParameter));
		portletURL.setParameter(
			"secondParameter", String.valueOf(secondParameter));
		portletURL.setParameter("result", String.valueOf(result));

		actionRequest.setAttribute(WebKeys.REDIRECT, portletURL.toString());
	}

	@Reference(unbind = "-")
	public void setSampleService(SampleService sampleService) {
		_sampleService = sampleService;
	}

	private SampleService _sampleService;

}
```

Create a view.jsp file:
```jsp
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %><%@
taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %><%@
taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %><%@
taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<portlet:defineObjects />

<liferay-theme:defineObjects />

<%
int firstParameter = ParamUtil.getInteger(request, "firstParameter", 1);
int secondParameter = ParamUtil.getInteger(request, "secondParameter", 1);
int result = ParamUtil.getInteger(request, "result");
%>

<portlet:actionURL name="add" var="portletURL" />

<p>
	<b>Sample Portlet is working!</b>
</p>

<aui:form action="<%= portletURL %>" method="post" name="fm">

	<aui:input inlineField="<%= true %>" label="" name="firstParameter" size="4" type="int" value="<%= firstParameter %>" />
	<span> + </span>
	<aui:input inlineField="<%= true %>" label="" name="secondParameter" size="4" type="int" value="<%= secondParameter %>" />
	<span> = </span>
	<span class="result"><%= result %></span>

	<aui:button type="submit" value="add" />
</aui:form>
```

###Create a BND file for deployment

For testing we will use [BND](http://www.aqute.biz/Code/Bnd) to create the package to be deployed, so we need to add a maven dependency to the pom.xml


```xml
	...
	<dependencies>
		...
		<dependency>
			<groupId>org.jboss.shrinkwrap.osgi</groupId>
			<artifactId>shrinkwrap-osgi</artifactId>
			<version>1.0.0-alpha-1</version>
			<scope>test</scope>
		</dependency>
		...
	</dependencies>
	...
```

And create a bnd-basic-portlet-test.bnd file:

```bnd
Bundle-Name: Basic Portlet Test
Bundle-SymbolicName: com.liferay.arquillian.sample
Bundle-Version: 1.0.0
Export-Package: com.liferay.arquillian.sample
Include-Resource:\
	target/classes,\
	META-INF/resources=src/main/resources/META-INF/resources

Require-Capability:\
 osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=http://java.sun.com/portlet_2_0))",\
 osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=http://liferay.com/tld/aui))",\
 osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=http://liferay.com/tld/portlet))",\
 osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=http://liferay.com/tld/theme))",\
 osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=http://liferay.com/tld/ui))",\
 osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))"

-dsannotations: *
```

##Configuration Steps

### Configure Liferay Tomcat Server

#### Enable and Configure JMX in tomcat

You can follow this [guide](https://tomcat.apache.org/tomcat-7.0-doc/monitoring.html#Enabling_JMX_Remote) to enable your JMX configuration in tomcat 

In the next example you can see a example of a **setenv** file that enable JMX in a Tomcat in the port 8099 without authentication:

```sh
CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xmx1024m -XX:MaxPermSize=256m"

JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=8099 -Dcom.sun.management.jmxremote.ssl=false"

CATALINA_OPTS="${CATALINA_OPTS} ${JMX_OPTS}"
```

## Create a test in Liferay with the Arquillian Liferay Extension

#### Add dependencies to pom.xml

```xml
...
	<dependencies>
	....
		<dependency>
			<groupId>com.liferay.arquillian</groupId>
			<artifactId>arquillian-container-liferay</artifactId>
			<version>1.0.0.Final-SNAPSHOT</version>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.jboss.arquillian.junit</groupId>
			<artifactId>arquillian-junit-container</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
			<version>4.12</version>
			<scope>test</scope>
		</dependency>
		....
	</dependencies>
...	
```
#### Create simple integration tests using the Arquillian Extension

Create a simple test that:

1) Inject SampleService
2) Test SampleService

```java
package com.liferay.arquillian.test;

import com.liferay.portal.kernel.exception.PortalException;

import java.io.File;
import java.io.IOException;

import com.liferay.arquillian.containter.liferay.remote.enricher.Inject;
import com.liferay.arquillian.sample.service.SampleService;
import com.liferay.shrinkwrap.osgi.api.BndProjectBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BasicPortletIntegrationTest {

	@Deployment
	public static JavaArchive create() {
		BndProjectBuilder bndProjectBuilder = ShrinkWrap.create(
			BndProjectBuilder.class);

		bndProjectBuilder.setBndFile(new File("bnd-basic-portlet-test.bnd"));

		bndProjectBuilder.generateManifest(true);

		return bndProjectBuilder.as(JavaArchive.class);
	}

	@Test
	public void testAdd() throws IOException, PortalException {
		int result = _sampleService.add(1, 3);

		Assert.assertEquals(4, result);
	}

	@Inject
	private SampleService _sampleService;

}
```

## Create simple functional tests using the Arquillian Extension

To create a functional test in Liferay with the Arquillian Liferay Extension we are going to follow this [guide](http://arquillian.org/guides/functional_testing_using_graphene/)

#### Add dependencies to pom.xml

First of all, we need to configure the pom.xml file to add the graphene-webdriver dependencies

```xml
...
	<dependencies>
	....
		<dependency>
			<groupId>org.jboss.arquillian.graphene</groupId>
			<artifactId>graphene-webdriver</artifactId>
			<version>2.1.0.Alpha2</version>
			<type>pom</type>
			<scope>test</scope>
		</dependency>
	....
	</dependencies>
...	
```

#### Add a easy way to execute tests between different browsers

First, create a profile for each desired browser

```xml
...
properties>
    <browser>phantomjs</browser> 
</properties>
...

<profiles>
...
    <profile>
       <id>firefox</id>
       <properties>
          <browser>firefox</browser>
       </properties>
    </profile>
    <profile>
       <id>chrome</id>
       <properties>
           <browser>chrome</browser>
       </properties>
    </profile>
...
</profiles>
```

Next you need to setup arquillian.xml in order to change the Arquillian settings for browser selection. Add the following to the arquillian.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<arquillian xmlns="http://jboss.org/schema/arquillian"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">

	<extension qualifier="webdriver">
		<property name="browser">${browser}</property>
	</extension>

</arquillian>
```

#### Create a Portlet functional test

```java
package com.liferay.arquillian.test;

import com.liferay.portal.kernel.exception.PortalException;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import com.liferay.arquillian.containter.liferay.remote.enricher.Inject;
import com.liferay.arquillian.installportlet.annotation.InstallPortlet;
import com.liferay.arquillian.sample.service.SampleService;
import com.liferay.shrinkwrap.osgi.api.BndProjectBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Cristina González
 */
@RunAsClient
@RunWith(Arquillian.class)
public class BasicPortletFunctionalTest {

	@Deployment
	public static JavaArchive create() {
		BndProjectBuilder bndProjectBuilder = ShrinkWrap.create(
			BndProjectBuilder.class);

		bndProjectBuilder.setBndFile(new File("bnd-basic-portlet-test.bnd"));

		bndProjectBuilder.generateManifest(true);

		return bndProjectBuilder.as(JavaArchive.class);
	}

	@Test
	public void testAdd() throws IOException, PortalException {
		browser.get(_portlerURL.toExternalForm());

		firstParamter.clear();

		firstParamter.sendKeys("2");

		secondParameter.clear();

		secondParameter.sendKeys("3");

		add.click();

		Assert.assertEquals("5", result.getText());
	}

	@Test
	public void testInstallPortlet() throws IOException, PortalException {
		browser.get(_portlerURL.toExternalForm());

		String bodyText = browser.findElement(By.tagName("body")).getText();

		Assert.assertTrue(
			"The portlet is not well deployed",
			bodyText.contains("Sample Portlet is working!"));
	}

	@InstallPortlet(name ="arquillian_sample_portlet")
	private URL _portlerURL;

	@Inject
	private SampleService _sampleService;

	@FindBy(css = "button[type=submit]")
	private WebElement add;

	@Drone
	private WebDriver browser;

	@FindBy(css = "input[id$='firstParameter']")
	private WebElement firstParamter;

	@FindBy(css = "span[class='result']")
	private WebElement result;

	@FindBy(css = "input[id$='secondParameter']")
	private WebElement secondParameter;
```

#### Configure the ArquillianResource

If we want to Inject the URL of the container using the annotation @ArquillianResource (and we don't want to define a deployment), we can use one of these solutions (if we are using the Arquillian Liferay Extension)

1) Create a deployment method in our test class.
2) Configure Arquillian using the graphene url property (via arquillian.xml, arquillian.properties or System Properties)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<arquillian xmlns="http://jboss.org/schema/arquillian"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">
...
	<extension qualifier="graphene">
		<property name="url">http://localhost:8080</property>
	</extension>
...
</arquillian>
```

##Create Code Coverage Reports with Jacoco

To create a code coverage report with Jacoco for the Liferay integration tests, we are going to follow this [guide](https://github.com/arquillian/arquillian-extension-jacoco)

###Create a Jacoco profile in your Maven pom.xml

Create a new profile *jacoco*  that configure he plugin ''jacoco-maven-plugin'' with the dependencies ''org.jacoco.core'' and ''arquillian-jacoco''.


```xml
...
<profile>
	<id>jacoco</id>
	<dependencies>
		<dependency>
			<groupId>org.jacoco</groupId>
			<artifactId>org.jacoco.core</artifactId>
			<version>0.7.4.201502262128</version>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.jboss.arquillian.extension</groupId>
			<artifactId>arquillian-jacoco</artifactId>
			<version>1.0.0.Alpha8</version>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.jacoco</groupId>
				<artifactId>jacoco-maven-plugin</artifactId>
				<version>0.7.4.201502262128</version>
				<executions>
					<execution>
						<goals>
							<goal>prepare-agent</goal>
						</goals>
					</execution>
					<execution>
						<id>report</id>
						<phase>prepare-package</phase>
						<goals>
							<goal>report</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</profile>
```

###Generate the HTML Jacoco report

```sh
mvn test -Pjacoco jacoco:report
```

