package com.ansmed.sb.physician;

// SMT Base Libs 2.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs 2.0
import com.ansmed.sb.action.EpiducerMailFormatter;
import com.ansmed.sb.security.ANSRoleFilter;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SurgeonSearchAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since August 15, 2007
 ****************************************************************************/
public class SurgeonSearchAction extends SBActionAdapter {
	public static final int DEACTIVATION_CODE = 10;
	public static final String SURGEON_ID = "surgeonId";
	public static final String CLINIC_ID = "clinicId";
	public static final String FAILED_PHYSICIAN_UPDATE = "failedPhysicianUpdate";
	
    SiteBuilderUtil util = null;
    
	/**
	 * 
	 */
	public SurgeonSearchAction() {
        util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public SurgeonSearchAction(ActionInitVO actionInit) {
		super(actionInit);
        util = new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String message = "You have successfully updated the physician information";
		
		// Set auto commit off so you can roll back
		try {
			dbConn.setAutoCommit(false);
		} catch (SQLException e) {}
		
		//Update the physician info
		this.update(req);
		Integer success = Convert.formatInteger((Integer)req.getAttribute(FAILED_PHYSICIAN_UPDATE));
		Boolean physOnly = Convert.formatBoolean(req.getParameter("physOnly"));
		
		// If the surgeon info updated, add the clinic info
		// If not, roll back and set the error message
		if (success > -1 && ! physOnly) {
			// Update the clinic
			SMTActionInterface aac = new PhysicianClinicAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.update(req);
			success = Convert.formatInteger((Integer)req.getAttribute(FAILED_PHYSICIAN_UPDATE));
		}
		
		// Rollback on a failure
		if (success < 0) {
			try {
				dbConn.rollback();
			} catch(Exception e) {}
			message = "Error: Unable to update physician information";
		} else {
			try {
				dbConn.commit();
			} catch(Exception e) {}
		}
		
		// Set auto commit back on
		try {
			dbConn.setAutoCommit(true);
		} catch (SQLException e) {}
		
		// Notify sales rep if 'notify' flag was set
		if (StringUtil.checkVal(req.getParameter("notifyRepTrainingGroupAssignment")).length() > 0) {
			//if (StringUtil.checkVal(req.getParameter("")).length() > 0) {
				this.sendEmailNotification(req);
			//}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		log.info("deactivating surgeon: " + req.getParameter("surgeonId"));
		String message = "You have successfully deactivated the physician";
		StringEncoder se = new StringEncoder();
		Boolean deleteEntries = Convert.formatBoolean(req.getParameter("deleteEntries"));
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		
		if (! deleteEntries) {
			sql.append("update ").append((String)getAttribute("customDbSchema"));
			sql.append("ans_surgeon set status_id = ").append(DEACTIVATION_CODE);
			sql.append(" where surgeon_id = ?");
		} else {
			message = "You have successfully deleted the deactivated the physicians";
			sql.append("delete from ").append((String)getAttribute("customDbSchema"));
			sql.append("ans_surgeon where status_id = ").append(DEACTIVATION_CODE);
		}
		
		log.info("Deactivate/Deleting Physicians SQL: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (! deleteEntries) ps.setString(1, req.getParameter("surgeonId"));
			ps.executeUpdate();
		} catch (SQLException sqle) {
			message = "Error Deactivating Physician";
			log.error("Error searching for surgeons", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Build the redir url
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?");
		url.append("page=").append(StringUtil.checkVal(req.getParameter("page")));
		url.append("&order=").append(StringUtil.checkVal(req.getParameter("order")));
		url.append("&firstName=").append(se.decodeValue(req.getParameter("firstName")));
		url.append("&lastName=").append(se.decodeValue(req.getParameter("lastName")));
		url.append("&state=").append(StringUtil.checkVal(req.getParameter("state")));
		url.append("&zipCode=").append(StringUtil.checkVal(req.getParameter("zipCode")));
		url.append("&searchSubmitted=true");
		
        // Add the message to the redirect
		url.append("&msg=").append(message);
		log.info("URL: " + url);
		
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("Getting the detail info for a surgeon");
		int count = 0;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.*, status_nm, specialty_nm, type_nm from ");
		sql.append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_status b ");
		sql.append("on a.status_id = b.status_id ");
		sql.append("left outer join ").append(schema).append("ans_specialty c ");
		sql.append("on a.specialty_id = c.specialty_id ");
		sql.append("inner join ").append(schema).append("ans_sales_rep d on a.sales_rep_id = d.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region e on d.region_id = e.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon_type f on a.surgeon_type_id = f.surgeon_type_id ");
		sql.append("where a.surgeon_id = ? ");
		
		// Add the role filter
		sql.append(filter.getSearchFilter(role, "d"));
		
		log.debug("ANS Phys Detail Info SQL: " + sql + "|" + req.getParameter("surgeonId"));
		
		PreparedStatement ps = null;
		SurgeonVO vo = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				count = 1;
				vo = new SurgeonVO(rs);
				
				// Get the Clinic Info
				SMTActionInterface ca = new PhysicianClinicAction(this.actionInit);
				ca.setAttributes(this.attributes);
				ca.setDBConnection(dbConn);
				ca.retrieve(req);
				List<ClinicVO> clinics = (List<ClinicVO>)req.getAttribute(PhysicianClinicAction.CLINIC_DATA);
				vo.setClinics(clinics);
				
				// Get the Staff Info
				SMTActionInterface sa = new PhysicianStaffAction(this.actionInit);
				sa.setAttributes(this.attributes);
				sa.setDBConnection(dbConn);
				sa.retrieve(req);
				List<StaffVO> staff = (List<StaffVO>)req.getAttribute(PhysicianStaffAction.STAFF_DATA);
				vo.setStaff(staff);
				
				// Get the Documents Info
				log.debug("Getting docs:");
				SMTActionInterface pd = new PhysicianDocumentAction(this.actionInit);
				pd.setAttributes(this.attributes);
				pd.setDBConnection(dbConn);
				pd.retrieve(req);
				List<DocumentVO> docs = (List<DocumentVO>)req.getAttribute(PhysicianDocumentAction.DOCUMENT_DATA);
				vo.setDocuments(docs);
			}

		} catch (SQLException sqle) {
			log.error("Error searching for surgeons", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Return the data to the user
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(vo);
		mod.setDataSize(count);
		attributes.put(Constants.MODULE_DATA, mod);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Searching for physicians");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String schema = (String) this.getAttribute("customDbSchema");
		NavManager nav = new NavManager();
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		StringEncoder se = new StringEncoder();
		
		// Get the search params
		String tmName = se.decodeValue(StringUtil.checkVal(req.getParameter("tmName")));
		String lastName = se.decodeValue(StringUtil.checkVal(req.getParameter("lastName")));
		String zipCode = StringUtil.checkVal(req.getParameter("zipCode"));
		String state = StringUtil.checkVal(req.getParameter("state"));
		Integer rank = Convert.formatInteger(req.getParameter("rank"));
		String reqRank = StringUtil.checkVal(req.getParameter("rank"));
		String order = StringUtil.checkVal((String)req.getParameter("order"), "rank_no desc,last_nm,first_nm ");
		
		// Build the nav object
		nav.setRpp(Convert.formatInteger(req.getParameter("rpp"), 75));
		nav.setCurrentPage(Convert.formatInteger(req.getParameter("page"), 1));
		StringBuffer baseUrl = new StringBuffer("?searchSubmitted=true");
		baseUrl.append("&rpp=").append(nav.getRpp());
		baseUrl.append("&tmName=").append(tmName);
		baseUrl.append("&lastName=").append(lastName);
		baseUrl.append("&state=").append(state);
		baseUrl.append("&zipCode=").append(zipCode);
		baseUrl.append("&order=").append(order);
		baseUrl.append("&rank=").append(reqRank);
		nav.setBaseUrl(baseUrl.toString());
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.*, specialty_nm, type_nm, ");
		sql.append("g.first_nm + ' ' + g.last_nm as rep_nm from ");
		sql.append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_clinic b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
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
		sql.append("where location_type_id = 1 ");
		
		// Build the where clause and append the data to the nav element
		if (tmName.length() > 0) sql.append("and g.last_nm like ? ");
		if (lastName.length() > 0) sql.append("and a.last_nm like ? ");
		if (zipCode.length() > 0) sql.append("and zip_cd like ? ");
		if (state.length() > 0) sql.append("and state_cd = ? ");
		if (reqRank.length() > 0) sql.append("and rank_no > ? ");
		
		// Add the role filter
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
			
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		// Finish the SQL clause
		sql.append("order by ").append(order);
		log.info("ANS Phys Search SQL: " + sql);
		log.debug("Start/Stop: " + nav.getStart() + "/" + nav.getEnd());
		PreparedStatement ps = null;
		List<SurgeonVO> data = new ArrayList<SurgeonVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			int i = 1;
			if (tmName.length() > 0) ps.setString(i++, "%" + tmName + "%");
			if (lastName.length() > 0) ps.setString(i++, "%" + lastName + "%");
			if (zipCode.length() > 0) ps.setString(i++, zipCode + "%");
			if (state.length() > 0) ps.setString(i++, state);
			if (reqRank.length() > 0) ps.setInt(i++, rank);
			
			// Get the results
			ResultSet rs = ps.executeQuery();
			int ctr = 1;
			for(;rs.next(); ctr++) {
				if (ctr >= nav.getStart() && ctr <= nav.getEnd()) {
					SurgeonVO phys = new SurgeonVO(rs);
					phys.addClinic(new ClinicVO(rs));
					data.add(phys);
				}
			}
			nav.setTotalElements(ctr - 1);
			log.debug("resultCnt=" + ctr);
			
		} catch (SQLException sqle) {
			log.error("Error searching for surgeons", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Define the pages
		// Return the data to the user
		req.setAttribute("navigationManager", nav);
		log.debug("Nav: " + nav.toString());
		log.debug("Num returned: " + nav.getTotalElements());
		mod.setDataSize(nav.getTotalElements());
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Updating physician");
		Integer success = 100;
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		SurgeonVO vo = new SurgeonVO(req);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		StringEncoder se = new StringEncoder();
		
		// Look for the profile id, if it's missing, add to the profile table
		if (StringUtil.checkVal(vo.getProfileId()).length() == 0) {
		    ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			try {
				UserDataVO user = vo.getUserInfo();
				log.debug("Added physician info to profile table with ID: " + user.getProfileId());
				pm.updateProfile(user, dbConn);
				vo.setProfileId(user.getProfileId());
			} catch(Exception e) {
				log.error("Unable to add profile", e);
			}
		}
		
		// Build the SQL statement
		if(surgeonId.length() > 0) {
			sql.append("update ").append(schema).append("ans_surgeon set ");
			sql.append("surgeon_type_id = ?, status_id = ?, specialty_id = ?, ");
			sql.append("sales_rep_id = ?, title_nm = ?, first_nm = ?, middle_nm = ?, ");
			sql.append("last_nm = ?, suffix_nm = ?, email_address_txt = ?, ");
			sql.append("website_url = ?, spouse_nm = ?, children_nm = ?, ");
			sql.append("cell_phone_no = ?, pager_no = ?, allow_mail_flg = ?, ");
			sql.append("spanish_flg = ?, board_cert_flg = ?, fellowship_flg = ?, ");
			sql.append("clinic_days = ?, procedure_days = ?, profile_id = ?, ");
			sql.append("scs_start_dt = ?, prod_approval_flg = ?, prod_group_no = ?, ");
			sql.append("medical_license_no = ?, national_provider_id = ?, update_dt = ? where surgeon_id = ?");
		} else {
			surgeonId = new UUIDGenerator().getUUID();
			req.setParameter("surgeonId", surgeonId);
			sql.append("insert into ").append(schema).append("ans_surgeon (");
			sql.append("surgeon_type_id, status_id, specialty_id, sales_rep_id, ");
			sql.append("title_nm, first_nm, middle_nm, last_nm, suffix_nm, ");
			sql.append("email_address_txt, website_url, spouse_nm, children_nm, ");
			sql.append("cell_phone_no, pager_no, allow_mail_flg, spanish_flg, ");
			sql.append("board_cert_flg, fellowship_flg, clinic_days, procedure_days, ");
			sql.append("profile_id, scs_start_dt, prod_approval_flg, prod_group_no, ");
			sql.append("medical_license_no, national_provider_id, create_dt, surgeon_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		}
		log.debug("Physician Update sql: " + sql + "|" + surgeonId);
		
		// Update the db
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getSurgeonTypeId());
			ps.setInt(2, vo.getStatusId());
			ps.setInt(3, vo.getSpecialtyId());
			ps.setString(4, vo.getSalesRepId());
			ps.setString(5, vo.getTitle());
			ps.setString(6, se.decodeValue(req.getParameter("physFirstName")).trim());
			ps.setString(7, vo.getMiddleName());
			ps.setString(8, se.decodeValue(req.getParameter("physLastName")).trim());
			ps.setString(9, vo.getSuffix());
			ps.setString(10, vo.getEmailAddress());
			ps.setString(11, vo.getWebsite());
			ps.setString(12, vo.getSpouseName());
			ps.setString(13, vo.getChildrenName());
			ps.setString(14, se.decodeValue(vo.getCellPhone()));
			ps.setString(15, se.decodeValue(vo.getPager()));
			ps.setInt(16, vo.getAllowMail());
			ps.setInt(17, vo.getSpanishFlag());
			ps.setInt(18, vo.getBoardCertifiedFlag());
			ps.setInt(19, vo.getFellowshipFlag());
			ps.setString(20, vo.parseMap(vo.getClinicDays(),","));
			ps.setString(21, vo.parseMap(vo.getProcedureDays(),","));
			ps.setString(22, vo.getProfileId());
			ps.setDate(23, Convert.formatSQLDate(vo.getScsStartDate()));
			ps.setInt(24, Convert.formatInteger(vo.getProductApprovalFlag()));
			ps.setInt(25, Convert.formatInteger(vo.getProductGroupNumber()));
			ps.setString(26, vo.getLicenseNumber());
			ps.setString(27, vo.getNationalProviderIdentifier());
			ps.setTimestamp(28, Convert.getCurrentTimestamp());
			ps.setString(29, surgeonId);
			
			// Update the db
			int count = ps.executeUpdate();
			if (count == 0) success = -1;
			log.debug("Count: " + count);
		} catch(SQLException sqle) {
			success = -1;
			log.error("Unable to add/update physician", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("SurgeonID/Success: " + surgeonId + "|" + success);
		// Append the surgeon Id to the req obj
		req.setAttribute(SurgeonSearchAction.SURGEON_ID, surgeonId);
		req.setAttribute(FAILED_PHYSICIAN_UPDATE, success);
	}
	
	/**
	 * Retrieves sales rep's profile data
	 * @param req
	 * @return
	 */
	private UserDataVO retrieveSalesRep(SMTServletRequest req) {
		UserDataVO rep = null;
		String profileId = StringUtil.checkVal(req.getParameter("salesRepId"));
		Map<String,Object> config = new HashMap<String,Object>();
		config.put(Constants.ENCRYPT_KEY, getAttribute(Constants.ENCRYPT_KEY));
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		try {
			rep = pm.getProfile(profileId, dbConn, "PROFILE_ID");
		} catch (DatabaseException de) {
			log.error("Error retrieving sales rep profile, ", de);
		}
		return rep;
	}
	
	/**
	 * Send a copy of the form submission to the designated recipient(s).
	 * @param req
	 * @param contactForm
	 * @param recipients
	 * @throws ActionException
	 */
	private void sendEmailNotification(SMTServletRequest req) {
		SurgeonVO phys = new SurgeonVO(req);
		// using req params for first/last name to prevent possible name pollution from first/last name
		// params used in search strings
		phys.setFirstName(StringUtil.checkVal(req.getParameter("physFirstName")));
		phys.setLastName(StringUtil.checkVal(req.getParameter("physLastName")));
		UserDataVO rep = this.retrieveSalesRep(req);
		if (rep == null || ! StringUtil.isValidEmail(rep.getEmailAddress())) return;
		
    	StringBuffer subject = new StringBuffer("Epiducer System Training Invitation | ");
    	subject.append(phys.getLastName());
    	subject.append(" | St. Jude Medical Neuromodulation");
    	    	    	
    	// set up the email formatter
    	EpiducerMailFormatter emf = new EpiducerMailFormatter();
    	emf.setSurgeon(phys);
    	emf.setRep(rep);
    	
    	StringBuffer body = emf.getPhysicianInvitation(phys.getProductGroupNumber());
    	
    	// set sender/recipient
    	String fromSender = "EpiducerTraining@sjmneuro.com";
    	String[] repRecpt = {rep.getEmailAddress()};
    	
    	/* DEBUG */
    	//String fromSender = "davedebug@siliconmtn.com";
    	//String[] repRecpt = {"dave@siliconmtn.com"};
    	/* END of DEBUG */
    	
    	// send the physician's invitation
    	SMTMail mail = null;
    	try {
       		// Create the mail object and send
    		mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
    		mail.setPort(new Integer((String)getAttribute(Constants.CFG_SMTP_PORT)));
    		mail.setRecpt(repRecpt);
    		mail.setCC(new String[] {"john.astorga@sjmneuro.com"});
    		mail.setFrom(fromSender);
    		mail.setSubject(subject.toString());
    		mail.setHtmlBody(body.toString());
			log.debug("Mail Info: " + mail.toString());	    		
    		mail.postMail();
    	} catch (MailException me) {
    		log.error("Error sending SJM email notification for ", me);
    	}
    	
    }
	
}
