/**
 *
 */
package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.survey.SurveyFacadeAction;
import com.smt.sitebuilder.action.survey.SurveyVO;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SiteSurveyAction.java
 * <p/>
 * <b>Project</b>: WebCrescendo
 * <p/>
 * <b>Description: </b> Wrapper class that allows an ajax interface to Survey
 * for purposes of displaying a Survey to users visiting the site.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 26, 2016
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class SiteSurveyAction extends SBActionAdapter {

	/**
	 * @param actionInit
	 */
	public SiteSurveyAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 */
	public SiteSurveyAction() {
	}

	public void list(ActionRequest req) throws ActionException {

		//Get parameters off request.
		String actionId = req.getParameter(SB_ACTION_ID);
		String orgId = req.getParameter("organizationId");

		//Retrieve Site Survey Info
		SiteSurveyVO ssv = getSiteSurveyVO(actionId);

		//Retrieve Surveys
		List<SurveyVO> surveys = getSurveys(orgId);

		//Put Info on Request
		this.putModuleData(ssv, 1, true);
		req.setAttribute("surveys", surveys);
	}

	@Override
	public void delete(ActionRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String siteSurveyId = req.getParameter(SB_ACTION_ID);
		try(PreparedStatement ps = dbConn.prepareStatement(getSiteSurveyDeleteSql())) {
			ps.setString(1, siteSurveyId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		}

        sbUtil.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH), req.getParameter(SBModuleAction.SB_ACTION_ID));
	}

	public void update(ActionRequest req) throws ActionException {
		//Update SB_ACTION record
		super.update(req);

		//Determine isInsert
		boolean isInsert = Convert.formatBoolean(req.getAttribute(INSERT_TYPE));

		//Store Data in VO
		SiteSurveyVO ssv = new SiteSurveyVO(req);

		//Save Site Survey.
		saveSiteSurvey(ssv, isInsert);

		//Redirect
        sbUtil.adminRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH), ssv.getSiteSurveyId());
	}

	public void build(ActionRequest req) throws ActionException {

		//Get the Site Survey Info
		retrieve(req);

		//Retrieve info off request.
		SiteSurveyVO ssv = (SiteSurveyVO)((ModuleVO) attributes.get(Constants.MODULE_DATA)).getActionData();

		/*
		 * If User declined to Submit Form, Write a dummy record into first
		 * question. Otherwise if user submitted form, record all results.
		 */
		if(req.hasParameter("noThanks")) {
			UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			writeNoThanks(ssv, user, site.getSiteId());
		} else if(req.hasParameter("surveySubmit")) {
			actionInit.setActionId(ssv.getSurveyId());
			SurveyFacadeAction sfa = new SurveyFacadeAction(this.actionInit);
			sfa.setAttributes(attributes);
			sfa.setDBConnection(dbConn);
			sfa.build(req);
		}
	}

	/**
	 * Helper method that writes a default User Declined Value to the first
	 * questions Value.
	 * @param ssv
	 * @param user
	 * @param siteId
	 */
	private void writeNoThanks(SiteSurveyVO ssv, UserDataVO user, String siteId) {

		//Retrieve the first questionId on Survey
		String questionId = getQuestion(ssv.getSurveyId());

		//If there is a question, write a value.
		if(StringUtil.checkVal(questionId).length() > 0) {
			try(PreparedStatement ps = dbConn.prepareStatement(getResponseInsert())) {
				int i = 1;
				ps.setString(i++, new UUIDGenerator().getUUID());
				ps.setString(i++, questionId);
				ps.setString(i++, ssv.getSurveyId());
				ps.setString(i++, user.getProfileId());
				ps.setString(i++, siteId);
				ps.setString(i++, new UUIDGenerator().getUUID());
				ps.setString(i++, "Declined Survey");
				ps.setTimestamp(i++, Convert.getCurrentTimestamp());
				ps.setString(i++, ssv.getSurveyId());
				ps.executeUpdate();
			} catch (SQLException e) {
				log.error(e);
			}
		}
	}

	/**
	 * Helper method that builds Sql for Inserting a Survey Response.
	 * @return
	 */
	private String getResponseInsert() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into SURVEY_RESPONSE (SURVEY_RESPONSE_ID, ");
		sql.append("SURVEY_QUESTION_ID, ACTION_ID, PROFILE_ID, SITE_ID, ");
		sql.append("TRANSACTION_ID, VALUE_TXT, CREATE_DT, ACTION_GROUP_ID) ");
		sql.append("values (?,?,?,?,?,?,?,?,?)");
		return sql.toString();
	}

	/**
	 * Helper method that retrieves the first question on a form.
	 * @param surveyId
	 * @return
	 */
	private String getQuestion(String surveyId) {
		String qid = null;
		try(PreparedStatement ps = dbConn.prepareStatement(getQuestionSql())) {
			ps.setString(1, surveyId);

			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				qid = rs.getString("SURVEY_QUESTION_ID");
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return qid;
	}

	/**
	 * Helper method that builds sql for retrieving first question on a form.
	 * @return
	 */
	private String getQuestionSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select top 1 SURVEY_QUESTION_ID from SURVEY_QUESTION ");
		sql.append("where ACTION_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method that Saves SiteSurvey Information.
	 * @param ssv
	 * @param isInsert
	 */
	public void saveSiteSurvey(SiteSurveyVO ssv, boolean isInsert) {
		String query = null;

		//Get appropriate Insert/Update Query.
		if(isInsert) {
			query = getSiteSurveyInsertSql();
		} else {
			query = getSiteSurveyUpdateSql();
		}

		log.debug("Executing" + query);
		try(PreparedStatement ps = dbConn.prepareStatement(query)) {
			int i = 1;
			ps.setString(i++, ssv.getSurveyId());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			if(isInsert)
				ps.setString(i++, ssv.getActionId());
			ps.setString(i++, ssv.getSiteSurveyId());

			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that retrieves a list of Surveys for the Site Survey.
	 * @param orgId
	 * @return
	 */
	private List<SurveyVO> getSurveys(String orgId) {
		Map<String, SurveyVO> surveys = new HashMap<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getSurveyListSql())) {
			ps.setString(1, "SURVEY");
			ps.setString(2, orgId);

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				surveys.put(rs.getString("ACTION_GROUP_ID"), new SurveyVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return new ArrayList<SurveyVO>(surveys.values());
	}

	/**
	 * Helper method that retrieves the SiteSurvey Data wrapped in a
	 * SiteSurveyVO.
	 * @param actionId
	 * @return
	 */
	private SiteSurveyVO getSiteSurveyVO(String actionId) {
		SiteSurveyVO ssv = null;

		try(PreparedStatement ps = dbConn.prepareStatement(getSiteSurveyListSql())) {
			ps.setString(1, actionId);

			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				ssv = new SiteSurveyVO(rs);
			}

		} catch (SQLException e) {
			log.error(e);
		}

		return ssv;
	}

	public void retrieve(ActionRequest req) throws ActionException {
		//Get parameters off request.
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (user == null) return;
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String profileId = user.getProfileId();
		String actionId = actionInit.getActionId();

		//Query for data.
		SiteSurveyVO ssv = null;
		try(PreparedStatement ps = dbConn.prepareStatement(getSurveyLookupQuery())) {
			log.debug(profileId + " | " + actionId);
			ps.setString(1, profileId);
			ps.setString(2, actionId);
			ResultSet rs = ps.executeQuery();

			/*
			 * Query will return both pending and approved survey records in
			 * order of approved -> pending.
			 */
			while(rs.next()) {
				ssv = new SiteSurveyVO(rs);
				//If not in preview Mode and survey not pending Approval break.
				if(!page.isPreviewMode() && rs.getInt("PENDING_SYNC_FLG") == 0) {
					break;
				}
			}
		} catch (SQLException e) {
			log.error(e);
		}

		if(req.hasParameter("getSurvey")) {
			actionInit.setActionId(ssv.getSurveyId());
			SurveyFacadeAction ssfa = new SurveyFacadeAction(actionInit);
			ssfa.setAttributes(attributes);
			ssfa.setDBConnection(dbConn);
			ssfa.retrieve(req);
		} else {
			//Place data on Request.
			this.putModuleData(ssv);
		}
	}

	/**
	 * Helper method that builds Delete Query for removing Site Survey Records.
	 * @return
	 */
	private String getSiteSurveyDeleteSql() {
		StringBuilder sql = new StringBuilder(60);
		sql.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_HUDDLE_SURVEY where SITE_SURVEY_ID = ?");
		return sql.toString(); 
	}

	/**
	 * Helper method that builds List Query to get Survey Records.
	 * @return
	 */
	private String getSurveyListSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from SB_ACTION a where MODULE_TYPE_ID = ? and ");
		sql.append("ORGANIZATION_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method that returns the Site Survey List Query.
	 * @return
	 */
	private String getSiteSurveyListSql() {
		StringBuilder sql = new StringBuilder(50);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_HUDDLE_SURVEY a ");
		sql.append("inner join SB_ACTION b on b.ACTION_ID = a.SITE_SURVEY_ID ");
		sql.append("where SITE_SURVEY_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method that returns the SiteSurvey Update Query.
	 * @return
	 */
	private String getSiteSurveyUpdateSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_HUDDLE_SURVEY set SURVEY_ID = ?, UPDATE_DT = ? where SITE_SURVEY_ID = ?");
		return sql.toString();
	}

	/**
	 * Helper method that returns the SiteSurvey Insert Query.
	 * @return
	 */
	private String getSiteSurveyInsertSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_HUDDLE_SURVEY (SURVEY_ID, CREATE_DT, ACTION_ID, SITE_SURVEY_ID) ");
		sql.append("values (?,?,?,?)");
		return sql.toString();
	}

	/**
	 * Helper method that returns the query for looking up all relevant
	 * information about a Site Survey.
	 * @return
	 */
	private String getSurveyLookupQuery() {
		StringBuilder sql = new StringBuilder(600);
		sql.append("select a.SITE_SURVEY_ID, a.ACTION_ID, b.ACTION_ID as 'SURVEY_ID', ");
		sql.append("b.PENDING_SYNC_FLG, b.ACTION_DESC, b.ACTION_NM, c.ACTION_ID, ");
		sql.append("c.REQUIRED_FLG, d.PROFILE_ID from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_HUDDLE_SURVEY a ");
		sql.append("inner join SB_ACTION b on a.SURVEY_ID = b.ACTION_GROUP_ID ");
		sql.append("inner join SURVEY c on b.ACTION_ID = c.ACTION_ID ");
		sql.append("left outer join SURVEY_RESPONSE d on b.ACTION_ID = d.ACTION_ID ");
		sql.append("and d.PROFILE_ID = ? where a.ACTION_ID = ? order by b.PENDING_SYNC_FLG");
		log.debug(sql.toString());
		return sql.toString();
	}
}
