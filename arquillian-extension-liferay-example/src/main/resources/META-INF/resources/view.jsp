<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>

<%--
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
--%>

<%@ include file="/init.jsp" %>

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