package com.sjm.corp.mobile.collection.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.sjm.corp.mobile.collection.MobileCollectionVO;
import com.sjm.corp.mobile.collection.ThemeVO;

/****************************************************************************
 * <b>Title</b>: fetchData.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Fetches keys, themes, and other data for the sjm mobile collection actions
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FetchData extends CollectionAbstractAction{
	
	private String practiceTable; //These members store the custom location of the various tables that we handle.
	private String themeTable;
	private String templateTable;

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo){
		practiceTable = req.getAttribute("custom") + "sjm_mobile_practice";
		themeTable = req.getAttribute("custom") + "sjm_mobile_theme";
		templateTable = req.getAttribute("custom") + "sjm_mobile_template_practice";
		
		boolean flag = updatePractice(req,vo);
		if(flag)
			return;
		
		UserDataVO user = (UserDataVO)req.getSession().getAttribute("userData");
		log.debug("USER ID: " + user.getProfileId());
		getLocation(req, user.getProfileId(), vo);
		
		fetchKeys(req,vo);
		fetchThemes(req,vo);
		vo.setEmailSent(false);
		super.blockBack(req);
	}
	/**
	 * Get's the location for the SJM Rep
	 * @param id
	 * @param vo
	 */
	public void getLocation(ActionRequest req, String id, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		dbConn = (SMTDBConnection) req.getAttribute("dbConn");
		sql.append("select region_nm, reg.region_id from ").append((String)req.getAttribute("custom"));
		sql.append("ans_sales_rep as rep , ").append((String)req.getAttribute("custom"));
		sql.append("ans_sales_region as reg where profile_id = ? and rep.region_id = ");
		sql.append("reg.region_id");
		
		log.debug(sql.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				vo.getRegion().setName(rs.getString(1));
				vo.getRegion().setRegionId(rs.getString(2));
				vo.getPractice().setLocation(rs.getString(1));
			}
		} catch(Exception e){
			log.error("Error fetching the region name and region id from ans_sales_region for this user ",e);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
			}
		}
		if(vo.getRegion().getName() != null && vo.getRegion().getRegionId() != null)
			getMaxSelected(req, vo);
	}
	
	/**
	 * 
	 * @param req
	 * @param vo
	 */
	public void getMaxSelected(ActionRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		dbConn = (SMTDBConnection) req.getAttribute("dbConn");
		sql.append("Select max_selected from ").append((String)req.getAttribute("custom"));
		sql.append("sjm_mobile_region where action_id = ? and name like ?");
		
		log.debug(sql.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getActionId());
			ps.setString(2, vo.getRegion().getName());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				vo.getRegion().setMaxSelected(Integer.parseInt(rs.getString(1)));
		} catch(Exception e) {
			log.error("Error fetching the max_selected from sjm_mobile_region ",e);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
			}
		}
		log.debug(vo.getRegion().getMaxSelected());
	}
	
	/**
	 * 
	 * @param req
	 * @param vo
	 * @return
	 */
	public boolean updatePractice(ActionRequest req, MobileCollectionVO vo){
		vo.getPractice().setPracticioner(req.getParameter("docName"));
		vo.getPractice().setName(vo.getPractice().getPracticioner());
		//vo.getPractice().setLocation(req.getParameter("docLoc"));
		vo.getPractice().setAdminEmail(req.getParameter("adminEmail"));
		vo.getPractice().setOfficeName(req.getParameter("practiceOffice"));
		if(req.getParameter("adminEmail") == null || req.getParameter("adminEmail").equals("")){
			req.setParameter("pageNumber", "-1");
			req.setParameter("error", "Please input an email to send the results to");
			return true;
		}
		if(req.getParameter("practiceOffice") == null || req.getParameter("practiceOffice").equals("")){
			req.setParameter("pageNumber", "-1");
			req.setParameter("error", "Please input a name for this office");
		}
		if(req.getParameter("docName") == null || req.getParameter("docName").length() == 0){
			req.setParameter("pageNumber", "-1");
			req.setParameter("error", "Please input a practitioner name");
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the keys, if they exsist
	 * @param req
	 * @param vo
	 */
	public void fetchKeys(ActionRequest req, MobileCollectionVO vo) {
		StringBuffer sql = new StringBuffer();
		sql.append("select goal_id, marketing_id, template_id, patient_id, region_id from ");
		sql.append(practiceTable).append(" where name like ? and location like ?");
		
		dbConn = (SMTDBConnection) req.getAttribute("dbConn");
		log.debug(sql.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getPractice().getPracticioner());
			ps.setString(2, vo.getPractice().getLocation());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo.getGoals().setGoalId(rs.getString(1));
				vo.getMarketing().setMarketingId(rs.getString(2));
				vo.getTemplates().setTemplateId(rs.getString(3));
				vo.getPatients().setPatientId(rs.getString(4));
				vo.getRegion().setRegionId(rs.getString(5));
			}
		} catch (Exception e){
			log.error("Error fetching the practice data",e);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	
	/**
	 * Get's the theme data for whatever location the current practice is, puts it in an List
	 * @param req
	 * @param vo
	 */
	public void fetchThemes(ActionRequest req,MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		
		/*
		 * select theme_id, thumb_loc, color_1_front, color_1_back, color_2_front
		 * 		color_2_back, theme_name, theme_pdf 
		 * 		from sjm_mobile_theme
		 * 		where location like location and theme_id not in(
		 * 			select template_1 
		 * 			from sjm_mobile_template_practice 
		 *  		group by template_1 
		 *			having count(template_1) > maxSelected
		 *		)
		 *
		 * This line of code assumes that theme_id's are unique per template per region. You can change this to get rid
		 * of that assumption by changing the sub-query to an inner join like this:
		 * 
		 * select template_1
		 * 		from sjm_mobile_template_practice as a, sjm_mobile_practice as b
		 * 		where a.template_id = b.template_id and b.location = ?
		 * 		group by template_1
		 * 		having count(template_1) > maxSelected
		 */
		
		sql.append("select theme_id, thumb_loc, color_1_front, color_1_back, color_2_front, ");
		sql.append("color_2_back, theme_name, theme_pdf from ").append(themeTable).append(" where location like ? ");
		sql.append("and theme_id not in(select template_1 from ").append(templateTable);
		sql.append(" group by template_1 having count(template_1) >= ?)");
		 
		log.debug(sql.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getPractice().getLocation());
			ps.setString(2, Integer.toString(vo.getRegion().getMaxSelected()));
			ResultSet rs = ps.executeQuery();
			vo.setThemes(new ThemeVO());
			while(rs.next()){
				vo.getThemes().getThemeId().add(rs.getString(1));
				vo.getThemes().getThumbLoc().add((String)rs.getObject(2));
				vo.getThemes().getColorOneFront().add((String)rs.getObject(3));
				vo.getThemes().getColorOneBack().add((String)rs.getObject(4));
				vo.getThemes().getColorTwoFront().add((String)rs.getObject(5));
				vo.getThemes().getColorTwoBack().add((String)rs.getObject(6));
				vo.getThemes().getName().add((String)rs.getObject(7));
				vo.getThemes().getThemePdf().add((String)rs.getObject(8));
			}
		} catch(Exception e) {
			log.debug("Error fetching the themes from the Database",e);
		} finally{
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
	}
}
