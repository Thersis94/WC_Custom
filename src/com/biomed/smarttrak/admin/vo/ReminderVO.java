package com.biomed.smarttrak.admin.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DealVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO container information related to a single reminder for a CRM customer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

public class ReminderVO {
	
	private String reminderName;
	private String reminderText;
	private String reminderDate;
	private UserDataVO reminderAuthor;
	private int reminderDateId;
	private String reminderCompleteFlg;
	private int reminderCompleteId;
	
	public enum ReminderField {
		NAME("REMINDER_1", 12),
		TEXT("REMINDER_2", 13),
		DATE("REMINDER_3", 14),
		COMPLETE("REMINDER_4", 15),
		AUTHOR("REMINDER_5", 17);
		
		private String questionCd;
		private int mapId;
		ReminderField(String dbField, int mapId) {
			this.questionCd = dbField;
			this.mapId = mapId;
		}
		
		public String getQuestionCd() {
			return questionCd;
		}
		
		public int getMapId() {
			return mapId;
		}
		
		public static ReminderField getForDb(int fieldName) {
			switch (fieldName) {
			case 12: return ReminderField.NAME;
			case 13: return ReminderField.TEXT;
			case 14: return ReminderField.DATE;
			case 15: return ReminderField.COMPLETE;
			case 17: return ReminderField.AUTHOR;
			default: return null;
			}
		}
	}
	
	public ReminderVO() {
		reminderAuthor = new UserDataVO();
	}
	
	public ReminderVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		reminderName = req.getParameter("reminderName");
		reminderText = req.getParameter("reminderText");
		reminderDate = req.getParameter("reminderDate");
		reminderDateId = Convert.formatInteger(req.getParameter("reminderDateId"));
		reminderCompleteFlg = req.getParameter("reminderCompleteFlg");
		reminderCompleteId = Convert.formatInteger(req.getParameter("reminderCompleteId"));
		reminderAuthor = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
	}
	
	public String getReminderName() {
		return reminderName;
	}
	public void setReminderName(String reminderName) {
		this.reminderName = reminderName;
	}
	public String getReminderText() {
		return reminderText;
	}
	public void setReminderText(String reminderText) {
		this.reminderText = reminderText;
	}
	public UserDataVO getReminderAuthor() {
		return reminderAuthor;
	}

	public void setReminderAuthor(UserDataVO reminderAuthor) {
		this.reminderAuthor = reminderAuthor;
	}

	public String getReminderDate() {
		return reminderDate;
	}
	public void setReminderDate(String reminderDate) {
		this.reminderDate = reminderDate;
	}
	public int getReminderDateId() {
		return reminderDateId;
	}
	public void setReminderDateId(int dateId) {
		this.reminderDateId = dateId;
	}
	public String getReminderCompleteFlg() {
		if (StringUtil.isEmpty(reminderCompleteFlg)) reminderCompleteFlg = "0";
		return reminderCompleteFlg;
	}
	public void setReminderCompleteFlg(String reminderCompleteFlg) {
		this.reminderCompleteFlg = reminderCompleteFlg;
	}
	public int getReminderCompleteId() {
		return reminderCompleteId;
	}
	public void setReminderCompleteId(int completeId) {
		this.reminderCompleteId = completeId;
	}
	
	
	/**
	 * Set the field in the vo based on the supplied result set row
	 * @param fieldId
	 * @param value
	 * @param valueId
	 * @throws SQLException 
	 */
	public void setField(ResultSet rs) throws SQLException {
		ReminderField field = ReminderField.getForDb(rs.getInt("QUESTION_MAP_ID"));
		if (field == null) return;
		
		String value = rs.getString("RESPONSE_TXT");
		switch (field) {
		case NAME:
			reminderName = StringUtil.checkVal(value);
			break;
		case TEXT:
			reminderText = StringUtil.checkVal(value);
			break;
		case DATE:
			reminderDate = StringUtil.checkVal(value);
			reminderDateId = rs.getInt("CUSTOMER_RESPONSE_ID");
			break;
		case COMPLETE:
			reminderCompleteFlg = StringUtil.checkVal(value);
			reminderCompleteId = rs.getInt("CUSTOMER_RESPONSE_ID");
			break;
		case AUTHOR:
			reminderAuthor = new UserDataVO();
			reminderAuthor.setProfileId(value);
			break;
		}
	}

	
	/**
	 * Create a map of question id/response text items out of fields on the
	 * customer vo that need to be placed in the customer response table
	 * @param customer
	 * @return
	 */
	public Map<Integer, String> makeValueMap() {
		Map<Integer, String> values = new HashMap<>();
		values.put(ReminderField.COMPLETE.getMapId(), getReminderCompleteFlg());
		values.put(ReminderField.DATE.getMapId(), reminderDate);
		values.put(ReminderField.NAME.getMapId(), reminderName);
		values.put(ReminderField.TEXT.getMapId(), reminderText);
		values.put(ReminderField.AUTHOR.getMapId(), reminderAuthor.getProfileId());
		return values;
	}
	
}
