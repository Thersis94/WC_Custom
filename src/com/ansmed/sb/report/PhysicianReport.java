package com.ansmed.sb.report;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

// ANS SB Libs
import com.ansmed.sb.physician.BusinessPlanVO;
import com.ansmed.sb.physician.ClinicVO;
import com.ansmed.sb.security.ANSRoleFilter;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilder Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>:PhysicianReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 2, 2008
 ****************************************************************************/
public class PhysicianReport extends SBActionAdapter {
	
	/**
	 * 
	 */
	public PhysicianReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Searching for physicians for report");
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		int maxClinics = 0;
		
		// Get the search params
		String tmName = StringUtil.checkVal(req.getParameter("tmName"));
		String lastName = StringUtil.checkVal(req.getParameter("lastName"));
		String zipCode = StringUtil.checkVal(req.getParameter("zipCode"));
		String state = StringUtil.checkVal(req.getParameter("state"));
		String physType = StringUtil.checkVal(req.getParameter("physicianType"));
		String includeDeact = StringUtil.checkVal(req.getParameter("includeDeact"));
		Integer allowMailFlag = Convert.formatInteger(req.getParameter("allowMailFlag"));
		Integer cassFlag = Convert.formatInteger(req.getParameter("cassFlag"));
		boolean includeBusPlan = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("includeBusPlan")));
		
		// Build the SQL Statement
		StringBuffer sql = getSQL(lastName, zipCode, state, tmName, physType, includeDeact,
				allowMailFlag, cassFlag, includeBusPlan, role);
		log.info("ANS Phys Search SQL: " + sql);
		
		// get the data
		PreparedStatement ps = null;
		Map<String, PhysicianContainerVO> data = new LinkedHashMap<String, PhysicianContainerVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			int i = 1;
			//ps.setInt(i++, allowMailFlag.intValue());
			if (tmName.length() > 0) ps.setString(i++, "%" + tmName + "%");
			if (lastName.length() > 0) ps.setString(i++, "%" + lastName + "%");
			if (zipCode.length() > 0) ps.setString(i++, zipCode + "%");
			if (state.length() > 0) ps.setString(i++, state);
			if (physType.length() > 0) ps.setString(i++, physType);
			
			// Get the results
			ResultSet rs = ps.executeQuery();
			PhysicianContainerVO phys = null;
			
			String id = null;
			String currId = "";
			
			String clinicId = null;
			String currClinicId = "";
			
			while(rs.next()) {
				id = rs.getString("surgeon_id");
				clinicId = rs.getString("clinic_id");
				
				if (currId.equals(id)) {
					if (includeBusPlan) phys.addBusinessPlan(new BusinessPlanVO(rs));
					if (!currClinicId.equals(clinicId)) {
						phys.addClinic(new ClinicVO(rs));
						log.debug("Added clinic for surgeonId: "  + phys.getSurgeonId());
					}
				} else {
					if (phys != null) {
						data.put(currId, phys);
						log.debug("phys NOT null...adding to map.  surgeonId : phys.getSurgeonID = " + currId + " : "+ phys.getSurgeonId());
						if (maxClinics < phys.getClinics().size()) {
							maxClinics = phys.getClinics().size();
						}
					}
					phys = new PhysicianContainerVO(rs);
					log.debug("New PhysicianContainerVO created for surgeonId: " + phys.getSurgeonId());
					log.debug("Added first clinic for surgeonId: " + phys.getSurgeonId());
					if (includeBusPlan) phys.addBusinessPlan(new BusinessPlanVO(rs));
					data.put(id, phys);
				}
				
				currId = id;
				currClinicId = clinicId;
			}
			if (phys != null) log.debug("Adding business plan: " + phys.getBusinessPlan().size());
			
			// Add the last entry (If size > 0)
			if (id != null) {
				log.debug("Adding last record to map.  surgeonId: " + id);
				data.put(id, phys);
			}
			if (phys != null) {
				if (maxClinics < phys.getClinics().size()) {
					maxClinics = phys.getClinics().size();
				}
			}
			
		} catch (SQLException sqle) {
			log.error("Error searching for surgeons", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Set the data to stream back
		log.info("Number Physicians Found: " + data.size());
		PhysicianReportVO rpt = new PhysicianReportVO();
		rpt.setData(data);
		rpt.setMaxClinics(maxClinics);
		
		// If including business plan data, get the business plan fields
		if (includeBusPlan) {
			rpt.setAttributes(this.getFields());
		}
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("searchSubmitted")))
			build(req);
	}
	
	/**
	 * Builds the SQL Statement for the report
	 * @param lName
	 * @param zip
	 * @param state
	 * @param tm
	 * @return
	 */
	protected StringBuffer getSQL(String lName, String zip, String state, String tm, 
								  String physType, String incDe, Integer am, Integer cassFlag, 
								  boolean includeBusPlan, SBUserRole role) {
		
		String schema = (String) this.getAttribute("customDbSchema");
		ANSRoleFilter filter = new ANSRoleFilter();
		Calendar cal = Calendar.getInstance();
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		if (includeBusPlan) {
			// include the business plan cross-ref table (i.*) in the query
			sql.append("select a.*, b.*, h.*, i.*, specialty_nm, type_nm, region_nm, ");
		} else {
			sql.append("select a.*, b.*, h.*, specialty_nm, type_nm, region_nm, ");
		}
		sql.append("g.first_nm + ' ' + g.last_nm as rep_nm from ");
		sql.append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_clinic b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("left outer join ").append(schema).append("ans_phone h ");
		sql.append("on b.clinic_id = h.clinic_id ");
		sql.append("left outer join ").append(schema).append("ans_specialty c ");
		sql.append("on a.specialty_id = c.specialty_id  ");
		sql.append("inner join ").append(schema).append("ans_sales_rep d ");
		sql.append("on a.sales_rep_id = d.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region e ");
		sql.append("on d.region_id = e.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon_type f ");
		sql.append("on a.surgeon_type_id = f.surgeon_type_id ");
		sql.append("inner join ").append(schema).append("ans_sales_rep g ");
		sql.append("on a.sales_rep_id = g.sales_rep_id ");
		
		// Check to see if we need to exclude the business plan data
		if (includeBusPlan) {
			sql.append("left outer join ").append(schema).append("ans_xr_surgeon_busplan i ");
			sql.append("on a.surgeon_id = i.surgeon_id ");
			sql.append("and bp_year_no = ").append(cal.get(Calendar.YEAR));
		}
				
		sql.append(" where 1 = 1 ");
		
		if (Convert.formatInteger(incDe) < 1) sql.append("and status_id < 10 ");
		
		// Build the where clause and append the data to the nav element
		if (tm.length() > 0) sql.append("and g.last_nm like ? ");
		if (lName.length() > 0) sql.append("and a.last_nm like ? ");
		if (zip.length() > 0) sql.append("and zip_cd like ? ");
		if (state.length() > 0) sql.append("and state_cd = ? ");
		if (physType.length() > 0) sql.append("and a.surgeon_type_id = ? ");
		if (am > 0) sql.append("and allow_mail_flg = 1 ");
		if (cassFlag > 0) sql.append("and (cass_validate_flg = 0 or cass_validate_flg is null)");
		
		// Apply permissions
		sql.append(filter.getSearchFilter(role, "d", false));
		sql.append(" order by last_nm, first_nm, b.location_type_id, a.surgeon_id, b.clinic_id ");
		return sql;
	}
	
	/**
	 * Retrieves the Business Plan Fields and IDs
	 * @param schema
	 * @return
	 */
	protected Map<String, String> getFields() {
		String schema = (String)this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String, String> fields = new LinkedHashMap<String, String>();
		StringBuffer sb = new StringBuffer();
		sb.append("select business_plan_id, field_nm ");
		sb.append("from ").append(schema).append("ans_business_plan ");
		sb.append("order by field_nm");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				fields.put(rs.getString(1), rs.getString(1));
			}
		} catch (Exception e) {
			log.error("Error Retrieving Business Plan Fields", e);
		}
		
		return fields;
	}
}
