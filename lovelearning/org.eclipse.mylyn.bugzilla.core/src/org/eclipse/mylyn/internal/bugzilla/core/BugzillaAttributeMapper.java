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

package org.eclipse.mylyn.internal.bugzilla.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;

/**
 * @author Rob Elves
 * @author Frank Becker
 */
public class BugzillaAttributeMapper extends TaskAttributeMapper {

	private final String dateFormat_1 = "yyyy-MM-dd HH:mm:ss"; //$NON-NLS-1$

	private final String dateFormat_2 = "yyyy-MM-dd HH:mm"; //$NON-NLS-1$

	private final String dateFormat_3 = "yyyy-MM-dd"; //$NON-NLS-1$

	private final String dateFormat_1_TimeZone = "yyyy-MM-dd HH:mm:ss zzz"; //$NON-NLS-1$

	private final String dateFormat_2_TimeZone = "yyyy-MM-dd HH:mm zzz"; //$NON-NLS-1$

	private final String dateFormat_3_TimeZone = "yyyy-MM-dd zzz"; //$NON-NLS-1$

	public BugzillaAttributeMapper(TaskRepository taskRepository) {
		super(taskRepository);
	}

	@Override
	public Date getDateValue(TaskAttribute attribute) {
		if (attribute == null) {
			return null;
		}
		String dateString = attribute.getValue();
		String id = attribute.getId();
		Date parsedDate = getDate(id, dateString);
		if (parsedDate == null) {
			parsedDate = super.getDateValue(attribute);
		}
		return parsedDate;
	}

