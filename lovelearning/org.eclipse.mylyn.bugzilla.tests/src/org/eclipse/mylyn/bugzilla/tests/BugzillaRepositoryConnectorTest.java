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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaOperation;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaStatus;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaTaskDataHandler;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.bugzilla.core.RepositoryConfiguration;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskAttachment;
import org.eclipse.mylyn.internal.tasks.core.data.FileTaskAttachmentSource;
import org.eclipse.mylyn.internal.tasks.core.sync.SynchronizationSession;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.search.RepositorySearchResult;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchHitCollector;
import org.eclipse.mylyn.internal.tasks.ui.util.AttachmentUtil;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.TaskMapping;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.data.ITaskDataWorkingCopy;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;

/**
 * @author Rob Elves
 * @author Frank Becker
 */
public class BugzillaRepositoryConnectorTest extends AbstractBugzillaTest {

	public void testSingleRetrievalFailure() throws CoreException {
		init32();
		ITask task = generateLocalTaskAndDownload("9999");
		assertTrue(((AbstractTask) task).getStatus().getMessage().contains(IBugzillaConstants.ERROR_MSG_INVALID_BUG_ID));
	}

//	public void testMultiRetrievalFailure() throws CoreException {
//		init32();
//		ITask task1 = TasksUi.getRepositoryModel().createTask(repository, "1");
//		ITask taskX = TasksUi.getRepositoryModel().createTask(repository, "9999");
//		ITask task2 = TasksUi.getRepositoryModel().createTask(repository, "2");
//		// FIXME task.setStale(true);
//		TasksUiPlugin.getTaskList().addTask(task1);
//		TasksUiPlugin.getTaskList().addTask(taskX);
//		TasksUiPlugin.getTaskList().addTask(task2);
//		Set<ITask> tasks = new HashSet<ITask>();
//		tasks.add(task1);
//		tasks.add(taskX);
//		tasks.add(task2);
//		TasksUiInternal.synchronizeTasks(connector, tasks, true, null);
//
//		//TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
//		assertNotNull(TasksUiPlugin.getTaskDataManager().getTaskData(task1));
//		assertNotNull(TasksUiPlugin.getTaskDataManager().getTaskData(task2));
//		assertNull(TasksUiPlugin.getTaskDataManager().getTaskData(taskX));
//		assertNull(((AbstractTask) task1).getStatus());
//		assertNull(((AbstractTask) task2).getStatus());
//		assertEquals(IBugzillaConstants.ERROR_MSG_NO_DATA_RETRIEVED, ((AbstractTask) taskX).getStatus().getMessage());
//	}

	public void testBugWithoutDescription218() throws Exception {
		init218();
		doBugWithoutDescription("57");
	}

	public void testBugWithoutDescription222() throws Exception {
		init222();
		doBugWithoutDescription("391");
	}

	public void testBugWithoutDescription32() throws Exception {
		init32();
		doBugWithoutDescription("293");
	}

	private void doBugWithoutDescription(String taskNumber) throws CoreException {
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		int numComment = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_COMMENT).size();

		String newCommentText = "new Comment for Bug Without an Description " + (new Date()).toString();
		TaskAttribute comment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		comment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(comment);

