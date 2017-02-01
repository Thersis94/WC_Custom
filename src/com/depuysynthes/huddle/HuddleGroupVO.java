/**
 *
 */
package com.depuysynthes.huddle;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.formbuilder.vo.FBFormVO;
import com.smt.sitebuilder.admin.action.SBModuleAction;

/****************************************************************************
 * <b>Title</b>: HuddleFormVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Helper VO That holds HuddleFormGroupVOs
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 15, 2016
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class HuddleGroupVO extends SBModuleVO {

	private static final long serialVersionUID = 1L;
	private Map<String, HuddleForm> forms;
	
	public HuddleGroupVO() {
		forms = new LinkedHashMap<String, HuddleForm>();
	}
	
	public HuddleGroupVO(ResultSet rs) {
		this();
		setData(rs);
	}
	

	/**
	 * Helper method to retrieve data off the Request.
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setActionId(req.getParameter(SBModuleAction.SB_ACTION_ID));
		setActionName(req.getParameter("actionName"));
		setActionDesc(req.getParameter("actionDesc"));
		setActionGroupId(req.getParameter(SBModuleAction.SB_ACTION_GROUP_ID));
	}

	/**
	 * Helper method that loads data off ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		organizationId = db.getStringVal("ORGANIZATION_ID", rs);
		setActionName(db.getStringVal("ACTION_NM", rs));
		setActionDesc(db.getStringVal("ACTION_DESC", rs));
		actionGroupId = db.getStringVal("ACTION_GROUP_ID", rs);
		pendingSyncFlag = db.getIntVal("PENDING_SYNC_FLG", rs);
		setActionId(db.getStringVal("ACTION_ID", rs));
		super.setAttribute(SBModuleVO.ATTRIBUTE_2, db.getStringVal("attrib2_txt", rs)); //used for Specialty
	}

	//Getter
	public Map<String, HuddleForm> getFormsMap() {return forms;}
	public List<HuddleForm> getForms() {return new ArrayList<HuddleForm>(forms.values());}

	//Setter
	public void setForms(Map<String, HuddleForm> forms) {this.forms = forms;}

	/**
	 * Helper method that adds a form to the forms List.
	 * @param form
	 */
	public void addHuddleForm(HuddleForm form) {
		forms.put(form.getActionGroupId(), form);
	}

	/**
	 * **************************************************************************
	 * <b>Title</b>: HuddleGroupVO.java
	 * <p/>
	 * <b>Project</b>: WC_Custom
	 * <p/>
	 * <b>Description: </b> Helper inner class that extends the FBFormVO with
	 * group information.
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2016
	 * <p/>
	 * <b>Company:</b> Silicon Mountain Technologies
	 * <p/>
	 * 
	 * @author raptor
	 * @version 1.0
	 * @since Jan 18, 2016
	 *        <p/>
	 *        <b>Changes: </b>
	 ***************************************************************************
	 */
	public class HuddleForm extends FBFormVO {

		private static final long serialVersionUID = 1L;
		private String huddleGroupId;
		private String formGroupId;
		public HuddleForm(ResultSet rs) {
			setData(rs);
		}
		public void setData(ResultSet rs) {
			super.setData(rs);
			DBUtil db = new DBUtil();
			huddleGroupId = db.getStringVal("HUDDLE_GROUP_ID", rs);
			formGroupId = db.getStringVal("FORM_GROUP_ID", rs);
		}

		//Getters
		public String getHuddleGroupId() {return huddleGroupId;}
		public String getFormGroupId() {return formGroupId;}

		//Setters
		public void setHuddleGroupId(String huddleGroupId) {this.huddleGroupId = huddleGroupId;}
		public void setFormGroupId(String formGroupId) {this.formGroupId = formGroupId;}
	}
}