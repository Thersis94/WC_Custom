package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codman.cu.tracking.vo.PhysicianVO;
import com.codman.cu.tracking.vo.RequestSearchVO;
import com.codman.cu.tracking.vo.UnitHistoryReportRepsVO;
import com.codman.cu.tracking.vo.UnitHistoryReportVO;
import com.codman.cu.tracking.vo.UnitSearchVO;
import com.codman.cu.tracking.vo.UnitVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.MSSQLRangeQuery;
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
 * <b>Title</b>: UnitAction.java<p/>
 * <b>Description: Responsible for saving Unit data into the DB.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Oct 30, 2014
 ****************************************************************************/
public class UnitAction extends SBActionAdapter {

	public static final int STATUS_IN_USE = 130;
	public static final int STATUS_AVAILABLE = 120;
	public static final int STATUS_BEING_SERVICED = 110;
	public static final int STATUS_DECOMMISSIONED = 100;
	public static final int STATUS_RETURNED = 140;
	
	private Object msg = null;

	//map storing possible sort fields for the retrieve query
	private static Map<String, String> sortMap = new HashMap<String,String>(){
		private static final long serialVersionUID = 1l; 
		{
			put("serial","a.serial_no_txt");
			put("status","f.status_nm");
			put("account","d.account_nm");
			put("date","isnull(a.deployed_dt,a.update_dt) desc");
		}
	};

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
		if (id == UnitAction.STATUS_RETURNED) return "Returned";
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
		StringBuilder url = new StringBuilder();
		if (StringUtil.checkVal(req.getParameter("redir")).length() > 0) {
			url.append(req.getParameter("redir"));
			url.append("?type=unit&accountId=").append(req.getParameter("accountId"));
		} else {
			url.append(req.getRequestURI()).append("?1=1");
		}
		url.append("&prodCd=").append( StringUtil.checkVal(req.getParameter("prodCd")));
		url.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		log.debug("redirUrl = " + url);

	}

	public String saveUnit(UnitVO vo) throws SQLException {
		msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();

		if (StringUtil.checkVal(vo.getUnitId()).length() == 0 && vo.getParentId() == null)
			vo.setUnitId(findUnitId(vo.getSerialNo(), vo.getOrganizationId()));

		if (vo.getUnitId() == null || vo.getUnitId().length() == 0) {
			vo.setUnitId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("codman_cu_unit (serial_no_txt, software_rev_no, hardware_rev_no, ");
			sql.append("comments_txt, unit_status_id, create_dt, organization_id, ");
			sql.append("parent_id, ifu_art_no, ifu_rev_no, prog_guide_art_no, prog_guide_rev_no, ");
			sql.append("battery_type, battery_serial_no, lot_number, service_ref, service_dt, ");
			sql.append("modifying_user_id, production_comments_txt,product_cd, ");
			sql.append("battery_recharge_dt, unit_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
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
			sql.append("service_dt=?, modifying_user_id=?, production_comments_txt=?, ");
			sql.append("product_cd=?, battery_recharge_dt=? where unit_id=?");

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
			ps.setString(20, vo.getProductCode()); 
			ps.setDate(21, Convert.formatSQLDate(vo.getBatteryRechargeDate()));
			ps.setString(22, vo.getUnitId());
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
		UnitVO vo = retrieveUnit(unitId);

		if (vo != null) {
			//send this record to the saveUnit method for re-insertion,
			//note we're moving the pkId to the parentId field, a new pkId will be created.
			log.debug("starting clone");
			vo.setParentId(vo.getUnitId());
			vo.setUnitId(null);
			this.saveUnit(vo);
		}

	}

	/**
	 * retrieves a fully-populated UnitVO from the database.
	 * Used for cloning, as well as when changing status from UnitReturnAction.
	 * @param unitId
	 * @return
	 */
	public UnitVO retrieveUnit(String unitId) {
		UnitVO vo = null;
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder(40);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("CODMAN_CU_UNIT where unit_id=?");


		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, unitId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				vo = new UnitVO(rs);

		} catch (SQLException sqle) {
			log.error("could not load unitVO", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		return vo;
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 *
	 * modified 2/13/2012, added city and country to query.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		final String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) role = new SBUserRole();
		String join = (role.getRoleLevel() != 10) ? "left outer join " : "inner join ";

		if (req.hasParameter("historyReport")) {
			this.historyReport(req, role.getRoleLevel());
			return;
		}

		List<UnitVO> data = new ArrayList<UnitVO>();
		String prodCd = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1), UnitVO.ProdType.MEDSTREAM.toString());
		UnitSearchVO search = new UnitSearchVO(req, prodCd);
		String unitId = req.getParameter("unitId");
		boolean isReport = Convert.formatBoolean(req.getParameter("excel"));
		log.debug("prod=" + prodCd);

		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.*, f.status_nm, e.profile_id as phys_profile_id, ");
		sql.append("d.account_nm, g.profile_id as rep_person_id, e.center_txt, e.department_txt, ");
		sql.append("b.create_dt as ledger_deployed_dt, d.city_nm, d.country_cd, c.transaction_type_id ");
		//sql.append(" ROW_NUMBER() OVER (ORDER BY ").append(getSortOrder(search)).append(") AS 'RowNumber' ");
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
		sql.append("where a.organization_id='").append(site.getOrganizationId()).append("' ");

		// add any search filters
		if (search.getStatusId() != null) sql.append(" and a.unit_status_id=").append(search.getStatusId());  //unit status
		if (search.getAccountName() != null) sql.append(" and d.account_nm like '").append(search.getAccountName()).append("%' ");
		if (search.getSerialNoText() != null) sql.append(" and a.serial_no_txt like '").append(search.getSerialNoText()).append("%' ");
		if (unitId != null) sql.append(" and a.unit_id='").append(unitId).append("' ");
		if (prodCd != null) sql.append(" and a.product_cd='").append(prodCd).append("' ");
		sql.append(" and parent_id is null ");

		//limit reps to their accounts ONLY
		//if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL)
		//int rLevel = role.getRoleLevel();
		//if (rLevel == 10)
		//	sql.append("and d.person_id=? ");

		//sort the results based on one of the fields. Since rep and physician fields 
		//are looked up in a separate process, the return value will be used to indicate
		//which of them should be used to organize the unit vo's
		//appendOrderBy(search, sql);
		
		//sql.append(") select * from qry where RowNumber between ").append(search.getStart()).append(" AND ").append(search.getEnd());

		log.debug(sql);
		MSSQLRangeQuery qryBldr = new MSSQLRangeQuery(getSortOrder(search), search.getStart(), search.getEnd());
		Statement ps = null;
		try {
			ps = dbConn.createStatement();


			//if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL)
			//if (rLevel == 10)
			//	ps.setString(i++, role.getProfileId());

			ResultSet rs = ps.executeQuery(qryBldr.buildRangeQuery(sql));
			while (rs.next()) {
				PhysicianVO phys = new PhysicianVO();
				phys.setCenterText(rs.getString("center_txt"));
				phys.setDepartmentText(rs.getString("department_txt"));
				UnitVO unit = new UnitVO(rs);
				unit.setTransactionType(rs.getInt("transaction_type_id"));
				unit.setDeployedDate(rs.getDate("ledger_deployed_dt"));
				unit.setPhys(phys);
				data.add(unit);
			}
			rs.close();
			
			//get the total RS size
			rs = ps.executeQuery(qryBldr.buildCountQuery(sql));
			if (rs.next())
				req.setAttribute("resultCnt",rs.getInt(1));

		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		data = attachProfiles(search, data);

		mod.setActionData(data);
		mod.setDataSize(data.size());
		if (msg != null) mod.setErrorMessage(msg.toString());
		setAttribute(Constants.MODULE_DATA, mod);
		log.debug("loaded " + data.size() + " units");

		//support exporting this data set to reports
		if (isReport) {
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
	
	
	/**
	 * query ProfileManager for the encrypted personal information (names)
	 * @param search
	 * @param profileIds
	 * @param data
	 */
	private List<UnitVO> attachProfiles(RequestSearchVO search, List<UnitVO> data) {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Set<String> profileIds = new HashSet<String>();
		for (UnitVO vo : data) {
			profileIds.add(vo.getRepId());
			profileIds.add(vo.getPhysicianId());
			profileIds.add(vo.getModifyingUserId());
		}
		
		List<UnitVO> newResults = new ArrayList<UnitVO>(data.size());
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
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

		} catch (Exception e) {
			log.error("could not lookup profileIds attached to Units", e);
		}
		return newResults;
	}

	/**
	 * Helper to append the order by clause to the retrieve statement. Some unit
	 * fields (the names of the reps, physicians, and modifying user) are not 
	 * fetched until after the retrieve query is done. So they will be sorted 
	 * with a comparator in UnitVO when requested.
	 * @param req
	 * @param sql
	 */
	private String getSortOrder(RequestSearchVO search) {
		String sort = search.getSort();

		//if the user chooses a sort field, use it as the first one
		if (sortMap.containsKey(sort)) {
			return sortMap.get(sort) + ", b.create_dt";
		} else {
			return "b.create_dt, a.serial_no_txt";
		}
	}


	private void historyReport(SMTServletRequest req, int roleLevel) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> profileIds = new ArrayList<String>();
		List<UnitVO> data = new ArrayList<UnitVO>();
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder(200);
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
		UnitSearchVO search = new UnitSearchVO(req, "MEDSTREAM");
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