	@Override
	public boolean getBooleanValue(TaskAttribute attribute) {
		if (attribute.getValue().equals("1")) { //$NON-NLS-1$
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setBooleanValue(TaskAttribute attribute, Boolean value) {
		if (value == null) {
			attribute.setValue("0"); //$NON-NLS-1$
		} else if (value) {
			attribute.setValue("1"); //$NON-NLS-1$
		} else {
			attribute.setValue("0"); //$NON-NLS-1$
		}
	}

	/**
	 * Note: Date formatters constructed within method for thread safety
	 */
	protected Date getDate(String attributeId, String dateString) {
		Date parsedDate = null;

		/**
		 * Bugzilla < 2.22 uses "yyyy-MM-dd HH:mm" but later versions use "yyyy-MM-dd HH:mm:ss" Using lowest common
		 * denominator "yyyy-MM-dd HH:mm"
		 */

		RepositoryConfiguration repositoryConfiguration;
		BugzillaVersion bugzillaVersion = null;
		repositoryConfiguration = BugzillaCorePlugin.getRepositoryConfiguration(getTaskRepository().getUrl());
		if (repositoryConfiguration != null) {
			bugzillaVersion = repositoryConfiguration.getInstallVersion();
		} else {
			bugzillaVersion = BugzillaVersion.MIN_VERSION;
		}

		SimpleDateFormat comment_creation_ts_Format;
		SimpleDateFormat attachment_creation_ts_Format;

		SimpleDateFormat comment_creation_ts_Format_Timezone;
		SimpleDateFormat attachment_creation_ts_Format_Timezone;

		try {
			if (attributeId.equals(BugzillaAttribute.DELTA_TS.getKey())) {
				parsedDate = new SimpleDateFormat(dateFormat_1_TimeZone).parse(dateString);
			} else if (attributeId.equals(BugzillaAttribute.CREATION_TS.getKey())) {
				parsedDate = new SimpleDateFormat(dateFormat_2_TimeZone).parse(dateString);
			} else if (attributeId.equals(BugzillaAttribute.BUG_WHEN.getKey())) {
				if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
					comment_creation_ts_Format_Timezone = new SimpleDateFormat(dateFormat_2_TimeZone);
				} else {
					comment_creation_ts_Format_Timezone = new SimpleDateFormat(dateFormat_1_TimeZone);
				}
				parsedDate = comment_creation_ts_Format_Timezone.parse(dateString);
			} else if (attributeId.equals(BugzillaAttribute.DATE.getKey())) {
				if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
					attachment_creation_ts_Format_Timezone = new SimpleDateFormat(dateFormat_2_TimeZone);
				} else {
					attachment_creation_ts_Format_Timezone = new SimpleDateFormat(dateFormat_1_TimeZone);
				}
				parsedDate = attachment_creation_ts_Format_Timezone.parse(dateString);
			} else if (attributeId.equals(BugzillaAttribute.DEADLINE.getKey())) {
				parsedDate = new SimpleDateFormat(dateFormat_3_TimeZone).parse(dateString);
			} else if (attributeId.startsWith(BugzillaCustomField.CUSTOM_FIELD_PREFIX)) {
				parsedDate = new SimpleDateFormat(dateFormat_1_TimeZone).parse(dateString);
			}
		} catch (ParseException e) {
			try {
				if (attributeId.equals(BugzillaAttribute.DELTA_TS.getKey())) {
					parsedDate = new SimpleDateFormat(dateFormat_1).parse(dateString);
				} else if (attributeId.equals(BugzillaAttribute.CREATION_TS.getKey())) {
					parsedDate = new SimpleDateFormat(dateFormat_2).parse(dateString);
				} else if (attributeId.equals(BugzillaAttribute.BUG_WHEN.getKey())) {
					if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
						comment_creation_ts_Format = new SimpleDateFormat(dateFormat_2);
					} else {
						comment_creation_ts_Format = new SimpleDateFormat(dateFormat_1);
					}
					parsedDate = comment_creation_ts_Format.parse(dateString);
				} else if (attributeId.equals(BugzillaAttribute.DATE.getKey())) {
					if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
						attachment_creation_ts_Format = new SimpleDateFormat(dateFormat_2);
					} else {
						attachment_creation_ts_Format = new SimpleDateFormat(dateFormat_1);
					}
					parsedDate = attachment_creation_ts_Format.parse(dateString);
				} else if (attributeId.equals(BugzillaAttribute.DEADLINE.getKey())) {
					parsedDate = new SimpleDateFormat(dateFormat_3).parse(dateString);
				} else if (attributeId.startsWith(BugzillaCustomField.CUSTOM_FIELD_PREFIX)) {
					parsedDate = new SimpleDateFormat(dateFormat_1).parse(dateString);
				}
			} catch (ParseException e1) {

				try {
					// Fall back to legacy formats
					String delta_ts_format = dateFormat_1;
					String creation_ts_format = dateFormat_2;
					String deadline_format = dateFormat_3;
					String customAttribute_format = dateFormat_1;
					String comment_creation_ts_format = dateFormat_2;
					String attachment_creation_ts_format = dateFormat_2;
					if (attributeId.equals(BugzillaAttribute.DELTA_TS.getKey())) {
						parsedDate = new SimpleDateFormat(delta_ts_format).parse(dateString);
					} else if (attributeId.equals(BugzillaAttribute.CREATION_TS.getKey())) {
						parsedDate = new SimpleDateFormat(creation_ts_format).parse(dateString);
					} else if (attributeId.equals(BugzillaAttribute.BUG_WHEN.getKey())) {
						parsedDate = new SimpleDateFormat(comment_creation_ts_format).parse(dateString);
					} else if (attributeId.equals(BugzillaAttribute.DATE.getKey())) {
						parsedDate = new SimpleDateFormat(attachment_creation_ts_format).parse(dateString);
					} else if (attributeId.equals(BugzillaAttribute.DEADLINE.getKey())) {
						parsedDate = new SimpleDateFormat(deadline_format).parse(dateString);
					} else if (attributeId.startsWith(BugzillaCustomField.CUSTOM_FIELD_PREFIX)) {
						parsedDate = new SimpleDateFormat(customAttribute_format).parse(dateString);
					}
				} catch (ParseException e2) {
				}
			}
		} catch (NumberFormatException e) {
		}
		return parsedDate;
	}

	@Override
	public void setDateValue(TaskAttribute attribute, Date date) {
		if (date != null) {

			RepositoryConfiguration repositoryConfiguration;
			BugzillaVersion bugzillaVersion = null;
			repositoryConfiguration = BugzillaCorePlugin.getRepositoryConfiguration(getTaskRepository().getUrl());
			if (repositoryConfiguration != null) {
				bugzillaVersion = repositoryConfiguration.getInstallVersion();
			} else {
				bugzillaVersion = BugzillaVersion.MIN_VERSION;
			}

			SimpleDateFormat comment_creation_ts_Format;
			SimpleDateFormat attachment_creation_ts_Format;

			String dateString = null;
			String attributeId = attribute.getId();

			if (attributeId.equals(BugzillaAttribute.DELTA_TS.getKey())) {
				dateString = new SimpleDateFormat(dateFormat_1).format(date);
			} else if (attributeId.equals(BugzillaAttribute.CREATION_TS.getKey())) {
				dateString = new SimpleDateFormat(dateFormat_2).format(date);
			} else if (attributeId.equals(BugzillaAttribute.BUG_WHEN.getKey())) {
				if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
					comment_creation_ts_Format = new SimpleDateFormat(dateFormat_2);
				} else {
					comment_creation_ts_Format = new SimpleDateFormat(dateFormat_1);
				}
				dateString = comment_creation_ts_Format.format(date);
			} else if (attributeId.equals(BugzillaAttribute.DATE.getKey())) {
				if (bugzillaVersion.compareMajorMinorOnly(BugzillaVersion.BUGZILLA_2_22) < 0) {
					attachment_creation_ts_Format = new SimpleDateFormat(dateFormat_2);
				} else {
					attachment_creation_ts_Format = new SimpleDateFormat(dateFormat_1);
				}
				dateString = attachment_creation_ts_Format.format(date);
			} else if (attributeId.equals(BugzillaAttribute.DEADLINE.getKey())) {
				dateString = new SimpleDateFormat(dateFormat_3).format(date);
			} else if (attributeId.startsWith(BugzillaCustomField.CUSTOM_FIELD_PREFIX)) {
				dateString = new SimpleDateFormat(dateFormat_1).format(date);
			}

			if (dateString == null) {
				super.setDateValue(attribute, date);
			} else {
				attribute.setValue(dateString);
			}

		} else {
			attribute.clearValues();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public String mapToRepositoryKey(TaskAttribute parent, String key) {
		/*if (key.equals(TaskAttribute.NEW_CC)) {
			return BugzillaReportElement.NEWCC.getKey();
		} else*/if (key.equals(TaskAttribute.COMMENT_DATE)) {
			return BugzillaAttribute.BUG_WHEN.getKey();
		} else if (key.equals(TaskAttribute.COMMENT_AUTHOR)) {
			return BugzillaAttribute.WHO.getKey();
		} else if (key.equals(TaskAttribute.COMMENT_AUTHOR_NAME)) {
			return BugzillaAttribute.WHO_NAME.getKey();
		} else if (key.equals(TaskAttribute.USER_CC)) {
			return BugzillaAttribute.CC.getKey();
		} else if (key.equals(TaskAttribute.COMMENT_TEXT)) {
			return BugzillaAttribute.THETEXT.getKey();
		} else if (key.equals(TaskAttribute.DATE_CREATION)) {
			return BugzillaAttribute.CREATION_TS.getKey();
		} else if (key.equals(TaskAttribute.DESCRIPTION)) {
			return BugzillaAttribute.LONG_DESC.getKey();
		} else if (key.equals(TaskAttribute.ATTACHMENT_ID)) {
			return BugzillaAttribute.ATTACHID.getKey();
		} else if (key.equals(TaskAttribute.ATTACHMENT_DESCRIPTION)) {
			return BugzillaAttribute.DESC.getKey();
		} else if (key.equals(TaskAttribute.ATTACHMENT_CONTENT_TYPE)) {
			return BugzillaAttribute.CTYPE.getKey();
			//return BugzillaReportElement.TYPE.getKey();*/
		} else if (key.equals(TaskAttribute.USER_ASSIGNED)) {
			return BugzillaAttribute.ASSIGNED_TO.getKey();
		} else if (key.equals(TaskAttribute.USER_ASSIGNED_NAME)) {
			return BugzillaAttribute.ASSIGNED_TO_NAME.getKey();
		} else if (key.equals(TaskAttribute.RESOLUTION)) {
			return BugzillaAttribute.RESOLUTION.getKey();
		} else if (key.equals(TaskAttribute.STATUS)) {
			return BugzillaAttribute.BUG_STATUS.getKey();
		} else if (key.equals(TaskAttribute.DATE_MODIFICATION)) {
			return BugzillaAttribute.DELTA_TS.getKey();
		} else if (key.equals(TaskAttribute.USER_REPORTER)) {
			return BugzillaAttribute.REPORTER.getKey();
		} else if (key.equals(TaskAttribute.USER_REPORTER_NAME)) {
			return BugzillaAttribute.REPORTER_NAME.getKey();
		} else if (key.equals(TaskAttribute.SUMMARY)) {
			return BugzillaAttribute.SHORT_DESC.getKey();
		} else if (key.equals(TaskAttribute.PRODUCT)) {
			return BugzillaAttribute.PRODUCT.getKey();
		} else if (key.equals(TaskAttribute.KEYWORDS)) {
			return BugzillaAttribute.KEYWORDS.getKey();
		} else if (key.equals(TaskAttribute.ATTACHMENT_DATE)) {
			return BugzillaAttribute.DATE.getKey();
		} else if (key.equals(TaskAttribute.ATTACHMENT_SIZE)) {
			return BugzillaAttribute.SIZE.getKey();
		} else if (key.equals(TaskAttribute.ADD_SELF_CC)) {
			return BugzillaAttribute.ADDSELFCC.getKey();
		} else if (key.equals(TaskAttribute.PRIORITY)) {
			return BugzillaAttribute.PRIORITY.getKey();
		} else if (key.equals(TaskAttribute.COMMENT_NEW)) {
			return BugzillaAttribute.NEW_COMMENT.getKey();
		} else if (key.equals(TaskAttribute.COMPONENT)) {
			return BugzillaAttribute.COMPONENT.getKey();
		} else if (key.equals(TaskAttribute.TASK_KEY)) {
			return BugzillaAttribute.BUG_ID.getKey();
		} else if (key.equals(TaskAttribute.DATE_DUE)) {
			return BugzillaAttribute.DEADLINE.getKey();
		} else if (key.equals(TaskAttribute.SEVERITY)) {
			return BugzillaAttribute.BUG_SEVERITY.getKey();
		} else if (key.equals(TaskAttribute.VERSION)) {
			return BugzillaAttribute.VERSION.getKey();
		}
		return super.mapToRepositoryKey(parent, key);
	}

	@Override
	public TaskAttribute getAssoctiatedAttribute(TaskAttribute taskAttribute) {
		String id = taskAttribute.getMetaData().getValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID);
		if (id != null) {
			// operation associated input attributes are stored on the root attribute
			if (TaskAttribute.TYPE_OPERATION.equals(taskAttribute.getMetaData().getType())) {
				return taskAttribute.getTaskData().getRoot().getMappedAttribute(id);
			}
			return taskAttribute.getMappedAttribute(id);
		}
		return null;
	}

	@Override
	public IRepositoryPerson getRepositoryPerson(TaskAttribute taskAttribute) {

		IRepositoryPerson person = super.getRepositoryPerson(taskAttribute);
		if (person.getName() == null) {
			if (taskAttribute.getId().equals(BugzillaAttribute.ASSIGNED_TO.getKey())) {
				TaskAttribute attrAssigned = taskAttribute.getTaskData().getRoot().getAttribute(
						BugzillaAttribute.ASSIGNED_TO_NAME.getKey());
				if (attrAssigned != null) {
					person.setName(attrAssigned.getValue());
				}
			} else if (taskAttribute.getId().equals(BugzillaAttribute.REPORTER.getKey())) {
				TaskAttribute attrReporter = taskAttribute.getTaskData().getRoot().getAttribute(
						BugzillaAttribute.REPORTER_NAME.getKey());
				if (attrReporter != null) {
					person.setName(attrReporter.getValue());
				}
			} else if (taskAttribute.getId().equals(BugzillaAttribute.QA_CONTACT.getKey())) {
				TaskAttribute attrReporter = taskAttribute.getTaskData().getRoot().getAttribute(
						BugzillaAttribute.QA_CONTACT_NAME.getKey());
				if (attrReporter != null) {
					person.setName(attrReporter.getValue());
				}
			}
		}
		return person;
	}

	@Override
	public Map<String, String> getOptions(TaskAttribute attribute) {
		RepositoryConfiguration configuration = BugzillaCorePlugin.getRepositoryConfiguration(getTaskRepository().getRepositoryUrl());
		if (configuration != null) {
			TaskAttribute attributeProduct = attribute.getTaskData().getRoot().getMappedAttribute(
					BugzillaAttribute.PRODUCT.getKey());
			if (attributeProduct != null && attributeProduct.getValue().length() > 0) {
				List<String> options = configuration.getAttributeOptions(attributeProduct.getValue(), attribute);
				if (options.size() == 0 && attribute.getId().equals(BugzillaOperation.resolve.getInputId())) {
					options = configuration.getOptionValues(BugzillaAttribute.RESOLUTION, attributeProduct.getValue());
					// DUPLICATE and MOVED have special meanings so do not show as resolution
					// TODO: COPIED FUNCTIONALITY from RepositoryConfiguration.addOperation() refactor.
					options.remove("DUPLICATE"); //$NON-NLS-1$
					options.remove("MOVED"); //$NON-NLS-1$
				}
				Map<String, String> newOptions = new LinkedHashMap<String, String>();
				for (String option : options) {
					newOptions.put(option, option);
				}
				return newOptions;
			}
		}
		return super.getOptions(attribute);
	}

	@Override
	public boolean equals(TaskAttribute newAttribute, TaskAttribute oldAttribute) {
		if (oldAttribute.getId().startsWith(TaskAttribute.PREFIX_ATTACHMENT)) {
			TaskAttachmentMapper oldAttachment;
			oldAttachment = TaskAttachmentMapper.createFrom(oldAttribute);
			TaskAttachmentMapper newAttachment;
			newAttachment = TaskAttachmentMapper.createFrom(newAttribute);
			return newAttachment.equals(oldAttachment);
		}
		return super.equals(newAttribute, oldAttribute);
	}

	@Override
	public String getLabel(TaskAttribute taskAttribute) {
		if (taskAttribute.getId().startsWith(BugzillaCustomField.CUSTOM_FIELD_PREFIX)) {
			return super.getLabel(taskAttribute) + ":"; //$NON-NLS-1$
		} else {
			return super.getLabel(taskAttribute);
		}
	}

}
