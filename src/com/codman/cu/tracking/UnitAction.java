package com.codman.cu.tracking;

import java.sql.PreparedStatement;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codman.cu.tracking.vo.PhysicianVO;
import com.codman.cu.tracking.vo.UnitHistoryReportRepsVO;
import com.codman.cu.tracking.vo.UnitHistoryReportVO;
import com.codman.cu.tracking.vo.UnitSearchVO;
import com.codman.cu.tracking.vo.UnitVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: RegistrationAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 26, 2010
 ****************************************************************************/
public class UnitAction extends SBActionAdapter {

	public static final int STATUS_IN_USE = 130;
	public static final int STATUS_AVAILABLE = 120;
	public static final int STATUS_BEING_SERVICED = 110;
	public static final int STATUS_DECOMMISSIONED = 100;
	private Object msg = null;
	
	public UnitAction() {
		super();
	}
	
	public UnitAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public static String getStatusName(int id) {
		if (id == UnitAction.STATUS_IN_USE) return "In-Use";
		if (id == UnitAction.STATUS_AVAILABLE) return "Available";
		if (id == UnitAction.STATUS_BEING_SERVICED) return "Being Serviced";
		if (id == UnitAction.STATUS_DECOMMISSIONED) return "Decommissioned";
		else return "";
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		try {
			this.saveUnit(new UnitVO(req));
		} catch (SQLException sqle) {
			log.error(sqle);
		}
		
		// Setup the redirect
    	StringBuffer url = new StringBuffer();
    	if (StringUtil.checkVal(req.getParameter("redir")).length() > 0) {
    		url.append(req.getParameter("redir"));
    		url.append("?type=unit&accountId=").append(req.getParameter("accountId"));
    	} else {
    		url.append(req.getRequestURI()).append("?1=1");
    	}
    	url.append("&msg=").append(msg);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
		
	}
	
