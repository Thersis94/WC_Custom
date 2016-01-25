/**
 *
 */
package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.formbuilder.FormBuilderFacadeAction;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: HuddleFormGroupAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that wraps the FormBuilderFacadeAction with
 * a grouping system.
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
public class HuddleFormGroupAction extends SBActionAdapter {

	/**
	 * 
	 */
	public HuddleFormGroupAction() {
	}

	/**
	 * @param actionInit
	 */
	public HuddleFormGroupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void list(SMTServletRequest req) throws ActionException {

		//Get necessary information off request.
		String formGroupId = StringUtil.checkVal(req.getParameter(SBModuleAction.SB_ACTION_ID));
		String organizationId = StringUtil.checkVal(req.getParameter("organizationId"));

		/*
		 * Retrieve the HuddleGroupVO for the given formGroupId and place in
		 * AdminModuleData. 
		 */
		super.putModuleData(getHuddleGroupVO(formGroupId, organizationId, false), 1, true);
	}

	public void update(SMTServletRequest req) throws ActionException {

		//update SBAction Record.
		super.update(req);

		//Get formIds and formGroupId off request
		String [] formIds = req.getParameter("formGroupIds").trim().split(",");
		String formGroupId = StringUtil.checkVal(req.getAttribute(SBModuleAction.SB_ACTION_ID));

		/*
		 * If we have a formGroupId, remove all existing group records and
		 * re-add based off formIds that are passed.
		 */
		if(formGroupId.length() > 0) {
			//Flush All Existing Records
			flushGroup(formGroupId);

			//Add New FormGroup Records
			addForms(formGroupId, formIds);
		}
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		/*
		 * If there is a formId on the request, forward the call to
		 * FormBuilderFacadeActions build method.  Otherwise just retrieve the
		 * HuddleFormGroupActions vo.
		 */
		if(req.hasParameter("formId")) {
			getFormBuilderFacadeAction(req.getParameter("formId")).retrieve(req);
		} else {
			String formGroupId = actionInit.getActionId();
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			putModuleData(getHuddleGroupVO(formGroupId, site.getOrganizationId(), page.isPreviewMode()), 1, false);
		}
	}

	/**
	 * Proxy Build request through the FormBuilderFacadeAction Build method.
	 */
	public void build(SMTServletRequest req) throws ActionException {

		/*
		 * If there is a formId on the request, forward the call to
		 * FormBuilderFacadeActions build method.
		 */
		if(req.hasParameter("formId")) {
			getFormBuilderFacadeAction(req.getParameter("formId")).build(req);
		}
	}

	/**
	 * Helper method that manages building a FormBuilderFacadeAction instance.
	 * @param formId
	 * @return
	 */
	public FormBuilderFacadeAction getFormBuilderFacadeAction(String formId) {
		this.actionInit.setActionId(formId);
		FormBuilderFacadeAction fbfa = new FormBuilderFacadeAction(this.actionInit);
		fbfa.setDBConnection(getDBConnection());
		fbfa.setAttributes(getAttributes());
		return fbfa;
	}

	/**
	 * Helper method that adds HuddleFormGroup records for a given list of
	 * formIds.
	 * @param formGroupId
	 * @param formIds
	 */
	private void addForms(String formGroupId, String[] formIds) {
		try(PreparedStatement ps = dbConn.prepareStatement(getFormAddSql())) {
			for(int i = 0; i < formIds.length; i++) {
				int cnt = 1;
				ps.setString(cnt++, formGroupId);
				ps.setString(cnt++, formIds[i]);
				ps.setInt(cnt++, i);
				ps.setTimestamp(cnt++, Convert.getCurrentTimestamp());
				ps.setString(cnt++, new UUIDGenerator().getUUID());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that removes all HuddleFormGroup records for a given
	 * formGroupId.
	 * @param formGroupId
	 */
	private void flushGroup(String formGroupId) {
		try(PreparedStatement ps = dbConn.prepareStatement(getflushGroupSql())) {
			ps.setString(1, formGroupId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that returns all forms in system for an org along with any
	 * matching HuddleGroup records that 
	 * @param actionId
	 * @return
	 */
	private HuddleGroupVO getHuddleGroupVO(String formGroupId, String organizationId, boolean isPreview) {
		HuddleGroupVO grp = new HuddleGroupVO();
		try(PreparedStatement ps = dbConn.prepareStatement(getFormGroupSql())) {
			ps.setString(1, formGroupId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				grp.setData(rs);
			}
		} catch (SQLException e) {
			log.error(e);
		}

		getFormList(grp, organizationId, isPreview);
		return grp;
	}

	/**
	 * Helper method that retrieves the Form Group Information and all
	 * associated fields.
	 * @param grp
	 * @param organizationId
	 */
	private void getFormList(HuddleGroupVO grp, String organizationId, boolean isPreview) {
		try(PreparedStatement ps = dbConn.prepareStatement(getFormsSql())) {
			ps.setString(1, StringUtil.checkVal(grp.getActionId()));
			ps.setString(2, StringUtil.checkVal(organizationId));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				//If we are not in preview mode don't add Pending Forms.
				if(!isPreview && rs.getInt("PENDING_SYNC_FLG") == 1) {
					continue;
				} else {
					grp.addHuddleForm(grp.new HuddleForm(rs));
				}
			}
		} catch (SQLException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that returns the sql for adding new HuddleFormRecords.
	 * @return
	 */
	private String getFormAddSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("HUDDLE_FORM_GROUP (FORM_GROUP_ID, FORM_ID, ORDER_NO, ");
		sql.append("CREATE_DT, HUDDLE_GROUP_ID) values (?,?,?,?,?)");
		return sql.toString();
	}

	/**
	 * Helper method that returns the sql for Flushing all Huddle Form Records
	 * for a particular Form Group
	 * @return
	 */
	private String getflushGroupSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("HUDDLE_FORM_GROUP where FORM_GROUP_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method that retrieves the HuddleGroup Action data. 
	 * @return
	 */
	private String getFormGroupSql() {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select * from SB_ACTION a where ACTION_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method returns FormGroup List Lookup
	 * @return
	 */
	private String getFormsSql() {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select * from FB_FORM a inner join SB_ACTION c ");
		sql.append("on a.FORM_ID = c.ACTION_ID left outer join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("HUDDLE_FORM_GROUP b on c.ACTION_GROUP_ID = b.FORM_ID ");
		sql.append("and b.FORM_GROUP_ID = ? and c.ORGANIZATION_ID = ? ");
		sql.append("order by b.ORDER_NO, PENDING_SYNC_FLG ");
		return sql.toString();
	}
}