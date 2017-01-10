package com.sjm.corp.locator.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// J2EE 1.5
import com.siliconmtn.http.session.SMTSession;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AdminApprovalAction.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 28, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class AdminApprovalAction extends SBActionAdapter {

	/**
	 * 
	 */
	public AdminApprovalAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public AdminApprovalAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		if (StringUtil.checkVal(req.getParameter("dealerLocationId")).length() > 0) {
			req.setAttribute("dealerLocationId", req.getParameter("parentId"));
			SMTActionInterface sai = new ClinicManagerAction(this.actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
			return;
		}
		
		SMTSession ses = req.getSession();
		SBUserRole role = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		if (role.getRoleLevel() < 70) return;
		
		// Get the data and determine if it is country
		String org = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		String attr = null;
		boolean isCountry = false;
		if (role.getRoleLevel() == 70) {
			attr = (String) role.getAttribute(0); 
			isCountry = true;
		}
		
		// Get the dealers with pending activities
		List<DealerLocationVO> dlrs = this.getPendingDealers(isCountry, org, attr);
		this.putModuleData(dlrs, dlrs.size(), false);
	}

	/**
	 * 
	 * @param req
	 * @throws SQLException
	 */
	public void deletePending(ActionRequest req) throws SQLException {
		String s = "delete from dealer_location where dealer_location_id = ?";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, req.getParameter("dealerLocationId"));
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * 
	 * @param req
	 * @throws SQLException
	 */
	public void setOptOut(ActionRequest req) throws SQLException {
		String s = "update dealer_location set region_cd = 'OPT-OUT' where dealer_location_id = ?";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, req.getParameter("dealerLocationId"));
		ps.executeUpdate();
		ps.close();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		log.debug("Building the approvals");
		
		String msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			// Make sure the user is authorized to perform the transaction
			this.checkAuth(req);
			
			// update the transaction log
			boolean approved = this.updateTransaction(req);
			
			// move the entry from the pending to the live
			String parentId = StringUtil.checkVal(req.getParameter("parentId"));
			boolean isOptOut = this.isOptOut(parentId);
			
			int activeFlag = 1;
			if (isOptOut) activeFlag = 0;
			
			// Only move the record if it is approved or if the user is opting out
			// If the request is an initial request and it is approved, make it active
			if (approved && parentId.length() > 0) 
				this.movePending(req, activeFlag);
			else if (approved && parentId.length() == 0)
				this.makeActive(req.getParameter("dealerLocationId"));
			
			// delete the pending transaction
			if (parentId.length() > 0) this.deletePending(req);
			
			// Send the emails
			this.sendEmail(req, approved, isOptOut);
			
			// set the status to opt out if the clinic registered and was denied
			if (!approved && parentId.length() == 0) this.setOptOut(req);
			
		} catch(AuthenticationException ae) {
			msg = "You are not authorized to perform the requested action";
		} catch (MailException me) {
			log.error("Unable to send admin emails", me);
			msg = "Unable to send admin emails";
		} catch (Exception e) {
			log.error("Error handling SJM Approvals", e);
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA); 
		this.sendRedirect(page.getFullPath(), msg, req);
	}
	
	/**
	 * Updates the active flag for a new registration approval
	 * @param dlid
	 * @throws SQLException
	 */
	public void makeActive(String dlid) throws SQLException {
		String s = "update dealer_location set active_flg = 1 where dealer_location_id = ?";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, dlid);
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * 
	 * @param req
	 * @throws AuthenticationException
	 */
	public void checkAuth(ActionRequest req) throws AuthenticationException {
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		
		// If the user is a site admin, do nothing more
		if (role.getRoleLevel() == 100) return;
		
		// Make sure the user is at least a country admin
		if (role.getRoleLevel() < 70) throw new AuthenticationException("NOT_AUTH");
		
		// Make sure if the user is not a site admin that the country is in their data
		String attrib = StringUtil.checkVal(role.getAttribute(0));
		if (role.getRoleLevel() < 100 && attrib.length() == 0)
			throw new AuthenticationException("NOT_AUTH");
		
		// Check the country codes to ensure the user country matches the 
		// Clinic country
		String dlid = req.getParameter("dealerLocationId");
		String s = "select country_cd from dealer_location where dealer_location_id = ?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, dlid);
			ResultSet rs = ps.executeQuery();
			
			String cc = "";
			if (rs.next()) cc = StringUtil.checkVal(rs.getString(1));
			log.debug("Chech Auth: " + attrib + "|" + cc);
			if (attrib.indexOf(cc) == -1)
				throw new AuthenticationException("NOT_AUTH");
		} catch(SQLException sqle) {
			log.error("Unable to retrieve country code", sqle);
			throw new AuthenticationException("NOT_AUTH");
		}  finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	public boolean updateTransaction(ActionRequest req) throws SQLException {
		String cdb = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder tr = new StringBuilder();
		tr.append("insert into ").append(cdb).append("sjm_loc_transaction ");
		tr.append("(loc_transaction_id, dealer_location_id, transaction_dt, profile_id, ");
		tr.append("admin_note_txt, submitter_note_txt) values (?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(tr.toString());
		String transId = new UUIDGenerator().getUUID();
		ps.setString(1, transId);
		ps.setString(2, req.getParameter("dealerLocationId"));
		ps.setTimestamp(3, Convert.getCurrentTimestamp());
		ps.setString(4, ((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).getProfileId());
		ps.setString(5, req.getParameter("adminText"));
		ps.setString(6, req.getParameter("submitterText"));
		ps.executeUpdate();
		log.debug("inserted into trans: ");
		
		String s = "insert into " + cdb + "sjm_trans_reason_xr (loc_transaction_id, ";
		s += "trans_reason_id, create_dt) values (?,?,?)";
		ps = dbConn.prepareStatement(s);
		
		List<String> ele = new ArrayList<String>();
		String[] vals = req.getParameterValues("denyId");
		if (vals != null && vals.length > 0) {
			for (int i = 0; i < vals.length; i++) {
				ele.add(vals[i].substring(0, vals[i].indexOf("|")));
			}
		}
		
		ele.add(req.getParameter("approvalType"));
		for (int i=0; i < ele.size(); i++) {
			ps.setString(1, transId);
			ps.setInt(2, Convert.formatInteger(ele.get(i)));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		
		int[] count = ps.executeBatch();
		log.debug("Added to batch: " + count.length);
		ps.close();
		
		return "10".equals(req.getParameter("approvalType"));
	}
	
	/**
	 * 
	 * @param req
	 * @throws Exception
	 */
	public void movePending(ActionRequest req, int optOut) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("update dealer_location set location_nm = b.location_nm,");
		s.append("address_txt = b.address_txt, address2_txt = b.address2_txt, ");
		s.append("city_nm = b.city_nm, state_cd = b.state_cd, zip_cd = b.zip_cd, ");
		s.append("country_cd = b.country_cd, primary_phone_no = b.primary_phone_no, ");
		s.append("fax_no = b.fax_no, website_url = b.website_url,");
		s.append("email_address_txt = b.email_address_txt, geo_lat_no = b.geo_lat_no, ");
		s.append("geo_long_no = b.geo_long_no, match_cd = b.match_cd, ");
		s.append("bar_code_id = b.bar_code_id, cass_validate_flg = b.cass_validate_flg, ");
		s.append("man_geocode_flg = b.man_geocode_flg, region_cd = b.region_cd, ");
		s.append("active_flg = ?, update_dt = getdate() from dealer_location inner join dealer_location b ");
		s.append("on dealer_location.dealer_location_id = b.parent_id ");
		s.append("where b.parent_id = ?");
		log.debug("Move Pending SQL: "  + s + "|" + req.getParameter("parentId"));
		
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setInt(1, optOut);
		ps.setString(2, req.getParameter("parentId"));
		
		ps.executeUpdate();
	}
	
	/**
	 * Determines if the user has opted out 
	 * @param dlid
	 * @return
	 * @throws SQLException
	 */
	public boolean isOptOut(String dlid) throws SQLException {
		String s = "select region_cd from dealer_location where parent_id = ?";
		log.debug("********* OPT Out Check: " + s + "|" + dlid);
		
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, dlid);
		ResultSet rs = ps.executeQuery();
		
		boolean optOut = false;
		if (rs.next()) {
			if ("OPT-OUT".equalsIgnoreCase(rs.getString(1))) optOut = true;
		}
		
		ps.close();
		
		log.debug("Opted Out: " + optOut);
		return optOut;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<DealerLocationVO> getPendingDealers(boolean isCountry, String org, String attr) {
		String countries = "";
		if (isCountry) countries = "'" + attr.replace(",", "','") + "'";
		
		StringBuilder s = new StringBuilder();
		s.append("select b.location_nm, city_nm, b.country_cd, c.country_nm , ");
		s.append("b.create_dt, b.update_dt, parent_id, dealer_location_id, region_cd ");
		s.append("from dealer a ");
		s.append("inner join dealer_location b on a.dealer_id = b.dealer_id "); 
		s.append("inner join country c on b.country_cd = c.country_cd ");
		s.append("where dealer_type_id = 5 and organization_id = ?  "); 
		s.append("and len(parent_id) > 0 ");
		if (isCountry) s.append("and b.country_cd in (" + countries + ") ");
		s.append("union ");
		s.append("select b.location_nm, city_nm, b.country_cd, c.country_nm, ");
		s.append("b.create_dt, b.update_dt, parent_id, dealer_location_id, region_cd ");
		s.append("from dealer a ");
		s.append("inner join dealer_location b on a.dealer_id = b.dealer_id "); 
		s.append("inner join country c on b.country_cd = c.country_cd ");
		s.append("where dealer_type_id = 5 and organization_id =  ? "); 
		s.append("and active_flg = 0 and region_cd is null ");
		if (isCountry) s.append("and b.country_cd in (" + countries + ") ");
		s.append("order by c.country_nm, city_nm, location_nm");
		//s.append("order by location_nm ");
		log.debug("Clinic Approval SQL: " + s);
		
		PreparedStatement ps = null;
		List<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		int ctr = 1;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(ctr++, org);
			ps.setString(ctr++, org);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new DealerLocationVO(rs));
			}
			
		} catch(SQLException e) {
			log.error("Unable to retrieve pending data", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
	
	
	/**
	 * Builds the emails messages based upon the provided input and sends the email
	 * @param dlid
	 * @param country
	 * @param approved
	 * @param adminText
	 * @param optOut
	 * @throws MailException
	 */
	public void sendEmail(ActionRequest req, boolean approved, boolean optOut) 
	throws MailException, SQLException  {
		// Get the necessary params
		String dlid = StringUtil.checkVal(req.getParameter("parentId"));
		if (dlid.length() == 0) dlid = req.getParameter("dealerLocationId");
		String adminText = req.getParameter("submitterText");
		String[] denyId = req.getParameterValues("denyId");
		String denyReasons = "<ul>";
		if (denyId != null && ! approved) {
			for (int i=0; i < denyId.length; i++) {
				denyReasons += "<li>" + denyId[i].substring(denyId[i].indexOf("|") + 1) + "</li>";
			}
		}
		
		denyReasons += "</ul>";
		
		// Build the HTMl Message
		String html = null;
		if (optOut) {
			html =  this.emailOptOutText(approved, adminText, denyReasons); 
		} else if (StringUtil.checkVal(req.getParameter("parentId")).length() == 0) {
			html = this.newApproveMessage(approved, denyReasons, adminText);
		} else {
			html = this.emailBodytext(approved, adminText, denyReasons);
		}
		
		html += this.getEmailFooter();
		log.debug(html);
		
		// Build the email and send
		SMTMail mail = new SMTMail();
		List<String> clinicAdmin = getAdminEmail(dlid, 20, req);
		String country = StringUtil.checkVal(req.getParameter("country"));
		List<String> countryAdmin =  this.getAdminEmail(country, 70, req);
		log.debug("Country Admin: " + country + "|" + StringUtil.checkVal(country.length()));
		if (clinicAdmin.size() == 0) return;
		List<String> rcpt = clinicAdmin;
		//List<String> rcpt = new ArrayList<String>();
		rcpt.addAll(countryAdmin);
		//rcpt.add("dave@siliconmtn.com");
		//rcpt.add("IChristison@sjm.com");
		if (countryAdmin.size() == 0) countryAdmin.add("cliniclocator@sjm.com");
		
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setPort(Integer.valueOf((String)getAttribute(Constants.CFG_SMTP_PORT)));
		mail.setSmtpServer((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setRecpt((String[])rcpt.toArray(new String[0]));
		mail.setSubject("Clinic Locator Update from St. Jude Medical");
		mail.setFrom(countryAdmin.get(0));
		mail.setReplyTo(countryAdmin.get(0));
		mail.setHtmlBody(html);
		log.debug("Send email to: " + clinicAdmin + "|" + countryAdmin);
		log.debug(html);
		mail.postMail();
	}

	/**
	 * 
	 * @param val
	 * @param encKey
	 * @return
	 * @throws SQLException
	 */
	public List<String> getAdminEmail(String val, int type, ActionRequest req) 
	throws SQLException {
		String encKey = (String) this.getAttribute(Constants.ENCRYPT_KEY);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String siteId = site.getSiteId();
		// if this is a subsite, use parent site's site ID because we need to
		// query against roles that exist at the parent site level.
		if (StringUtil.checkVal(site.getAliasPathParentId()).length() > 0) {
			siteId = site.getAliasPathParentId();
		}
		log.debug("Enc key: " + encKey);
		
		StringBuilder s = new StringBuilder();
		s.append("select * from profile_role a ");
		s.append("inner join profile b on a.profile_id = b.profile_id ");
		s.append("and attrib_txt_1 like ? ");
		s.append("inner join role c on a.role_id = c.role_id ");
		s.append("where site_id = ? and role_order_no = ? ");
		s.append("and status_id = " + SecurityController.STATUS_ACTIVE); 
		log.debug("Getting email addresses SQL: " + s + "|" + val + "|" + type + "|" + site.getSiteId());
		
		List<String> addr = new ArrayList<String>();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, "%" + val + "%");
		ps.setString(2, siteId);
		ps.setInt(3, type);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			try {
				StringEncrypter se = new StringEncrypter(encKey);
				addr.add(se.decrypt(rs.getString("email_address_txt")));
				log.debug("Email Address: " + se.decrypt(rs.getString("email_address_txt")));
			} catch (Exception e) {
				log.error("Unable to decrypt email address", e);
			}
		}
		
		return addr;
	}
	
	/**
	 * 
	 * @param approved
	 * @param adminText
	 * @return
	 */
	public String emailOptOutText(boolean approved, String adminText, String reason) {
		StringBuilder s = new StringBuilder();
		
		if (approved) {
			s.append("You have successfully opted out of the St. Jude Medical ");
			s.append("International Locator program.  If you wish to register ");
			s.append("at a later time, please visit ");
			s.append("<a href='http://www.sjmcliniclocator.com/register'>");
			s.append("www.sjmcliniclocator.com/register</a>.");
			s.append("<br/>");
			s.append("Additional questions/comments about information submitted:");
			s.append("<br/><br/>");
			s.append(StringUtil.checkVal(adminText,"none"));
			s.append("<br/><br/>");
			s.append("You may reply to this message if you have questions about this request.");

		} else {
			s.append("Your request to opt out of the St. Jude Medical ");
			s.append("International Locator program has not been approved for ");
			s.append("the following reason(s): ");
			s.append("<br/><br/>");
			s.append(reason);
			s.append("<br/>");
			s.append("Additional questions/comments about information submitted:");
			s.append("<br/><br/>");
			s.append(StringUtil.checkVal(adminText,"none"));
			s.append("<br/><br/>");
			s.append("You may reply to this message if you have questions ");
			s.append("about this request.");
		}
		
		return s.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public String emailBodytext(boolean approve, String adminText, String reason) {
		StringBuilder s = new StringBuilder();
		if (approve) {
			s.append("Your clinic contact information has been updated either ");
			s.append("according to your request or by your SJM Clinic Locator ");
			s.append("country administrator. Visit <a href='http://www.sjmcliniclocator.com'>");
			s.append("www.sjmcliniclocator.com</a> to review your updated listing.  ");
			s.append("For additional updates, please continue to access the ");
			s.append("clinic locator admin tool at ");
			s.append("<a href='http://www.sjmcliniclocator.com/clinic_admin'>");
			s.append("www.sjmcliniclocator.com/clinic_admin</a>.");
			
			if (StringUtil.checkVal(adminText, null) != null) {
				s.append("<br/>");
				s.append("Additional questions/comments about information submitted:");
				s.append("<br/><br/>");
				s.append(StringUtil.checkVal(adminText));
			}
			
			s.append("<br/><br/>");						
			s.append("You may reply to this message if you have questions ");
			s.append("about this request.");
		} else {
			s.append("Your request to update your clinic information has not ");
			s.append("been approved for the following reason(s):");
			s.append("<br/><br/>");
			s.append(reason);
			s.append("<br/>");
			s.append("Additional questions/comments about information submitted:");
			s.append("<br/><br/>");
			s.append(StringUtil.checkVal(adminText,"none"));
			s.append("<br/><br/>");
			s.append("You may reply to this message if you have questions ");
			s.append("about this request.");
		}
		
		return s.toString();
		
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String newApproveMessage(boolean approve, String reason, String adminText) {
		StringBuilder s = new StringBuilder();
		
		if (approve) {
			s.append("Thank you for registering for the St. Jude Medical International ");
			s.append("Clinic Locator. We are pleased to inform you that your ");
			s.append("request has been approved.");
			s.append("<br/><br/>");
			s.append("To update your clinic listing, visit <a href='http://www.sjmcliniclocator.com/clinic_admin'>");
			s.append("www.sjmcliniclocator.com/clinic_admin</a> and log in with ");
			s.append("your email address and password.");
			s.append("<br/>");
			s.append("Additional questions/comments about information submitted:");
			s.append("<br/><br/>");
			s.append(StringUtil.checkVal(adminText,"none"));
			s.append("<br/><br/>");
			s.append("Feel free to reply to this message if you have any ");
			s.append("questions about your request.");
		} else {
			s.append("Thank you for your interest in the St. Jude Medical ");
			s.append("International Clinic Locator.  Your request has been reviewed ");
			s.append("and at this time you do not meet the program requirements ");
			s.append("for the following reason(s): ");
			s.append("<br/><br/>");
			s.append(reason);
			s.append("<br/>");
			s.append("Additional questions/comments about information submitted:");
			s.append("<br/><br/>");
			s.append(StringUtil.checkVal(adminText,"none"));
			s.append("<br/><br/>");
			s.append("We appreciate your interest in the St. Jude Medical ");
			s.append("International Locator.");
			s.append("<br/><br/>");
			s.append("Feel free to reply to this message if you have any ");
			s.append("questions about your request.");
		}
		
		return s.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public String getEmailFooter() {
		StringBuilder ftr = new StringBuilder();
		ftr.append("<br/><br/>");
		ftr.append("This communication, including any attachments, may contain ");
		ftr.append("information that is proprietary, privileged, confidential or ");
		ftr.append("legally exempt from disclosure. If you are not a named addressee, ");
		ftr.append("you are hereby notified that you are not authorized to read, ");
		ftr.append("print, retain a copy of or disseminate any portion of this ");
		ftr.append("communication without the consent of the sender and that doing ");
		ftr.append("so may be unlawful. If you have received this communication ");
		ftr.append("in error, please immediately notify the sender via return ");
		ftr.append("e-mail and delete it from your system.");
		
		return ftr.toString();
	}
}