	public String saveUnit(UnitVO vo) throws SQLException {
		msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		PreparedStatement ps = null;
		StringBuffer sql = new StringBuffer();
		
		if (StringUtil.checkVal(vo.getUnitId()).length() == 0 && vo.getParentId() == null)
			vo.setUnitId(findUnitId(vo.getSerialNo(), vo.getOrganizationId()));

		if (vo.getUnitId() == null || vo.getUnitId().length() == 0) {
			vo.setUnitId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("codman_cu_unit (serial_no_txt, software_rev_no, hardware_rev_no, ");
			sql.append("comments_txt, unit_status_id, create_dt, organization_id, ");
			sql.append("parent_id, ifu_art_no, ifu_rev_no, prog_guide_art_no, prog_guide_rev_no, ");
			sql.append("battery_type, battery_serial_no, lot_number, service_ref, service_dt, ");
			sql.append("modifying_user_id, production_comments_txt, unit_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			//make a copy of the existing record (for history) before updating this record
			//parentId is only passed from the update Unit form.
			if (vo.getParentId() != null) this.cloneRecord(vo.getUnitId());
			
			//flush parentId so we don't impact the master record
			vo.setParentId(null);
			
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("codman_cu_unit set serial_no_txt=?, software_rev_no=?, ");
			sql.append("hardware_rev_no=?, comments_txt=?, ");
			sql.append("unit_status_id=?, update_dt=?, organization_id=?, parent_id=?, ");
			sql.append("ifu_art_no=?, ifu_rev_no=?, prog_guide_art_no=?, prog_guide_rev_no=?, ");
			sql.append("battery_type=?, battery_serial_no=?, lot_number=?, service_ref=?, ");
			sql.append("service_dt=?, modifying_user_id=?, production_comments_txt=? where unit_id=?");
			
		}
		log.debug(sql + "|" + vo.getUnitId() + "|" + vo.getStatusId());
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getSerialNo());
			ps.setString(2, vo.getSoftwareRevNo());
			ps.setString(3, vo.getHardwareRevNo());
			ps.setString(4, vo.getCommentsText());
			ps.setString(5, String.valueOf(vo.getStatusId()));
			//preseve the date on the previous record if we're creating a clone
			ps.setTimestamp(6, (vo.getCreateDate() != null) ? new java.sql.Timestamp(vo.getCreateDate().getTime()) : Convert.getCurrentTimestamp());
			ps.setString(7, vo.getOrganizationId());
			ps.setString(8, vo.getParentId());
			ps.setString(9, vo.getIfuArticleNo());
			ps.setString(10, vo.getIfuRevNo());
			ps.setString(11, vo.getProgramArticleNo());
			ps.setString(12, vo.getProgramRevNo());
			ps.setString(13, vo.getBatteryType());
			ps.setString(14, vo.getBatterySerNo());
			ps.setString(15, vo.getLotNo());
			ps.setString(16, vo.getServiceRefNo());
			ps.setTimestamp(17, Convert.getTimestamp(vo.getServiceDate(), false));
			ps.setString(18, vo.getModifyingUserId());
			ps.setString(19, vo.getProductionCommentsText());
			ps.setString(20, vo.getUnitId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return vo.getUnitId();
	}
	
	
	/**
	 * creates a copy of an existing record; 
	 * used for the historical data reporting (a ledger entry!)
	 * @param unitId
	 * @throws SQLException
	 */
	private void cloneRecord(String unitId) throws SQLException {
		UnitVO vo = null;
		PreparedStatement ps = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("CODMAN_CU_UNIT where unit_id=?");
		
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, unitId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				vo = new UnitVO(rs);
			
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (vo != null) {
			//send this record to the saveUnit method for re-insertion,
			//note we're moving the pkId to the parentId field, a new pkId will be created.
			log.debug("starting clone");
			vo.setParentId(vo.getUnitId());
			vo.setUnitId(null);
			this.saveUnit(vo);
		}
		
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 *
	 * modified 2/13/2012, added city and country to query.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) role = new SBUserRole();
		String join = (role.getRoleLevel() != 10) ? "left outer join " : "inner join ";

		if (req.hasParameter("historyReport")) {
			this.historyReport(req, role.getRoleLevel());
			return;
		}
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<UnitVO> data = new ArrayList<UnitVO>();
		List<String> profileIds = new ArrayList<String>();
		final String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		UnitSearchVO search = new UnitSearchVO(req);
		String unitId = req.getParameter("unitId");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, f.status_nm, e.profile_id as phys_profile_id, ");
		sql.append("d.account_nm, g.profile_id as rep_person_id, e.center_txt, e.department_txt, ");
		sql.append("b.create_dt as deployed_dt, d.city_nm, d.country_cd, c.transaction_type_id ");
		sql.append("from ").append(custom_db);
		sql.append("codman_cu_unit a ").append(join).append(custom_db);
		sql.append("codman_cu_unit_ledger b on a.unit_id=b.unit_id and b.active_record_flg=1 ");
		sql.append(join).append(custom_db).append("codman_cu_transaction c ");
		sql.append("on b.transaction_id=c.transaction_id ");
		sql.append(join).append(custom_db).append("codman_cu_account d ");
		sql.append("on c.account_id=d.account_id ");
		sql.append(join).append(custom_db).append("codman_cu_physician e ");
		sql.append("on c.physician_id=e.physician_id ").append(join).append(custom_db);
		sql.append("codman_cu_status f on a.unit_status_id=f.status_id  ").append(join).append(custom_db);
		sql.append("CODMAN_CU_PERSON g on d.PERSON_ID=g.PERSON_ID ");
		sql.append("where a.organization_id=? ");

		// add any search filters
		if (search.getStatusId() != null) sql.append("and a.unit_status_id=? ");  //unit status
		if (search.getAccountName() != null) sql.append("and d.account_nm like ? ");
		if (search.getSerialNoText() != null) sql.append("and a.serial_no_txt like ? ");
		if (unitId != null) sql.append("and a.unit_id=? ");
		
		//limit reps to their accounts ONLY
		//if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL)
		//int rLevel = role.getRoleLevel();
		//if (rLevel == 10)
		//	sql.append("and d.person_id=? ");
		
		sql.append("order by b.create_dt, a.serial_no_txt");
		log.debug(sql);
		int i = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			// add any search filters
			ps.setString(i++, site.getOrganizationId());
			if (search.getStatusId() != null) ps.setInt(i++, search.getStatusId());
			if (search.getAccountName() != null) ps.setString(i++, search.getAccountName() + "%");
			if (search.getSerialNoText() != null) ps.setString(i++, search.getSerialNoText() + "%");
			if (unitId != null) ps.setString(i++, unitId);
			
			//if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL)
			//if (rLevel == 10)
			//	ps.setString(i++, role.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if(rs.getString("parent_id") == null || rs.getString("parent_id").equals("")) {
					UnitVO unit = new UnitVO(rs);
					unit.setTransactionType(rs.getInt("transaction_type_id"));
					PhysicianVO phys = new PhysicianVO();
					phys.setCenterText(rs.getString("center_txt"));
					phys.setDepartmentText(rs.getString("department_txt"));
					unit.setPhys(phys);
					data.add(unit);
					
					profileIds.add(rs.getString("rep_person_id"));
					profileIds.add(rs.getString("phys_profile_id"));
					profileIds.add(rs.getString("modifying_user_id"));
				}
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//query ProfileManager for the encrypted personal information (names)
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UnitVO> newResults = new ArrayList<UnitVO>();
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, profileIds);
			for (UnitVO vo : data) {
				//bind the Rep
				UserDataVO user = profiles.get(vo.getRepId());
				if (user != null) vo.setRepName(user.getFirstName() + " " + user.getLastName());
				
				//strip out any results not matching the desired rep.  
				//Since lastName is encrypted this has to be done here (post processing)
				if (search.getRepLastName() != null) {
					if (!vo.getRepName().toUpperCase().contains(search.getRepLastName().toUpperCase())) {
						continue;
					}
				}
				
				//bind the Modifying User
				UserDataVO moderator = profiles.get(vo.getModifyingUserId());
				if(moderator != null)
					vo.setModifyingUserName(moderator.getFirstName() + " " + moderator.getLastName());
				
				
				//bind the Physician
				user = profiles.get(vo.getPhysicianId());
				if (user != null) {
					vo.setPhysicianName(user.getFirstName() + " " + user.getLastName());
					vo.getPhysician().setData(user.getDataMap());
				}
				
				newResults.add(vo);
			}
			
			data = newResults;
		} catch (Exception e) {
			log.error("could not lookup profileIds attached to Units", e);
		}
		
		mod.setActionData(data);
		mod.setDataSize(data.size());
		if (msg != null) mod.setErrorMessage(msg.toString());
		setAttribute(Constants.MODULE_DATA, mod);
		log.debug("loaded " + data.size() + " units");
		
		//support exporting this data set to reports
		if (Convert.formatBoolean(req.getParameter("excel"))) {		
			AbstractSBReportVO rpt = null;
			//do a full report for non-sales-reps, do a limited report for Reps
			if (role.getRoleLevel() > SecurityController.PUBLIC_REGISTERED_LEVEL) {
				rpt = new UnitHistoryReportVO(site);
			} else {
				rpt = new UnitHistoryReportRepsVO(site);
			}
			rpt.setData(data);
	        rpt.setFileName("Control Unit Summary Report.xls");
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		}

	}
	
	private void historyReport(SMTServletRequest req, int roleLevel) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> profileIds = new ArrayList<String>();
		List<UnitVO> data = new ArrayList<UnitVO>();
		PreparedStatement ps = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, d.account_nm, g.profile_id as 'phys_profile_id', ");
		sql.append("e.center_txt, e.department_txt, c.transaction_type_id, ");
		sql.append("g.profile_id as rep_person_id, ");
		sql.append("b.create_dt as deployed_dt ");
		sql.append("from ").append(customDb);
		sql.append("codman_cu_unit a left outer join ").append(customDb);
		sql.append("codman_cu_unit_ledger b on a.unit_id=b.unit_id "); 
		sql.append("left outer join ").append(customDb);
		sql.append("codman_cu_transaction c on b.transaction_id=c.transaction_id "); 
		sql.append("left outer join ").append(customDb);
		sql.append("codman_cu_account d	on c.account_id=d.account_id ");
		sql.append("left outer join ").append(customDb);
		sql.append("codman_cu_physician e on c.physician_id=e.physician_id ");
		sql.append("left outer join ").append(customDb);
		sql.append("CODMAN_CU_PERSON g on d.PERSON_ID=g.PERSON_ID ");
		sql.append("where a.unit_id=? or a.parent_id=? ");
		sql.append("order by isnull(c.update_dt, c.create_dt), isnull(a.update_dt,a.create_dt)");
		String unitId = req.getParameter("unitId");
		UnitSearchVO search = new UnitSearchVO(req);
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, unitId);
			ps.setString(2, unitId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				UnitVO unit = new UnitVO(rs);
				unit.setTransactionType(rs.getInt("transaction_type_id"));
				data.add(unit);
				
				profileIds.add(rs.getString("rep_person_id"));
				profileIds.add(rs.getString("modifying_user_id"));
				profileIds.add(rs.getString("phys_profile_id"));
			}
			
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UnitVO> newResults = new ArrayList<UnitVO>();
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, profileIds);
			for (UnitVO vo : data) {
				//bind the Rep
				UserDataVO user = profiles.get(vo.getRepId());
				if (user != null) vo.setRepName(user.getFirstName() + " " + user.getLastName());
				
				//strip out any results not matching the desired rep.  
				//Since lastName is encrypted this has to be done here (post processing)
				if (search.getRepLastName() != null) {
					if (!vo.getRepName().toUpperCase().contains(search.getRepLastName().toUpperCase())) {
						continue;
					}
				}
				
				//bind the Modifying User
				UserDataVO moderator = profiles.get(vo.getModifyingUserId());
				if (moderator != null) vo.setModifyingUserName(moderator.getFirstName() + " " + moderator.getLastName());
				
				
				//bind the Physician
				user = profiles.get(vo.getPhysicianId());
				if (user != null) {
					vo.setPhysicianName(user.getFirstName() + " " + user.getLastName());
					vo.getPhysician().setData(user.getDataMap());
				}
				
				newResults.add(vo);
			}
			
			data = newResults;
		} catch (Exception e) {
			log.error("could not lookup profileIds attached to Units", e);
		}
		

		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		AbstractSBReportVO rpt = null;
		//do a full report for non-sales-reps, do a limited report for Reps
		if (roleLevel > SecurityController.PUBLIC_REGISTERED_LEVEL) {
			rpt = new UnitHistoryReportVO(site);
		} else {
			rpt = new UnitHistoryReportRepsVO(site);
		}
		rpt.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
	
	/**
	 * simple method to verify if we already have this Unit in the database,
	 * or if we need to add it.
	 * @param serialNo
	 * @return
	 */
	private String findUnitId(String serialNo, String orgId) {
		if (serialNo == null || serialNo.length() == 0) return null;
		
		String unitId = null;
		final String sql = "select unit_id from " + ((String) getAttribute(Constants.CUSTOM_DB_SCHEMA)) +
			"codman_cu_unit where serial_no_txt=? and organization_id=?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, serialNo);
			ps.setString(2, orgId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				unitId = rs.getString(1);
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return unitId;
	}
	

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
