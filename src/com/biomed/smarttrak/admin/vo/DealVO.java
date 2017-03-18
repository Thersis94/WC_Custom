package com.biomed.smarttrak.admin.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
 * <b>Description: </b> VO container information related to a single deal with a CRM customer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

public class DealVO {
	
	private String customerId;
	private UserDataVO contact;
	private UserDataVO owner;
	private String companyName;
	private String dealName;
	private String amount;
	private int amountId;
	private String openDate;
	private String estCloseDate;
	private int estCloseDateId;
	private String stage;
	private int stageId;
	
	public enum DealField {
		NAME("DEAL_1", 1),
		OWNER("DEAL_2", 2),
		AMOUNT("DEAL_3", 3),
		OPEN("DEAL_4", 4),
		CLOSE("DEAL_5", 5),
		STAGE("DEAL_6", 6),
		COMPANY("DEAL_7", 7);
		
		private String questionCd;
		private int mapId;
		DealField(String dbField, int mapId) {
			this.questionCd = dbField;
			this.mapId = mapId;
		}
		
		public String getQuestionCd() {
			return questionCd;
		}
		
		public int getMapId() {
			return mapId;
		}
		
		public static DealField getForDb(int fieldName) {
			switch (fieldName) {
			case 1: return DealField.NAME;
			case 2: return DealField.OWNER;
			case 3: return DealField.AMOUNT;
			case 4: return DealField.OPEN;
			case 5: return DealField.CLOSE;
			case 6: return DealField.STAGE;
			case 7: return DealField.COMPANY;
			default: return null;
			}
		}
	}
	
	public DealVO() {
		// Default constructor
	}
	
	public DealVO(ActionRequest req) {
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		amount = req.getParameter("amount");
		companyName = req.getParameter("companyName");
		estCloseDate = req.getParameter("estCloseDate");
		estCloseDateId = Convert.formatInteger(req.getParameter("estCloseDateId"));
		stage = req.getParameter("stage");
		stageId = Convert.formatInteger(req.getParameter("stageId"));
		dealName = req.getParameter("dealName");
		customerId = req.getParameter("customerId");
		owner = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public UserDataVO getContact() {
		return contact;
	}

	public void setContact(UserDataVO contact) {
		this.contact = contact;
	}

	public UserDataVO getOwner() {
		return owner;
	}

	public void setOwner(UserDataVO owner) {
		this.owner = owner;
	}
	
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getDealName() {
		return dealName;
	}

	public void setDealName(String dealName) {
		this.dealName = dealName;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public int getAmountId() {
		return amountId;
	}

	public void setAmountId(int amountId) {
		this.amountId = amountId;
	}

	public String getOpenDate() {
		// If no open date has been supplied assume this is a new deal
		// and the open date is today.
		if (StringUtil.isEmpty(openDate)) {
			openDate = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Convert.getCurrentTimestamp());
		}
		return openDate;
	}

	public void setOpenDate(String openDate) {
		this.openDate = openDate;
	}

	public String getEstCloseDate() {
		return estCloseDate;
	}

	public void setEstCloseDate(String estCloseDate) {
		this.estCloseDate = estCloseDate;
	}

	public int getEstCloseDateId() {
		return estCloseDateId;
	}

	public void setEstCloseDateId(int estCloseDateId) {
		this.estCloseDateId = estCloseDateId;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public int getStageId() {
		return stageId;
	}

	public void setStageId(int stageId) {
		this.stageId = stageId;
	}
	
	
	/**
	 * Set the field in the vo based on the supplied result set row
	 * @param fieldId
	 * @param value
	 * @param valueId
	 * @throws SQLException 
	 */
	public void setField(ResultSet rs) throws SQLException {
		DealField field = DealField.getForDb(rs.getInt("QUESTION_MAP_ID"));
		if (field == null) return;
		
		String value = rs.getString("RESPONSE_TXT");
		switch (field) {
		case NAME:
			dealName = StringUtil.checkVal(value);
			break;
		case OWNER:
			owner = new UserDataVO(rs);
			break;
		case COMPANY:
			companyName = StringUtil.checkVal(value);
			break;
		case AMOUNT:
			amount = StringUtil.checkVal(value);
			amountId = rs.getInt("CUSTOMER_RESPONSE_ID");
			break;
		case OPEN:
			openDate = StringUtil.checkVal(value);
			break;
		case CLOSE:
			estCloseDate = StringUtil.checkVal(value);
			estCloseDateId = rs.getInt("CUSTOMER_RESPONSE_ID");
			break;
		case STAGE:
			stage = StringUtil.checkVal(value);
			stageId = rs.getInt("CUSTOMER_RESPONSE_ID");
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
		values.put(DealField.AMOUNT.getMapId(), amount);
		values.put(DealField.CLOSE.getMapId(), estCloseDate);
		values.put(DealField.NAME.getMapId(), dealName);
		values.put(DealField.OPEN.getMapId(), getOpenDate());
		values.put(DealField.STAGE.getMapId(), stage);
		values.put(DealField.OWNER.getMapId(), owner.getProfileId());
		values.put(DealField.COMPANY.getMapId(), companyName);
		return values;
	}
}
