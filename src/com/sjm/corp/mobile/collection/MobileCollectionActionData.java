package com.sjm.corp.mobile.collection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

import com.sjm.corp.mobile.collection.MobileDataVO;

/****************************************************************************
 * <b>Title</b>: MobileCollectionData.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Handles the retrieving of data for the Mobile Collection Data Tool.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 5, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MobileCollectionActionData extends SBActionAdapter{
	private String practiceTable;
	private String goalsTable;
	private String templateTable;
	private String themeTable;

	public MobileCollectionActionData() {
		super();
	}

	public MobileCollectionActionData(ActionInitVO arg0) {
		super(arg0);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req){
		log.debug("Attempting to retrieve");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req){
		practiceTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_practice";
		goalsTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_goals";
		templateTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_template_practice";	
		themeTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_theme";	
		List<MobileDataVO> vo = new ArrayList<MobileDataVO>();
		StringBuffer sql = new StringBuffer();
		sql.append("select action_id, name, location from ").append(practiceTable).append(" where ");
		sql.append("action_id = ? order by date_visited desc");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setMaxRows(Integer.parseInt(req.getParameter("numResults")));
			ps.setString(1, req.getParameter("mobileDataId").substring(0, req.getParameter("mobileDataId").indexOf('|')));
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				MobileDataVO mcVO = new MobileDataVO();
				mcVO.setPracticeName(rs.getString(2));
				mcVO.setLocation(rs.getString(3));
				getPracticeData(req, mcVO);
				getTemplateData(req,mcVO);
				getGoalData(req, mcVO);
				vo.add(mcVO);
			}
		} catch(Exception e) {
			log.error("Error grabbing data, ", e);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		log.info("Finished Retrieving Data");
		req.setAttribute(Constants.REDIRECT_DATATOOL, Boolean.TRUE);
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		mod.setActionData(vo);
        this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
	}
	/**
	 * Gets the goal data from the goal table
	 * @param req
	 * @param vo
	 */
	public void getGoalData(SMTServletRequest req, MobileDataVO vo) {
		StringBuffer sql = new StringBuffer();
		sql.append("select new_practice, rebrand_practice, consolidate, overall_patients, ");
		sql.append("interventional, hcp_patients from ").append(goalsTable).append(" where goal_id = ?");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getGoalId());
			
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				vo.setNewPractice(rs.getBoolean(1));
				vo.setRebrandPractice(rs.getBoolean(2));
				vo.setConsolidatePractice(rs.getBoolean(3));
				vo.setOverallPatients(rs.getBoolean(4));
				vo.setInterventionalPatients(rs.getBoolean(5));
				vo.setHcpPatients(rs.getBoolean(6));
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	/**
	 * Gets the template data from the template table
	 * @param req
	 * @param vo
	 */
	public void getTemplateData(SMTServletRequest req, MobileDataVO vo) {
		StringBuffer sql = new StringBuffer();
		
		/******************************************
		 * select theme_name,
		 * 		case when theme_id = template_1 then 1 else 0 end +
		 * 		case when theme_id = template_2 then 2 else 0 end +
		 * 		case when theme_id = template_3 then 3 else 0 end as matchScores
		 * from templateTable, themeTable
		 * where theme_id in (template_1, template_2, template_3) and template_id = ?
		 * order by matchScores
		 * 
		 * This statement grabs the theme names from the theme table for the themes the
		 * user had selected, and then orders them in the same way the user ordered them
		 * the matchScore field manages the ordering bit of this. If the theme_id is the same
		 * as what's in template_1, it gets a score of 2^0, and so on for the other two fields
		 * it then orders them in ascending order. Will break a user selects a theme multiple times.
		 * We do enforce that the user cannot select a theme multiple time in the app, however.
		 */
		
		sql.append("select theme_name, case when theme_id = template_1 then 1 else 0 end + ");
		sql.append("case when theme_id = template_2 then 2 else 0 end + case when theme_id = template_3 ");
		sql.append("then 3 else 0 end as matchScores from ").append(templateTable).append(" , ");
		sql.append(themeTable).append(" where theme_id in (template_1, template_2, template_3) ");
		sql.append(" and template_id = ? order by matchScores");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getTemplateId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				vo.getTemplateNames().add(rs.getString(1));
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	/**
	 * Gets the practice data from the practice table
	 * @param req
	 * @param vo
	 */
	public void getPracticeData(SMTServletRequest req, MobileDataVO vo){
		StringBuffer sql = new StringBuffer();
		sql.append("select primary_contact_name, primary_contact_email, ");
		sql.append("primary_contact_phone, primary_contact_title, alt_contact_name, ");
		sql.append("alt_contact_email, alt_contact_phone, alt_contact_title, ready_to_move, ");
		sql.append("wanting_conference_call, wanting_visit, comment, goal_id, template_id, date_visited, ");
		sql.append("review_time, office_name from ").append(practiceTable).append(" where name like ? and location like ?");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getPracticeName());
			ps.setString(2, vo.getLocation());
			ResultSet rs = ps.executeQuery();	
			if(rs.next()){
				vo.setPrimaryName((String) rs.getObject(1));
				vo.setPrimaryEmail((String) rs.getObject(2));
				vo.setPrimaryPhone((String) rs.getObject(3));
				vo.setPrimaryTitle((String) rs.getObject(4));
				vo.setAltName((String) rs.getObject(5));
				vo.setAltEmail((String) rs.getObject(6));
				vo.setAltPhone((String) rs.getObject(7));
				vo.setAltTitle((String) rs.getObject(8));
				vo.setMoveForward((Boolean)rs.getObject(9));
				vo.setConferenceCall((Boolean) rs.getObject(10));
				vo.setWantVisit((Boolean) rs.getObject(11));
				vo.setAdditionalComments((String) rs.getObject(12));
				vo.setGoalId((String)rs.getObject(13));
				vo.setTemplateId((String)rs.getObject(14));
				vo.setDate((String)rs.getObject(15));
				vo.setReviewTime(rs.getBoolean(16));
				vo.setOfficeName((String)rs.getString(17));
			}
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
}
