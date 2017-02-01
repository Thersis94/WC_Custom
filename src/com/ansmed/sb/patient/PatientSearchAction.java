package com.ansmed.sb.patient;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// ANS Libs
import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.security.AnsRoleModule;

// SMT Bse Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>:PatientSearchAction.java<p/>
 * <b>Description: </b> Searches for patients to display a search list or provide
 * detailed information.  Queries follow Security rules for roles.  Retrieve all 
 * method is used for the patient report as well
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 18, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PatientSearchAction extends SBActionAdapter {

	/**
	 * 
	 */
	public PatientSearchAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public PatientSearchAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/**
	 * Performs a search for all patient information on the MD Journal
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		String patientId = StringUtil.checkVal(req.getParameter("patientId"));
		Boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		Boolean isReport = Convert.formatBoolean(req.getParameter("report"));
		
		log.debug("Search params: " + searchSubmitted + "|" + patientId);
		
		// Make sure the user performed a search
		if (searchSubmitted && patientId.length() == 0) {
			if (! isReport) {
				int fn = StringUtil.checkVal(req.getParameter("firstName")).length();
				int ln = StringUtil.checkVal(req.getParameter("lastName")).length();
				int pn = StringUtil.checkVal(req.getParameter("phoneNumber")).length();
				
				if (fn == 0 && ln == 0 && pn == 0) {
					String url = "?msg=You+must+enter+search+data+for+at+least+one+field";
					req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
					req.setAttribute(Constants.REDIRECT_URL, url);
					return;
				}
			}
			
			// Get the data
			this.retrieveAll(req);
			
			log.debug("Data Retrieved");
			
			// If the data is being presented as a report, format the report
			if (isReport) {
				
				ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
				AbstractSBReportVO rpt = new StimTrackerReport();
				rpt.setData(mod.getActionData());
				rpt.setFileName("StimTrackerReport." + req.getParameter("reportType", "html"));
				log.debug("Mime Type: " + rpt.getContentType());
				
				req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
				req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
			}
		} else if (searchSubmitted && patientId.length() > 0) {
			this.retrievePatient(req, patientId);
		}
	}
	
	/**
	 * Retrieves an individual patient record
	 * @param req
	 * @param id
	 * @throws ActionException
	 */
	public void retrievePatient(ActionRequest req, String id) throws ActionException {
		log.debug("Searching for patients");

		// Retrieve attributes needed
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
		
		// Build the sql statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*,b.*, c.region_nm, d.first_nm, d.last_nm from ");
		sql.append(schema).append("ans_stim_tracker a ");
		sql.append("inner join ").append(schema).append("ans_patient_proc b ");
		sql.append("on a.stim_tracker_id = b.stim_tracker_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region c ");
		sql.append("on a.region_id = c.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon d ");
		sql.append("on a.surgeon_id = d.surgeon_id ");
		sql.append("where a.stim_tracker_id = ? ");
		
		// execute the SQL Statement
		PreparedStatement ps = null;
		PatientVO vo = new PatientVO();
		try {
			log.debug("Patient SQL: " + sql + "|" + id);
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo.setData(rs, encKey);
			}
		} catch(Exception e) {
			log.error("Unable to search for patients", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Save the action data
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(vo);
	}
	
	/**
	 * Retrieve all users that match the search criteria
	 * @param req
	 * @throws ActionException
	 */
	public void retrieveAll(ActionRequest req) throws ActionException {
		log.debug("Searching for patients");
		
		// Get the search params
		String fn = StringUtil.checkVal(req.getParameter("firstName"));
		String ln = StringUtil.checkVal(req.getParameter("lastName"));
		String pn = StringUtil.checkVal(req.getParameter("phoneNumber"));
		String salesRepId = StringUtil.checkVal(req.getParameter("salesRepId"));
		
		// Format the Dates
		Date startDate = null;
		Date endDate = null;
		if (StringUtil.checkVal(req.getParameter("startDate")).length() > 0)
			startDate = Convert.formatStartDate(req.getParameter("startDate"));
		if (StringUtil.checkVal(req.getParameter("endDate")).length() > 0)
			endDate = Convert.formatEndDate(req.getParameter("endDate"));
		log.debug("Dates: " + startDate + "|" + endDate);
		
		// Retrieve attributes needed
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		String region = StringUtil.checkVal(role.getAttribute(AnsRoleModule.ANS_REGION_ID));
		ANSRoleFilter rf = new ANSRoleFilter();
		log.debug(schema + "|" + encKey + "|" + role.getAttribute(AnsRoleModule.ANS_REGION_ID));
		
		// Build the sql statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*,c.*, b.region_nm, d.first_nm, d.last_nm from ");
		sql.append(schema).append("ans_stim_tracker a ");
		sql.append("inner join ").append(schema).append("ans_sales_region b on a.region_id = b.region_id ");
		sql.append("inner join ").append(schema).append("ans_patient_proc c on a.stim_tracker_id = c.stim_tracker_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon d on a.surgeon_id = d.surgeon_id ");
		sql.append("where 1 = 1 ");
		sql.append(rf.getSearchFilter(role,"c",false, false));
		if (region.length() > 0) sql.append("and a.region_id = ? ");
		if (fn.length() > 0) sql.append("and search_first_nm = ? ");
		if (ln.length() > 0) sql.append("and search_last_nm = ? ");
		if (pn.length() > 0) sql.append("and patient_phone_no = ? ");
		if (salesRepId.length() > 0) sql.append("and c.sales_rep_id = ? ");
		if (startDate != null) sql.append("and a.create_dt >= ? ");
		if (endDate != null) sql.append("and a.create_dt <= ? ");
		
		// execute the SQL Statement
		PreparedStatement ps = null;
		List<PatientVO> data = new ArrayList<PatientVO>();
		int ctr = 0;
		try {
			StringEncrypter se = new StringEncrypter(encKey);
			log.debug("Patient Search SQL: " + sql + "|" + salesRepId);
			ps = dbConn.prepareStatement(sql.toString());
			if (region.length() > 0) ps.setString(++ctr, region);
			if (fn.length() > 0) ps.setString(++ctr, se.encrypt(fn.toUpperCase()));
			if (ln.length() > 0) ps.setString(++ctr, se.encrypt(ln.toUpperCase()));
			if (pn.length() > 0) ps.setString(++ctr, new PhoneVO(pn).getPhoneNumber());
			if (salesRepId.length() > 0) ps.setString(++ctr, salesRepId);
			if (startDate != null) ps.setDate(++ctr, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(++ctr, Convert.formatSQLDate(endDate));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PatientVO vo = new PatientVO();
				vo.setData(rs, encKey);
				data.add(vo);
			}
		} catch(Exception e) {
			log.error("Unable to search for patients", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Save the action data
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setDataSize(data.size());
		mod.setActionData(data);
	}
}
