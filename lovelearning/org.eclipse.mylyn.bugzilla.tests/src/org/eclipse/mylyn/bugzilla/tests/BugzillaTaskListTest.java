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

import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.tests.TaskTestUtil;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Mik Kersten
 */
public class BugzillaTaskListTest extends TestCase {

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TasksUiPlugin.getTaskList().reset();
		TasksUiPlugin.getExternalizationManager().save(true);
		repository = new TaskRepository(BugzillaCorePlugin.CONNECTOR_KIND, IBugzillaConstants.ECLIPSE_BUGZILLA_URL);
		TasksUiPlugin.getRepositoryManager().addRepository(repository);
		assertEquals(0, TasksUiPlugin.getTaskList().getAllTasks().size());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		TasksUiPlugin.getTaskList().reset();
		TasksUiPlugin.getExternalizationManager().save(true);
		TasksUiPlugin.getRepositoryManager().removeRepository(repository,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}

	@SuppressWarnings("null")
	public void testRemindedPersistance() throws Exception {

		String repositoryUrl = "https://bugs.eclipse.org/bugs";

		String bugNumber = "106939";

		ITask task = TasksUi.getRepositoryModel().createTask(repository, bugNumber);
		TaskTask task1 = null;
		if (task instanceof TaskTask) {
			task1 = (TaskTask) task;
		}
		assertNotNull(task1);

		TasksUiPlugin.getTaskList().addTask(task1);

		task1.setReminded(true);
		TaskTestUtil.saveAndReadTasklist();

		TaskList taskList = TasksUiPlugin.getTaskList();
		assertEquals(1, taskList.getAllTasks().size());
		Set<ITask> tasksReturned = taskList.getTasks(repositoryUrl);
		assertNotNull(tasksReturned);
		assertEquals(1, tasksReturned.size());
		for (ITask taskRet : tasksReturned) {
			assertTrue(((AbstractTask) taskRet).isReminded());
		}
	}

	@SuppressWarnings("null")
	public void testRepositoryTaskExternalization() throws Exception {
		ITask task = TasksUi.getRepositoryModel().createTask(repository, "1");
		TaskTask repositoryTask = null;
		if (task instanceof TaskTask) {
			repositoryTask = (TaskTask) task;
		}
		assertNotNull(repositoryTask);
		repositoryTask.setTaskKind("kind");
		TasksUiPlugin.getTaskList().addTask(repositoryTask);
		TaskTestUtil.saveAndReadTasklist();
		assertEquals(1, TasksUiPlugin.getTaskList()
				.getUnmatchedContainer(IBugzillaConstants.ECLIPSE_BUGZILLA_URL)
				.getChildren()
				.size());
		ITask readTask = TasksUiPlugin.getTaskList()
				.getUnmatchedContainer(IBugzillaConstants.ECLIPSE_BUGZILLA_URL)
				.getChildren()
				.iterator()
				.next();

		assertEquals(repositoryTask.getHandleIdentifier(), readTask.getHandleIdentifier());
		assertEquals(repositoryTask.getSummary(), readTask.getSummary());
		assertEquals(repositoryTask.getTaskKind(), readTask.getTaskKind());
	}

	public void testQueryExternalization() throws Exception {
		RepositoryQuery query = (RepositoryQuery) TasksUi.getRepositoryModel().createRepositoryQuery(repository);
		assertEquals("https://bugs.eclipse.org/bugs", query.getRepositoryUrl());
		assertEquals("<never>", query.getLastSynchronizedTimeStamp());
		query.setLastSynchronizedStamp("today");
		TasksUiPlugin.getTaskList().addQuery(query);

		TaskTestUtil.saveAndReadTasklist();
		assertEquals(1, TasksUiPlugin.getTaskList().getQueries().size());
		IRepositoryQuery readQuery = TasksUiPlugin.getTaskList().getQueries().iterator().next();
		assertEquals(query.getRepositoryUrl(), readQuery.getRepositoryUrl());
		assertEquals("today", query.getLastSynchronizedTimeStamp());
		assertEquals("https://bugs.eclipse.org/bugs", readQuery.getRepositoryUrl());
	}

	public void testDeleteQuery() {
		RepositoryQuery query = new RepositoryQuery(BugzillaCorePlugin.CONNECTOR_KIND, "queryUrl");
		query.setRepositoryUrl("repositoryUrl");
		TasksUiPlugin.getTaskList().addQuery(query);

		IRepositoryQuery readQuery = TasksUiPlugin.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);

		TasksUiPlugin.getTaskList().deleteQuery(query);
		assertEquals(0, TasksUiPlugin.getTaskList().getQueries().size());
	}

	public void testDeleteQueryAfterRename() {
		RepositoryQuery query = new RepositoryQuery(BugzillaCorePlugin.CONNECTOR_KIND, "queryUrl");
		query.setRepositoryUrl("repositoryUrl");
		TasksUiPlugin.getTaskList().addQuery(query);

		IRepositoryQuery readQuery = TasksUiPlugin.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);
		query.setSummary("newName");
		TasksUiPlugin.getTaskList().deleteQuery(query);
		assertEquals(0, TasksUiPlugin.getTaskList().getQueries().size());
	}

	public void testCreateQueryWithSameName() {
		RepositoryQuery query = new RepositoryQuery(BugzillaCorePlugin.CONNECTOR_KIND, "queryUrl");
		query.setRepositoryUrl("repositoryUrl");
		TasksUiPlugin.getTaskList().addQuery(query);
		assertEquals(1, TasksUiPlugin.getTaskList().getQueries().size());
		IRepositoryQuery readQuery = TasksUiPlugin.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);

		try {
			query = new RepositoryQuery(BugzillaCorePlugin.CONNECTOR_KIND, "queryUrl");
			query.setRepositoryUrl("repositoryUrl");
			TasksUiPlugin.getTaskList().addQuery(query);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			if (!e.getMessage().equals("Handle queryUrl already exists in task list")) {
				throw e;
			}
		}
		assertEquals(1, TasksUiPlugin.getTaskList().getQueries().size());
	}

}
