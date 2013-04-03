/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Eugene Kuleshov - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui.tasklist;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.bugzilla.ui.BugzillaImages;
import org.eclipse.mylyn.internal.bugzilla.ui.search.BugzillaSearchPage;
import org.eclipse.mylyn.internal.bugzilla.ui.wizard.NewBugzillaTaskWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskComment;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentModel;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.LegendElement;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

/**
 * @author Mik Kersten
 */
public class BugzillaConnectorUi extends AbstractRepositoryConnectorUi {

	private static final String regexp = "(duplicate of|bug|task)( ?#? ?)(\\d+)"; //$NON-NLS-1$

	private static final Pattern PATTERN = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);

	@Override
	public String getAccountCreationUrl(TaskRepository taskRepository) {
		return taskRepository.getRepositoryUrl() + "/createaccount.cgi"; //$NON-NLS-1$
	}

	@Override
	public String getAccountManagementUrl(TaskRepository taskRepository) {
		return taskRepository.getRepositoryUrl() + "/userprefs.cgi"; //$NON-NLS-1$
	}

	@Override
	public String getTaskHistoryUrl(TaskRepository taskRepository, ITask task) {
		return taskRepository.getRepositoryUrl() + IBugzillaConstants.URL_BUG_ACTIVITY + task.getTaskId();
	}

	@Override
	public String getReplyText(TaskRepository taskRepository, ITask task, ITaskComment taskComment, boolean includeTask) {
		if (taskComment == null) {
			return Messages.BugzillaConnectorUi__In_reply_to_comment_0_;
		} else if (includeTask) {
			return MessageFormat.format(Messages.BugzillaConnectorUi__In_reply_to_X_comment_X_, task.getTaskKey(),
					taskComment.getNumber());
		} else {
			return MessageFormat.format(Messages.BugzillaConnectorUi__In_reply_to_comment_X_, taskComment.getNumber());
		}
	}

	private static final int TASK_NUM_GROUP = 3;

	@Override
	public List<LegendElement> getLegendElements() {
		List<LegendElement> legendItems = new ArrayList<LegendElement>();
		legendItems.add(LegendElement.createTask("blocker", BugzillaImages.OVERLAY_CRITICAL)); //$NON-NLS-1$
		legendItems.add(LegendElement.createTask("critical", BugzillaImages.OVERLAY_CRITICAL)); //$NON-NLS-1$
		legendItems.add(LegendElement.createTask("major", BugzillaImages.OVERLAY_MAJOR)); //$NON-NLS-1$
		legendItems.add(LegendElement.createTask("enhancement", BugzillaImages.OVERLAY_ENHANCEMENT)); //$NON-NLS-1$
		legendItems.add(LegendElement.createTask("trivial", BugzillaImages.OVERLAY_MINOR)); //$NON-NLS-1$
		return legendItems;
	}

	@Override
	public ImageDescriptor getTaskKindOverlay(ITask task) {
		String severity = task.getAttribute(BugzillaAttribute.BUG_SEVERITY.getKey());
		if (severity != null) {
			// XXX: refactor to use configuration
			if ("blocker".equals(severity) || "critical".equals(severity)) { //$NON-NLS-1$ //$NON-NLS-2$
				return BugzillaImages.OVERLAY_CRITICAL;
			} else if ("major".equals(severity)) { //$NON-NLS-1$
				return BugzillaImages.OVERLAY_MAJOR;
			} else if ("enhancement".equals(severity)) { //$NON-NLS-1$
				return BugzillaImages.OVERLAY_ENHANCEMENT;
			} else if ("trivial".equals(severity) || "minor".equals(severity)) { //$NON-NLS-1$ //$NON-NLS-2$
				return BugzillaImages.OVERLAY_MINOR;
			} else {
				return null;
			}
		}
		return super.getTaskKindOverlay(task);
	}

	@Override
	public IHyperlink[] findHyperlinks(TaskRepository repository, String text, int index, int textOffset) {
		ArrayList<IHyperlink> hyperlinksFound = null;

		Matcher m = PATTERN.matcher(text);
		while (m.find()) {
			if (index == -1 || (index >= m.start() && index <= m.end())) {
				IHyperlink link = extractHyperlink(repository, textOffset, m);
				if (link != null) {
					if (hyperlinksFound == null) {
						hyperlinksFound = new ArrayList<IHyperlink>();
					}
					hyperlinksFound.add(link);
				}
			}
		}

		return (hyperlinksFound != null) ? hyperlinksFound.toArray(new IHyperlink[0]) : null;
	}

	@Override
	public String getTaskKindLabel(ITask repositoryTask) {
		return IBugzillaConstants.BUGZILLA_TASK_KIND;
	}

	@Override
	public ITaskRepositoryPage getSettingsPage(TaskRepository taskRepository) {
		return new BugzillaRepositorySettingsPage(taskRepository);
	}

	@Override
	public AbstractRepositoryQueryPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new BugzillaSearchPage(repository);
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository taskRepository, ITaskMapping selection) {
		return new NewBugzillaTaskWizard(taskRepository, selection);
	}

	@Override
	public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
		RepositoryQueryWizard wizard = new RepositoryQueryWizard(repository);
		if (query == null) {
			wizard.addPage(new BugzillaQueryTypeWizardPage(repository));
		} else {
			if (isCustomQuery(query)) {
				wizard.addPage(new BugzillaCustomQueryWizardPage(repository, query));
			} else {
				wizard.addPage(new BugzillaSearchPage(repository, query));
			}
		}
		return wizard;
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}

	@Override
	public String getConnectorKind() {
		return BugzillaCorePlugin.CONNECTOR_KIND;
	}

	private boolean isCustomQuery(IRepositoryQuery query2) {
		String custom = query2.getAttribute(IBugzillaConstants.ATTRIBUTE_BUGZILLA_QUERY_CUSTOM);
		return custom != null && custom.equals(Boolean.TRUE.toString());
	}

	private static IHyperlink extractHyperlink(TaskRepository repository, int regionOffset, Matcher m) {

		int start = -1;

		if (m.group().startsWith("duplicate")) { //$NON-NLS-1$
			start = m.start() + m.group().indexOf(m.group(TASK_NUM_GROUP));
		} else {
			start = m.start();
		}

		int end = m.end();

		if (end == -1) {
			end = m.group().length();
		}

		try {

			String bugId = m.group(TASK_NUM_GROUP).trim();
			start += regionOffset;
			end += regionOffset;

			IRegion sregion = new Region(start, end - start);
			return new TaskHyperlink(sregion, repository, bugId);

		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public IWizardPage getTaskAttachmentPage(TaskAttachmentModel model) {
		return new BugzillaTaskAttachmentPage(model);
	}

}
