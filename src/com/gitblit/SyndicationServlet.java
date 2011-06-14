/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit;

import java.text.MessageFormat;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.models.RepositoryModel;
import com.gitblit.utils.JGitUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.utils.SyndicationUtils;
import com.gitblit.wicket.WicketUtils;

public class SyndicationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private transient Logger logger = LoggerFactory.getLogger(SyndicationServlet.class);

	public static String asLink(String baseURL, String repository, String objectId, int length) {
		if (baseURL.length() > 0 && baseURL.charAt(baseURL.length() - 1) == '/') {
			baseURL = baseURL.substring(0, baseURL.length() - 1);
		}
		StringBuilder url = new StringBuilder();
		url.append(baseURL);
		url.append(Constants.SYNDICATION_SERVLET_PATH);
		url.append(repository);
		if (!StringUtils.isEmpty(objectId) || length > 0) {
			StringBuilder parameters = new StringBuilder("?");
			if (StringUtils.isEmpty(objectId)) {
				parameters.append("l=");
				parameters.append(length);
			} else {
				parameters.append("h=");
				parameters.append(objectId);
				if (length > 0) {
					parameters.append("&l=");
					parameters.append(length);
				}
			}
			url.append(parameters);
		}
		return url.toString();
	}
	
	public static String getTitle(String repository, String objectId) {
		String id = objectId;
		if (!StringUtils.isEmpty(id)) {
			if (id.startsWith(org.eclipse.jgit.lib.Constants.R_HEADS)) {
				id = id.substring(org.eclipse.jgit.lib.Constants.R_HEADS.length());
			} else if (id.startsWith(org.eclipse.jgit.lib.Constants.R_REMOTES)) {
				id = id.substring(org.eclipse.jgit.lib.Constants.R_REMOTES.length());
			} else if (id.startsWith(org.eclipse.jgit.lib.Constants.R_TAGS)) {
				id = id.substring(org.eclipse.jgit.lib.Constants.R_TAGS.length());
			}
		}
		return MessageFormat.format("{0} ({1})", repository, id);
	}

	private void processRequest(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException,
			java.io.IOException {

		String hostURL = WicketUtils.getHostURL(request);
		String url = request.getRequestURI().substring(request.getServletPath().length());
		if (url.charAt(0) == '/' && url.length() > 1) {
			url = url.substring(1);
		}
		String repositoryName = url;
		String objectId = request.getParameter("h");
		String l = request.getParameter("l");
		int length = GitBlit.getInteger(Keys.web.syndicationEntries, 25);
		if (StringUtils.isEmpty(objectId)) {
			objectId = org.eclipse.jgit.lib.Constants.HEAD;
		}
		if (!StringUtils.isEmpty(l)) {
			try {
				length = Integer.parseInt(l);
			} catch (NumberFormatException x) {
			}
		}

		Repository repository = GitBlit.self().getRepository(repositoryName);
		RepositoryModel model = GitBlit.self().getRepositoryModel(repositoryName);
		List<RevCommit> commits = JGitUtils.getRevLog(repository, objectId, 0, length);
		try {
			SyndicationUtils.toRSS(hostURL, getTitle(model.name, objectId), model.description,
					model.name, commits, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occurred during feed generation", e);
		}
	}

	@Override
	protected void doPost(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException,
			java.io.IOException {
		processRequest(request, response);
	}

	@Override
	protected void doGet(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException,
			java.io.IOException {
		processRequest(request, response);
	}
}