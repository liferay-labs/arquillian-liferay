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

package com.liferay.arquillian.portal.bundle.servlet;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.PortletPreferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Cristina González
 */
public class PortalURLServlet extends HttpServlet {

	public PortalURLServlet(
		CompanyLocalService companyLocalService,
		GroupLocalService groupLocalService,
		LayoutLocalService layoutLocalService,
		PortletPreferencesLocalService porletPreferencesLocalService,
		UserLocalService userLocalService) {

		_companyLocalService = companyLocalService;

		_groupLocalService = groupLocalService;

		_layoutLocalService = layoutLocalService;

		_portletPreferencesLocalService = porletPreferencesLocalService;

		_userLocalService = userLocalService;
	}

	@Override
	public void destroy() {
		if (_layouts != null) {
			for (Layout layout : _layouts) {
				try {
					_layoutLocalService.deleteLayout(
						layout.getPlid(), new ServiceContext());
				}
				catch (PortalException pe) {
					_logger.log(
						Level.WARNING,
						"Error trying to delete layout " + layout.getPlid(),
						pe);
				}
			}
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {

		String portletId = request.getParameter("portlet-id");

		response.setContentType("text/html");

		PrintWriter out = response.getWriter();
		out.println("<h1> Portlet ID: " + portletId + "</h1>");

		Company company = _companyLocalService.getCompanies().get(0);

		Group guestGroup = null;

		if (_layouts == null) {
			_layouts = new ArrayList<>();
		}

		try {
			guestGroup = _groupLocalService.getGroup(
				company.getCompanyId(), "Guest");

			User defaultUser = _userLocalService.getDefaultUser(
				company.getCompanyId());

			UUID uuid = UUID.randomUUID();

			Layout layout = _layoutLocalService.addLayout(
				defaultUser.getUserId(), guestGroup.getGroupId(), false, 0,
				uuid.toString(), null, null, "portlet", false,
				"/" + uuid.toString(), new ServiceContext());

			_layouts.add(layout);

			LayoutTypePortlet layoutTypePortlet =
				(LayoutTypePortlet)layout.getLayoutType();

			layoutTypePortlet.setLayoutTemplateId(
				defaultUser.getUserId(), "1_column");

			String portletIdAdded = layoutTypePortlet.addPortletId(
				defaultUser.getUserId(), portletId, false);

			long ownerId = 0;
			int ownerType = 3;

			PortletPreferences prefs =
				_portletPreferencesLocalService.getPreferences(
					company.getCompanyId(), ownerId, ownerType,
					layout.getPlid(), portletIdAdded);

			_portletPreferencesLocalService.updatePreferences(
				ownerId, ownerType, layout.getPlid(), portletIdAdded, prefs);

			_layoutLocalService.updateLayout(
				layout.getGroupId(), layout.isPrivateLayout(),
				layout.getLayoutId(), layout.getTypeSettings());

			response.sendRedirect("/"+uuid.toString());
		}
		catch (PortalException pe) {
			_logger.log(Level.SEVERE, pe.getMessage(), pe);
		}
	}

	@Override
	public void init() throws ServletException {
		//There are not init actions for this servlet
	}

	private static final Logger _logger = Logger.getLogger(
		PortalURLServlet.class.getName());

	private final transient CompanyLocalService _companyLocalService;
	private final transient GroupLocalService _groupLocalService;
	private final transient LayoutLocalService _layoutLocalService;
	private List<Layout> _layouts;
	private final transient PortletPreferencesLocalService
		_portletPreferencesLocalService;
	private final transient UserLocalService _userLocalService;

}