		// Submit changes
		submit(task, taskData, changed);
		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		int numCommentNew = taskData.getAttributeMapper()
				.getAttributesByType(taskData, TaskAttribute.TYPE_COMMENT)
				.size();
		assertEquals(numComment + 1, numCommentNew);
	}

	public void testAttachmentToken323() throws Exception {
		init323();
		ITask task = generateLocalTaskAndDownload("2");
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskAttribute attachment = taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT).get(0);
		assertNotNull(attachment);
		TaskAttribute obsolete = attachment.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		TaskAttribute token = attachment.getAttribute(BugzillaAttribute.TOKEN.getKey());
		assertNotNull(token);
		attachment.removeAttribute(BugzillaAttribute.TOKEN.getKey());
		token = attachment.getAttribute(BugzillaAttribute.TOKEN.getKey());
		assertNull(token);
		boolean oldObsoleteOn = obsolete.getValue().equals("1");
		if (oldObsoleteOn) {
			obsolete.setValue("0"); //$NON-NLS-1$
		} else {
			obsolete.setValue("1"); //$NON-NLS-1$
		}
		try {
			((BugzillaTaskDataHandler) connector.getTaskDataHandler()).postUpdateAttachment(repository, attachment,
					"update", new NullProgressMonitor());
			fail("CoreException expected but not reached");
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			IStatus status = e.getStatus();
			assertTrue(status instanceof BugzillaStatus);
			assertEquals(IBugzillaConstants.REPOSITORY_STATUS_SUSPICIOUS_ACTION, status.getCode());
		}

		task = generateLocalTaskAndDownload("2");
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		attachment = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT).get(0);
		assertNotNull(attachment);
		obsolete = attachment.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		token = attachment.getAttribute(BugzillaAttribute.TOKEN.getKey());
		assertNotNull(token);
		oldObsoleteOn = obsolete.getValue().equals("1");
		if (oldObsoleteOn) {
			obsolete.setValue("0"); //$NON-NLS-1$
		} else {
			obsolete.setValue("1"); //$NON-NLS-1$
		}
		try {
			((BugzillaTaskDataHandler) connector.getTaskDataHandler()).postUpdateAttachment(repository, attachment,
					"update", new NullProgressMonitor());
		} catch (CoreException e) {
			fail("CoreException expected reached");
		}

	}

	public void testObsoleteAttachment222() throws Exception {
		init222();
		doObsoleteAttachment("81");
	}

	public void testObsoleteAttachment32() throws Exception {
		init32();
		doObsoleteAttachment("2");
	}

	private void doObsoleteAttachment(String taskNumber) throws CoreException {
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskAttribute attachment = taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT).get(0);
		assertNotNull(attachment);
		TaskAttribute obsolete = attachment.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		boolean oldObsoleteOn = obsolete.getValue().equals("1");
		if (oldObsoleteOn) {
			obsolete.setValue("0"); //$NON-NLS-1$
		} else {
			obsolete.setValue("1"); //$NON-NLS-1$
		}
		((BugzillaTaskDataHandler) connector.getTaskDataHandler()).postUpdateAttachment(repository, attachment,
				"update", new NullProgressMonitor()); //$NON-NLS-1$

		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		attachment = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT).get(0);
		assertNotNull(attachment);
		obsolete = attachment.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		boolean newObsoleteOn = obsolete.getValue().equals("1");
		assertEquals(true, oldObsoleteOn != newObsoleteOn);
	}

	//testReassign Bugs
	//Version	BugNum	assigned				reporter
	//2.22		92		user@mylar.eclipse.org	tests@mylar.eclipse.org
	//3.0		 5		tests@mylar.eclipse.org	tests2@mylar.eclipse.org
	//3.1		 1		rob.elves@eclipse.org	tests@mylar.eclipse.org

	public void testReassign222() throws CoreException {
		init222();
		String taskNumber = "92";
		doReassignOld(taskNumber, "user@mylar.eclipse.org");
	}

	public void testReassign30() throws CoreException {
		init30();
		String taskNumber = "5";
		doReassignOld(taskNumber, "tests@mylyn.eclipse.org");
	}

	public void testReassign31() throws CoreException {
		init31();
		String taskNumber = "1";

		// Get the task
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskMapper mapper = new TaskMapper(taskData);

		if (mapper.getOwner().equals("rob.elves@eclipse.org")) {
			assertEquals("rob.elves@eclipse.org", mapper.getOwner());
			reassingToUser31(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());

			reassignToDefault31(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("rob.elves@eclipse.org", mapper.getOwner());
		} else if (mapper.getOwner().equals("tests2@mylyn.eclipse.org")) {
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());
			reassignToDefault31(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("rob.elves@eclipse.org", mapper.getOwner());

			reassingToUser31(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());
		} else {
			fail("Bug with unexpected user assigned");
		}
	}

	private void reassignToDefault31(ITask task, TaskData taskData) throws CoreException {
		// Modify it (reassignbycomponent)
		String newCommentText = "BugzillaRepositoryClientTest.testReassign31(): reassignbycomponent "
				+ (new Date()).toString();
		TaskAttribute comment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		comment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(comment);

		TaskAttribute assignee = taskData.getRoot().getAttribute(BugzillaAttribute.SET_DEFAULT_ASSIGNEE.getKey());
		assignee.setValue("1");
		changed.add(assignee);
		// Submit changes
		submit(task, taskData, changed);
	}

	private void reassingToUser31(ITask task, TaskData taskData) throws CoreException {
		// Modify it (reassign to tests2@mylyn.eclipse.org)
		String newCommentText = "BugzillaRepositoryClientTest.testReassign31(): reassign " + (new Date()).toString();
		TaskAttribute comment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		comment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(comment);

		TaskAttribute assignedAttribute = taskData.getRoot().getAttribute(BugzillaAttribute.ASSIGNED_TO.getKey());
		assignedAttribute.setValue("tests2@mylyn.eclipse.org");
		changed.add(assignedAttribute);

		// Submit changes
		submit(task, taskData, changed);
	}

	private void doReassignOld(String taskNumber, String defaultAssignee) throws CoreException {
		// Get the task
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskMapper mapper = new TaskMapper(taskData);

		if (mapper.getOwner().equals(defaultAssignee)) {
			assertEquals(defaultAssignee, mapper.getOwner());
			reassingToUserOld(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());

			reassignToDefaultOld(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals(defaultAssignee, mapper.getOwner());
		} else if (mapper.getOwner().equals("tests2@mylyn.eclipse.org")) {
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());
			reassignToDefaultOld(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals(defaultAssignee, mapper.getOwner());

			reassingToUserOld(task, taskData);
			task = generateLocalTaskAndDownload(taskNumber);
			assertNotNull(task);
			model = createModel(task);
			taskData = model.getTaskData();
			assertNotNull(taskData);
			mapper = new TaskMapper(taskData);
			assertEquals("tests2@mylyn.eclipse.org", mapper.getOwner());
		} else {
			fail("Bug with unexpected user assigned");
		}
	}

	private void reassignToDefaultOld(ITask task, TaskData taskData) throws CoreException {
		// Modify it (reassignbycomponent)
		String newCommentText = "BugzillaRepositoryClientTest.testReassignOld(): reassignbycomponent "
				+ (new Date()).toString();
		TaskAttribute comment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		comment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(comment);
		TaskAttribute selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.reassignbycomponent.toString(),
				BugzillaOperation.reassignbycomponent.getLabel());
		changed.add(selectedOperationAttribute);

		// Submit changes
		submit(task, taskData, null);
	}

	private void reassingToUserOld(ITask task, TaskData taskData) throws CoreException {
		// Modify it (reassign to tests2@mylyn.eclipse.org)
		String newCommentText = "BugzillaRepositoryClientTest.testReassignOld(): reassign " + (new Date()).toString();
		TaskAttribute comment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		comment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(comment);
		TaskAttribute selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.reassign.toString(),
				BugzillaOperation.reassign.getLabel());
		TaskAttribute assignedAttribute = taskData.getRoot().getAttribute("reassignInput");
		assignedAttribute.setValue("tests2@mylyn.eclipse.org");
		changed.add(selectedOperationAttribute);
		changed.add(assignedAttribute);

		// Submit changes
		submit(task, taskData, null);
	}

	/*
	 * Test for the following State transformation
	 * NEW -> ASSIGNED -> RESOLVED DUPLICATE -> VERIFIED -> CLOSED -> REOPENED -> RESOLVED FIXED
	 * 
	 */

	private void doStdWorkflow(String DupBugID) throws Exception {
		final TaskMapping taskMappingInit = new TaskMapping() {

			@Override
			public String getProduct() {
				return "TestProduct";
			}
		};
		final TaskMapping taskMappingSelect = new TaskMapping() {
			@Override
			public String getComponent() {
				return "TestComponent";
			}

			@Override
			public String getSummary() {
				return "test the std workflow";
			}

			@Override
			public String getDescription() {
				return "The Description of the std workflow task";
			}

		};

		TasksUiPlugin.getRepositoryManager().addRepository(repository);
		final TaskData[] taskDataNew = new TaskData[1];
		// create Task
		taskDataNew[0] = TasksUiInternal.createTaskData(repository, taskMappingInit, taskMappingSelect, null);
		ITask taskNew = TasksUiUtil.createOutgoingNewTask(taskDataNew[0].getConnectorKind(),
				taskDataNew[0].getRepositoryUrl());

		//set Color to a legal value if exists
		TaskAttribute colorAttribute = taskDataNew[0].getRoot().getAttribute("cf_colors");
		if (colorAttribute != null) {
			colorAttribute.setValue("Green");
		}

		ITaskDataWorkingCopy workingCopy = TasksUi.getTaskDataManager().createWorkingCopy(taskNew, taskDataNew[0]);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		workingCopy.save(changed, null);
		RepositoryResponse response = submit(taskNew, taskDataNew[0], changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_CREATED.toString(), response.getReposonseKind().toString());
		String taskId = response.getTaskId();

		// change Status from NEW -> ASSIGNED
		ITask task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskAttribute statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("NEW", statusAttribute.getValue());
		TaskAttribute selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.accept.toString(),
				BugzillaOperation.accept.getLabel());
		model.attributeChanged(selectedOperationAttribute);
		changed.clear();
		changed.add(selectedOperationAttribute);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// change Status from ASSIGNED -> RESOLVED DUPLICATE
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("ASSIGNED", statusAttribute.getValue());
		selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.duplicate.toString(),
				BugzillaOperation.duplicate.getLabel());
		TaskAttribute duplicateAttribute = taskData.getRoot().getAttribute("dup_id");
		duplicateAttribute.setValue(DupBugID);
		model.attributeChanged(selectedOperationAttribute);
		model.attributeChanged(duplicateAttribute);
		changed.clear();
		changed.add(selectedOperationAttribute);
		changed.add(duplicateAttribute);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// change Status from RESOLVED DUPLICATE -> VERIFIED
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("RESOLVED", statusAttribute.getValue());
		TaskAttribute resolution = taskData.getRoot().getMappedAttribute(TaskAttribute.RESOLUTION);
		assertEquals("DUPLICATE", resolution.getValue());
		selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.verify.toString(),
				BugzillaOperation.verify.getLabel());
		model.attributeChanged(selectedOperationAttribute);
		changed.clear();
		changed.add(selectedOperationAttribute);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// change Status from VERIFIED -> CLOSE
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("VERIFIED", statusAttribute.getValue());
		selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.close.toString(),
				BugzillaOperation.close.getLabel());
		model.attributeChanged(selectedOperationAttribute);
		changed.clear();
		changed.add(selectedOperationAttribute);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// change Status from CLOSE -> REOPENED
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("CLOSED", statusAttribute.getValue());
		selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.reopen.toString(),
				BugzillaOperation.reopen.getLabel());
		model.attributeChanged(selectedOperationAttribute);
		changed.clear();
		changed.add(selectedOperationAttribute);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// change Status from REOPENED -> RESOLVED FIXED
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("REOPENED", statusAttribute.getValue());
		selectedOperationAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(selectedOperationAttribute, BugzillaOperation.resolve.toString(),
				BugzillaOperation.resolve.getLabel());
		resolution = taskData.getRoot().getMappedAttribute(TaskAttribute.RESOLUTION);
		resolution.setValue("FIXED");
		model.attributeChanged(selectedOperationAttribute);
		model.attributeChanged(resolution);
		changed.clear();
		changed.add(selectedOperationAttribute);
		changed.add(resolution);
		workingCopy.save(changed, null);
		response = submit(taskNew, taskData, changed);
		assertNotNull(response);
		assertEquals(ResponseKind.TASK_UPDATED.toString(), response.getReposonseKind().toString());

		// test last state
		task = generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		statusAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		assertEquals("RESOLVED", statusAttribute.getValue());
		resolution = taskData.getRoot().getMappedAttribute(TaskAttribute.RESOLUTION);
		assertEquals("FIXED", resolution.getValue());
	}

	public void testStdWorkflow222() throws Exception {
		init222();
		doStdWorkflow("101");
	}

	public void testStdWorkflow32() throws Exception {
		init32();
		doStdWorkflow("3");
	}

	public void testAttachToExistingReport() throws Exception {
		init222();
		String taskNumber = "33";
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
		assertEquals(taskNumber, taskData.getTaskId());
		int numAttached = taskData.getAttributeMapper()
				.getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT)
				.size();
		String fileName = "test-attach-" + System.currentTimeMillis() + ".txt";

		assertNotNull(repository.getCredentials(AuthenticationType.REPOSITORY));
		assertNotNull(repository.getCredentials(AuthenticationType.REPOSITORY).getUserName());
		assertNotNull(repository.getCredentials(AuthenticationType.REPOSITORY).getPassword());
		BugzillaClient client = connector.getClientManager().getClient(repository, new NullProgressMonitor());

		TaskAttribute attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		TaskAttachmentMapper attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		/* Initialize a local attachment */
		attachmentMapper.setDescription("Test attachment " + new Date());
		attachmentMapper.setContentType("text/plain");
		attachmentMapper.setPatch(false);
		attachmentMapper.setComment("Automated JUnit attachment test");
		attachmentMapper.applyTo(attrAttachment);

		/* Test attempt to upload a non-existent file */
		String filePath = "/this/is/not/a/real-file";

		FileTaskAttachmentSource attachment = new FileTaskAttachmentSource(new File(filePath));
		attachment.setContentType(FileTaskAttachmentSource.APPLICATION_OCTET_STREAM);
		attachment.setDescription(AttachmentUtil.CONTEXT_DESCRIPTION);
		attachment.setName("mylyn-context.zip");

		try {
			client.postAttachment(taskNumber, attachmentMapper.getComment(), attachment, attrAttachment,
					new NullProgressMonitor());
			fail("never reach this!");
		} catch (Exception e) {
			assertEquals("A repository error has occurred.", e.getMessage());
		}
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

		task = TasksUi.getRepositoryModel().createTask(repository, taskNumber);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		assertEquals(numAttached, taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT).size());

		/* Test attempt to upload an empty file */
		File attachFile = new File(fileName);
		attachFile.createNewFile();
		BufferedWriter write = new BufferedWriter(new FileWriter(attachFile));

		attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType(FileTaskAttachmentSource.APPLICATION_OCTET_STREAM);
		attachment.setDescription(AttachmentUtil.CONTEXT_DESCRIPTION);
		attachment.setName("mylyn-context.zip");

		try {
			client.postAttachment(taskNumber, attachmentMapper.getComment(), attachment, attrAttachment,
					new NullProgressMonitor());
			fail("never reach this!");
		} catch (Exception e) {
			assertEquals("A repository error has occurred.", e.getMessage());
		}
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		assertEquals(numAttached, taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT).size());

		/* Test uploading a proper file */
		write.write("test file");
		write.close();
		try {
			client.postAttachment(taskNumber, attachmentMapper.getComment(), attachment, attrAttachment,
					new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		model = createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		assertEquals(numAttached + 1, taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT).size());
		// use assertion to track clean-up
		assertTrue(attachFile.delete());
	}

	public void testDataRetrieval() throws CoreException, ParseException {
		init30();
		TaskData data = connector.getTaskData(repository, "2", new NullProgressMonitor());
		assertNotNull(data);
		ITaskMapping mapper = connector.getTaskMapping(data);
		assertEquals("2", data.getTaskId());
		assertEquals("New bug submit", mapper.getSummary());
		assertEquals("Test new bug submission", mapper.getDescription());
		assertEquals(PriorityLevel.P2, mapper.getPriorityLevel());
		assertEquals("TestComponent", mapper.getComponent());
		assertEquals("nhapke@cs.ubc.ca", mapper.getOwner());
		assertEquals("TestProduct", mapper.getProduct());
		assertEquals("PC", mapper.getTaskData()
				.getRoot()
				.getMappedAttribute(BugzillaAttribute.REP_PLATFORM.getKey())
				.getValue());
		assertEquals("Windows", mapper.getTaskData()
				.getRoot()
				.getMappedAttribute(BugzillaAttribute.OP_SYS.getKey())
				.getValue());
		assertEquals("ASSIGNED", mapper.getTaskData().getRoot().getMappedAttribute(
				BugzillaAttribute.BUG_STATUS.getKey()).getValue());
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		assertEquals(format1.parse("2007-03-20 16:37"), mapper.getCreationDate());
		assertEquals(format2.parse("2008-09-24 13:33:02"), mapper.getModificationDate());

		assertEquals(IBugzillaTestConstants.TEST_BUGZILLA_30_URL, mapper.getTaskUrl());
		assertEquals(BugzillaCorePlugin.CONNECTOR_KIND, mapper.getTaskKind());
		assertEquals("2", mapper.getTaskKey());

		// test comments
		List<TaskAttribute> comments = data.getAttributeMapper().getAttributesByType(data, TaskAttribute.TYPE_COMMENT);
		assertEquals(12, comments.size());
		TaskCommentMapper commentMap = TaskCommentMapper.createFrom(comments.get(0));
		assertEquals("Rob Elves", commentMap.getAuthor().getName());
		assertEquals("Created an attachment (id=1)\ntest\n\ntest attachments", commentMap.getText());
		commentMap = TaskCommentMapper.createFrom(comments.get(10));
		assertEquals("Tests", commentMap.getAuthor().getName());
		assertEquals("test", commentMap.getText());
	}

	public void testMidAirCollision() throws Exception {
		init30();
		String taskNumber = "5";

		// Get the task
		ITask task = generateLocalTaskAndDownload(taskNumber);

		ITaskDataWorkingCopy workingCopy = TasksUiPlugin.getTaskDataManager().getWorkingCopy(task);
		TaskData taskData = workingCopy.getLocalData();
		assertNotNull(taskData);

		String newCommentText = "BugzillaRepositoryClientTest.testMidAirCollision(): test " + (new Date()).toString();
		TaskAttribute attrNewComment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		attrNewComment.setValue(newCommentText);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(attrNewComment);
		TaskAttribute attrDeltaTs = taskData.getRoot().getMappedAttribute(TaskAttribute.DATE_MODIFICATION);
		attrDeltaTs.setValue("2007-01-01 00:00:00");
		changed.add(attrDeltaTs);

		workingCopy.save(changed, new NullProgressMonitor());

		try {
			// Submit changes
			submit(task, taskData, changed);
			fail("Mid-air collision expected");
		} catch (CoreException e) {
			assertTrue(e.getStatus().getMessage().indexOf("Mid-air collision occurred while submitting") != -1);
			return;
		}
		fail("Mid-air collision expected");
	}

	public void testAuthenticationCredentials() throws Exception {
		init218();
		ITask task = generateLocalTaskAndDownload("3");
		assertNotNull(task);
		TasksUiPlugin.getTaskActivityManager().activateTask(task);
		File sourceContextFile = ContextCorePlugin.getContextStore().getFileForContext(task.getHandleIdentifier());
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
		sourceContextFile.createNewFile();
		sourceContextFile.deleteOnExit();
		AuthenticationCredentials oldCreds = repository.getCredentials(AuthenticationType.REPOSITORY);
		AuthenticationCredentials wrongCreds = new AuthenticationCredentials("wrong", "wrong");
		repository.setCredentials(AuthenticationType.REPOSITORY, wrongCreds, false);
		TasksUiPlugin.getRepositoryManager().notifyRepositorySettingsChanged(repository);
		TaskData taskData = TasksUiPlugin.getTaskDataManager().getTaskData(task);
		TaskAttributeMapper mapper = taskData.getAttributeMapper();
		TaskAttribute attribute = mapper.createTaskAttachment(taskData);
		try {
			AttachmentUtil.postContext(connector, repository, task, "test", attribute, new NullProgressMonitor());
		} catch (CoreException e) {
			assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
			assertTrue(e.getStatus().getMessage().indexOf("invalid username or password") != -1);
			return;
		} finally {
			repository.setCredentials(AuthenticationType.REPOSITORY, oldCreds, false);
			TasksUiPlugin.getRepositoryManager().notifyRepositorySettingsChanged(repository);
		}
		fail("Should have failed due to invalid userid and password.");
	}

	public void testSynchronize() throws CoreException, Exception {
		init222();

		// Get the task
		ITask task = generateLocalTaskAndDownload("3");
		TasksUi.getTaskDataManager().discardEdits(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);

//		int numComments = taskData.getAttributeMapper()
//				.getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT)
//				.size();

		// Modify it
		String newCommentText = "BugzillaRepositoryClientTest.testSynchronize(): " + (new Date()).toString();
		TaskAttribute attrNewComment = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		attrNewComment.setValue(newCommentText);
		model.attributeChanged(attrNewComment);
		model.save(new NullProgressMonitor());
		assertEquals(SynchronizationState.OUTGOING, task.getSynchronizationState());
		submit(model);
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

//		TaskData taskData2 = workingCopy.getRepositoryData();
//		TaskMapper taskData2Mapper = new TaskMapper(taskData2);
//		TaskMapper taskData1Mapper = new TaskMapper(taskData);
//		assertFalse(taskData2Mapper.getModificationDate().equals(taskData1Mapper.getModificationDate()));
//		// Still not read
//		assertFalse(taskData2.getLastModified().equals(task.getLastReadTimeStamp()));
//		TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
//		assertEquals(taskData2.getLastModified(), task.getLastReadTimeStamp());
//		assertTrue(taskData2.getComments().size() > numComments);
//
//		// Has no outgoing changes or conflicts yet needs synch
//		// because task doesn't have bug report (new query hit)
//		// Result: retrieved with no incoming status
//		// task.setSyncState(SynchronizationState.SYNCHRONIZED);
//		TasksUiPlugin.getTaskDataStorageManager().remove(task.getRepositoryUrl(), task.getTaskId());
//		TasksUiInternal.synchronizeTask(connector, task, false, null);
//		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
//		RepositoryTaskData bugReport2 = null;
//		bugReport2 = TasksUiPlugin.getTaskDataStorageManager()
//				.getNewTaskData(task.getRepositoryUrl(), task.getTaskId());
//		assertNotNull(bugReport2);
//		assertEquals(task.getTaskId(), bugReport2.getTaskId());
//
//		assertEquals(newCommentText, bugReport2.getComments().get(numComments).getText());
//		// TODO: Test that comment was appended
//		// ArrayList<Comment> comments = task.getTaskData().getComments();
//		// assertNotNull(comments);
//		// assertTrue(comments.size() > 0);
//		// Comment lastComment = comments.get(comments.size() - 1);
//		// assertEquals(newCommentText, lastComment.getText());

	}

	public void testMissingHits() throws Exception {
		init323();
		String queryString = "http://mylyn.eclipse.org/bugs323/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=&product=TestProduct&long_desc_type=substring&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&deadlinefrom=&deadlineto=&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailassigned_to1=1&emailtype1=substring&email1=&emailassigned_to2=1&emailreporter2=1&emailcc2=1&emailtype2=substring&email2=&bugidtype=include&bug_id=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=";
		RepositoryQuery query = new RepositoryQuery(BugzillaCorePlugin.CONNECTOR_KIND, "test");
		query.setRepositoryUrl(repository.getRepositoryUrl());
		query.setUrl(queryString);
		TasksUiPlugin.getTaskList().addQuery(query);

		TasksUiInternal.synchronizeQuery(connector, query, null, true);

		for (ITask task : query.getChildren()) {
			assertTrue(task.getSynchronizationState() == SynchronizationState.INCOMING_NEW);
			TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
			assertTrue(task.getSynchronizationState() == SynchronizationState.SYNCHRONIZED);
		}

		repository.setSynchronizationTimeStamp("1970-01-01");
		TasksUiInternal.synchronizeQuery(connector, query, null, true);
		for (ITask task : query.getChildren()) {
			assertTrue(task.getSynchronizationState() == SynchronizationState.SYNCHRONIZED);
		}
	}

	ITask fruitTask;

	TaskData fruitTaskData;

	private void setFruitValueTo(String newValue) throws CoreException {
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		TaskAttribute cf_fruit = fruitTaskData.getRoot().getAttribute("cf_fruit");
		cf_fruit.setValue(newValue);
		assertEquals(newValue, fruitTaskData.getRoot().getAttribute("cf_fruit").getValue());
		changed.add(cf_fruit);
		submit(fruitTask, fruitTaskData, changed);
		TasksUiInternal.synchronizeTask(connector, fruitTask, true, null);
		fruitTaskData = TasksUiPlugin.getTaskDataManager().getTaskData(repository, fruitTask.getTaskId());
		assertEquals(newValue, fruitTaskData.getRoot().getAttribute("cf_fruit").getValue());

	}

	public void testCustomFields() throws Exception {
		init(IBugzillaTestConstants.TEST_BUGZILLA_303_URL);

		String taskNumber = "1";

		// Get the task
		fruitTask = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(fruitTask);
		TaskDataModel model = createModel(fruitTask);
		fruitTaskData = model.getTaskData();
		assertNotNull(fruitTaskData);

		if (fruitTaskData.getRoot().getAttribute("cf_fruit").getValue().equals("---")) {
			setFruitValueTo("apple");
			setFruitValueTo("orange");
			setFruitValueTo("---");
		} else if (fruitTaskData.getRoot().getAttribute("cf_fruit").getValue().equals("apple")) {
			setFruitValueTo("orange");
			setFruitValueTo("apple");
			setFruitValueTo("---");
		} else if (fruitTaskData.getRoot().getAttribute("cf_fruit").getValue().equals("orange")) {
			setFruitValueTo("apple");
			setFruitValueTo("orange");
			setFruitValueTo("---");
		}
		if (fruitTask != null) {
			fruitTask = null;
		}
		if (fruitTaskData != null) {
			fruitTaskData = null;
		}
	}

	public void testAnonymousRepositoryAccess() throws Exception {
		init218();
		assertNotNull(repository);
		AuthenticationCredentials anonymousCreds = new AuthenticationCredentials("", "");
		repository.setCredentials(AuthenticationType.REPOSITORY, anonymousCreds, false);
		TasksUiPlugin.getRepositoryManager().notifyRepositorySettingsChanged(repository);
		// test anonymous task retrieval
		ITask task = this.generateLocalTaskAndDownload("2");
		assertNotNull(task);

		// // test anonymous query (note that this demonstrates query via
		// eclipse search (ui)

		String queryUrl = "http://mylyn.eclipse.org/bugs218/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=search-match-test&product=TestProduct&long_desc_type=substring&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&deadlinefrom=&deadlineto=&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailassigned_to1=1&emailtype1=substring&email1=&emailassigned_to2=1&emailreporter2=1&emailcc2=1&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=";
		IRepositoryQuery bugzillaQuery = TasksUi.getRepositoryModel().createRepositoryQuery(repository);
		bugzillaQuery.setUrl(queryUrl);
		SearchHitCollector collector = new SearchHitCollector(taskList, repository, bugzillaQuery);
		RepositorySearchResult result = (RepositorySearchResult) collector.getSearchResult();

		collector.run(new NullProgressMonitor());
		assertEquals(2, result.getElements().length);

		for (Object element : result.getElements()) {
			assertEquals(true, element instanceof ITask);
			ITask hit = (ITask) element;
			assertTrue(hit.getSummary().contains("search-match-test"));
		}

		// test anonymous update of configuration
		RepositoryConfiguration config = BugzillaCorePlugin.getRepositoryConfiguration(repository, false, null);
		assertNotNull(config);
		assertTrue(config.getComponents().size() > 0);
	}

	public void testUpdate() throws Exception {
		init222();
		String taskNumber = "3";
		ITask task = generateLocalTaskAndDownload(taskNumber);
		TasksUi.getTaskDataManager().discardEdits(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);

		assertEquals("search-match-test 2", task.getSummary());
		assertEquals("TestProduct", task.getAttribute(TaskAttribute.PRODUCT));
		assertEquals("P1", task.getPriority());
		assertEquals("blocker", task.getAttribute(BugzillaAttribute.BUG_SEVERITY.getKey()));
		assertEquals("nhapke@cs.ubc.ca", task.getOwner());
		assertFalse(task.isCompleted());
		assertEquals("http://mylyn.eclipse.org/bugs222/show_bug.cgi?id=3", task.getUrl());
	}

	/**
	 * Ensure obsoletes and patches are marked as such by the parser.
	 */
	public void testAttachmentAttributes() throws Exception {
		init222();
		String taskNumber = "19";
		ITask task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);

		boolean isPatch[] = { false, true, false, false, false, false, false, true, false, false, false };
		boolean isObsolete[] = { false, true, false, true, false, false, false, false, false, false, false };

		int index = 0;
		for (TaskAttribute attribute : taskData.getAttributeMapper().getAttributesByType(taskData,
				TaskAttribute.TYPE_ATTACHMENT)) {
			assertTrue(validateAttachmentAttributes(model, attribute, isPatch[index], isObsolete[index]));
			index++;
		}
	}

	private boolean validateAttachmentAttributes(TaskDataModel model, TaskAttribute taskAttribute, boolean isPatch,
			boolean isObsolete) {
		TaskAttachment taskAttachment = new TaskAttachment(model.getTaskRepository(), model.getTask(), taskAttribute);
		model.getTaskData().getAttributeMapper().updateTaskAttachment(taskAttachment, taskAttribute);
		return (taskAttachment.isPatch() == isPatch) && (taskAttachment.isDeprecated() == isObsolete);
	}

	public void testTimeTracker222() throws Exception {
		init222();
		timeTracker(15, true);
	}

	public void testTimeTracker218() throws Exception {
		init218();
		timeTracker(20, false);
	}

	/**
	 * @param enableDeadline
	 *            bugzilla 218 doesn't support deadlines
	 */
	protected void timeTracker(int taskid, boolean enableDeadline) throws Exception {
		ITask task = generateLocalTaskAndDownload("" + taskid);
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		assertEquals(taskid + "", taskData.getTaskId());
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

		Set<ITask> tasks = new HashSet<ITask>();
		tasks.add(task);

		synchAndAssertState(tasks, SynchronizationState.SYNCHRONIZED);

		float estimatedTime, remainingTime, actualTime, addTime;
		String deadline = null;

		estimatedTime = Float.parseFloat(taskData.getRoot()
				.getAttribute(BugzillaAttribute.ESTIMATED_TIME.getKey())
				.getValue());
		remainingTime = Float.parseFloat(taskData.getRoot()
				.getAttribute(BugzillaAttribute.REMAINING_TIME.getKey())
				.getValue());
		actualTime = Float.parseFloat(taskData.getRoot()
				.getAttribute(BugzillaAttribute.ACTUAL_TIME.getKey())
				.getValue());

		if (enableDeadline) {
			deadline = taskData.getRoot().getAttribute(BugzillaAttribute.DEADLINE.getKey()).getValue();
		}

		estimatedTime += 2;
		remainingTime += 1.5;
		addTime = 0.75f;
		if (enableDeadline) {
			deadline = generateNewDay();
		}

		taskData.getRoot().getAttribute(BugzillaAttribute.ESTIMATED_TIME.getKey()).setValue("" + estimatedTime);
		taskData.getRoot().getAttribute(BugzillaAttribute.REMAINING_TIME.getKey()).setValue("" + remainingTime);
		TaskAttribute workTime = taskData.getRoot().getAttribute(BugzillaAttribute.WORK_TIME.getKey());
		if (workTime == null) {
			BugzillaTaskDataHandler.createAttribute(taskData.getRoot(), BugzillaAttribute.WORK_TIME);
			workTime = taskData.getRoot().getAttribute(BugzillaAttribute.WORK_TIME.getKey());
		}
		workTime.setValue("" + addTime);
		if (enableDeadline) {
			taskData.getRoot().getAttribute(BugzillaAttribute.DEADLINE.getKey()).setValue("" + deadline);
		}

		taskData.getRoot().getAttribute(BugzillaAttribute.NEW_COMMENT.getKey()).setValue(
				"New Estimate: " + estimatedTime + "\nNew Remaining: " + remainingTime + "\nAdd: " + addTime);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(taskData.getRoot().getAttribute(BugzillaAttribute.ESTIMATED_TIME.getKey()));
		changed.add(taskData.getRoot().getAttribute(BugzillaAttribute.REMAINING_TIME.getKey()));
		changed.add(taskData.getRoot().getAttribute(BugzillaAttribute.WORK_TIME.getKey()));
		if (enableDeadline) {
			changed.add(taskData.getRoot().getAttribute(BugzillaAttribute.DEADLINE.getKey()));
		}
		changed.add(taskData.getRoot().getAttribute(BugzillaAttribute.NEW_COMMENT.getKey()));

		submit(task, taskData, changed);

		synchAndAssertState(tasks, SynchronizationState.SYNCHRONIZED);
		model = createModel(task);
		taskData = model.getTaskData();

		assertEquals(estimatedTime, Float.parseFloat(taskData.getRoot().getAttribute(
				BugzillaAttribute.ESTIMATED_TIME.getKey()).getValue()));
		assertEquals(remainingTime, Float.parseFloat(taskData.getRoot().getAttribute(
				BugzillaAttribute.REMAINING_TIME.getKey()).getValue()));
		assertEquals(actualTime + addTime, Float.parseFloat(taskData.getRoot().getAttribute(
				BugzillaAttribute.ACTUAL_TIME.getKey()).getValue()));
		if (enableDeadline) {
			assertEquals(deadline, taskData.getRoot().getAttribute(BugzillaAttribute.DEADLINE.getKey()).getValue());
		}

	}

	private String generateNewDay() {
		int year = 2006;
		int month = (int) (Math.random() * 12 + 1);
		int day = (int) (Math.random() * 28 + 1);
		return "" + year + "-" + ((month <= 9) ? "0" : "") + month + "-" + ((day <= 9) ? "0" : "") + day;
	}

	public void testSynchChangedReports() throws Exception {

		init222();
		String taskID4 = "4";
		ITask task4 = generateLocalTaskAndDownload(taskID4);
		assertNotNull(task4);
		TaskDataModel model4 = createModel(task4);
		TaskData taskData4 = model4.getTaskData();
		assertNotNull(taskData4);
		assertEquals(taskID4, taskData4.getTaskId());
		assertEquals(SynchronizationState.SYNCHRONIZED, task4.getSynchronizationState());

		String taskID5 = "5";
		ITask task5 = generateLocalTaskAndDownload(taskID5);
		assertNotNull(task5);
		TaskDataModel model5 = createModel(task5);
		TaskData taskData5 = model5.getTaskData();
		assertNotNull(taskData5);
		assertEquals(taskID5, taskData5.getTaskId());
		assertEquals(SynchronizationState.SYNCHRONIZED, task5.getSynchronizationState());

		Set<ITask> tasks = new HashSet<ITask>();
		tasks.add(task4);
		tasks.add(task5);

		// Precondition for test passing is that task5's modification data is
		// AFTER
		// task4's
		DateFormat timeDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date lastModTime4 = null;
		Date lastModTime5 = null;
		String mostRecentTimeStamp4 = taskData4.getRoot().getAttribute(BugzillaAttribute.DELTA_TS.getKey()).getValue();
		String mostRecentTimeStamp = taskData5.getRoot().getAttribute(BugzillaAttribute.DELTA_TS.getKey()).getValue();
		lastModTime4 = timeDateFormat.parse(mostRecentTimeStamp4);
		lastModTime5 = timeDateFormat.parse(mostRecentTimeStamp);
		assertTrue("Precondition not mached", lastModTime5.after(lastModTime4));

		repository.setSynchronizationTimeStamp(mostRecentTimeStamp);

		SynchronizationSession event = new SynchronizationSession();
		event.setTasks(tasks);
		event.setNeedsPerformQueries(true);
		event.setTaskRepository(repository);
		event.setFullSynchronization(true);
		connector.preSynchronization(event, null);
		assertTrue(event.needsPerformQueries());
		// Always last known changed returned
		assertFalse(event.getStaleTasks().contains(task4));
		assertTrue(event.getStaleTasks().contains(task5));

		String priority4 = null;
		if (task4.getPriority().equals("P1")) {
			priority4 = "P2";
			taskData4.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()).setValue(priority4);
		} else {
			priority4 = "P1";
			taskData4.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()).setValue(priority4);
		}

		String priority5 = null;
		if (task5.getPriority().equals("P1")) {
			priority5 = "P2";
			taskData5.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()).setValue(priority5);
		} else {
			priority5 = "P1";
			taskData5.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()).setValue(priority5);
		}
		Set<TaskAttribute> changed4 = new HashSet<TaskAttribute>();
		Set<TaskAttribute> changed5 = new HashSet<TaskAttribute>();

		changed4.add(taskData4.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()));
		changed5.add(taskData5.getRoot().getAttribute(BugzillaAttribute.PRIORITY.getKey()));

		submit(task4, taskData4, changed4);
		submit(task5, taskData5, changed5);

		event = new SynchronizationSession();
		event.setTasks(tasks);
		event.setNeedsPerformQueries(true);
		event.setTaskRepository(repository);
		event.setFullSynchronization(true);
		connector.preSynchronization(event, null);
		assertTrue(event.getStaleTasks().contains(task4));
		assertTrue(event.getStaleTasks().contains(task5));

		TasksUiInternal.synchronizeTasks(connector, tasks, true, null);

		for (ITask task : tasks) {
			if (task.getTaskId() == "4") {
				assertEquals(priority4, taskData4.getRoot()
						.getAttribute(BugzillaAttribute.PRIORITY.getKey())
						.getValue());
			}
			if (task.getTaskId() == "5") {
				assertEquals(priority5, taskData5.getRoot()
						.getAttribute(BugzillaAttribute.PRIORITY.getKey())
						.getValue());
			}
		}
	}

	public void testContextAttachFailure() throws Exception {
		init218();
		ITask task = this.generateLocalTaskAndDownload("3");
		assertNotNull(task);
		TaskDataModel model = createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
//		assertNotNull(TasksUiPlugin.getTaskDataStorageManager().getNewTaskData(task.getRepositoryUrl(),
//				task.getTaskId()));
		TasksUi.getTaskActivityManager().activateTask(task);
		File sourceContextFile = ContextCorePlugin.getContextStore().getFileForContext(task.getHandleIdentifier());
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
		sourceContextFile.createNewFile();
		sourceContextFile.deleteOnExit();
		repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials("wrong", "wrong"), false);
		try {
			FileTaskAttachmentSource attachment = new FileTaskAttachmentSource(sourceContextFile);
			attachment.setContentType(FileTaskAttachmentSource.APPLICATION_OCTET_STREAM);

			attachment.setDescription(AttachmentUtil.CONTEXT_DESCRIPTION);
			attachment.setName("mylyn-context.zip");

			TaskAttribute attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
			TaskAttachmentMapper attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

			/* Initialize a local attachment */
			attachmentMapper.setDescription("Test attachment " + new Date());
			attachmentMapper.setContentType(AttachmentUtil.CONTEXT_DESCRIPTION);
			attachmentMapper.setPatch(false);
			attachmentMapper.setComment("Context attachment failure Test");
			attachmentMapper.applyTo(attrAttachment);

			connector.getTaskAttachmentHandler().postContent(repository, task, attachment,
					attachmentMapper.getComment(), attrAttachment, new NullProgressMonitor());
		} catch (CoreException e) {
			assertEquals(
					"Unable to login to http://mylyn.eclipse.org/bugs218.\n\ninvalid username or password \n\nPlease validate credentials via Task Repositories view.",
					e.getMessage());
			assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
			return;
		}
		fail("Should have failed due to invalid userid and password.");
	}

}
