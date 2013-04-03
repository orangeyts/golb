/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.tests.TaskTestUtil;
import org.eclipse.mylyn.tasks.tests.connector.MockRepositoryConnector;

/**
 * @author Mik Kersten
 */
public class RepositoryTaskHandleTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		TaskTestUtil.resetTaskList();
	}

	@Override
	protected void tearDown() throws Exception {
		TaskTestUtil.resetTaskList();
	}

	public void testRepositoryUrlHandles() throws Exception {
		String taskId = "123";
		String repositoryUrl = IBugzillaConstants.ECLIPSE_BUGZILLA_URL;
		TaskRepository repository = new TaskRepository(MockRepositoryConnector.REPOSITORY_KIND, repositoryUrl);
		TasksUiPlugin.getRepositoryManager().addRepository(repository);

		ITask bugTask = new TaskTask(BugzillaCorePlugin.CONNECTOR_KIND, repositoryUrl, taskId);
		bugTask.setSummary("Summary");
		assertEquals(repositoryUrl, bugTask.getRepositoryUrl());

		TasksUiPlugin.getTaskList().addTask(bugTask);
		TaskTestUtil.saveAndReadTasklist();

		ITask readReport = TasksUiPlugin.getTaskList().getTask(repositoryUrl, taskId);
		assertEquals("Summary", readReport.getSummary());
		assertEquals(repositoryUrl, readReport.getRepositoryUrl());
		TasksUiPlugin.getRepositoryManager().removeRepository(repository,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}
}
