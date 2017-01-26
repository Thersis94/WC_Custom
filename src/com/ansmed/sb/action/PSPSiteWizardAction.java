package com.ansmed.sb.action;

// SMT Base Libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.ContentAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;

/****************************************************************************
 * <b>Title</b>:PSPSiteWizardAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 29, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class PSPSiteWizardAction extends SBActionAdapter {

	/**
	 * 
	 */
	public PSPSiteWizardAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public PSPSiteWizardAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Calling Custom SJM Code for the site wizard");
		
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);		
		this.createPspContent(req, mod);
        mod.setActionId("SITE_WIZARD");
        log.debug("Completed Custom SJM Wizard Processing");
	}
	
	/**
	 * Creates additional content portlets
	 * @param req
	 * @param mod
	 * @throws ActionException
	 */
	private void createPspContent(ActionRequest req, ModuleVO mod)
	throws ActionException {
		
		// create the 'address' content'
		log.debug("Setting 'Address' content request parameters");
		mod.setActionId("CONTENT");
		String articleText = this.retrievePhysicianAddress(req);
		log.debug("articleText: " + articleText);
		req.setParameter("articleText", articleText);
		req.setParameter("actionName", "Address");
		req.setParameter("actionDesc", "Address");
		req.setParameter(SB_ACTION_ID, null);
			
        // Call the action
        ActionInterface sai = new ContentAction(this.actionInit);
        sai.setDBConnection(dbConn);
        sai.setAttributes(attributes);
        sai.update(req);
        
        // update the 'Privacy' content with the custom disclaimer
		log.debug("Setting 'Privacy' content request parameters");
		mod.setActionId("CONTENT");
		articleText = this.retrieveDisclaimer(req);
		//log.debug("articleText: " + articleText);
		req.setParameter("articleText", articleText);
		req.setParameter("actionName", "Privacy");
		req.setParameter("actionDesc", "Privacy");		
		
		// set the action id
        String contentId = this.retrieveDisclaimerContentActionId(req, "Privacy");
        if (contentId.length() > 0) {
        	req.setParameter(SB_ACTION_ID, contentId);
        }
        else {
        	req.setParameter(SB_ACTION_ID, null);
        }
		
        // Call the action
        sai = new ContentAction(this.actionInit);
        sai.setDBConnection(dbConn);
        sai.setAttributes(attributes);
        sai.update(req);
        
        // create logo content
		log.debug("Setting 'Site logo' content request parameters");
		mod.setActionId("CONTENT");
		articleText = "Site logo";
		log.debug("articleText: " + articleText);
		req.setParameter("articleText", articleText);
		req.setParameter("actionName", "Site logo");
		req.setParameter("actionDesc", "Site logo");
		req.setParameter(SB_ACTION_ID, null);
			
        // Call the action
        sai = new ContentAction(this.actionInit);
        sai.setDBConnection(dbConn);
        sai.setAttributes(attributes);
        sai.update(req);
		
	}
	
	/**
	 * Builds the physician's address
	 * @param req
	 * @return
	 */
	private String retrievePhysicianAddress(ActionRequest req) {
		// Format the content
		StringBuilder sb = new StringBuilder();
		sb.append("<table><tr><td>").append(req.getParameter("locationDesc")).append("</td></tr>");
		sb.append("<tr><td>").append(req.getParameter("address")).append("</td></tr>");
		sb.append("<tr><td>").append(req.getParameter("city")).append(", ");
		sb.append(req.getParameter("state")).append("&nbsp;");
		sb.append(req.getParameter("zipCode")).append("</td></tr>");
		sb.append("<tr><td>Phone: ").append(req.getParameter("mainPhone")).append("</td></tr>");
		sb.append("<tr><td>Fax: ").append(req.getParameter("fax")).append("</td></tr>");
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * Builds and returns the standard disclaimer/privacy statement for a PSP site.
	 * @param siteName
	 * @return
	 */
	private String retrieveDisclaimer(ActionRequest req) {
		
		// organizationName is site name
		String siteName = (StringUtil.checkVal(req.getParameter("organizationName"))).toUpperCase();
		
		StringBuffer sb = new StringBuffer();
		sb.append("<div id=\"pspDisclaimerContent\"><p>").append(siteName);
		sb.append(" DISCLAIMER STATEMENT</p>");
		sb.append("<p>1. <strong>No Medical Information</strong>. Information on the ");
		sb.append("website is provided to further general consumer understanding, ");
		sb.append("awareness and knowledge of pain. All information provided on this ");
		sb.append("website is for general information purposes only. The information ");
		sb.append("provided by the website is not intended nor implied to be a ");
		sb.append("substitute for professional medical advice from health care providers.</p>");
		sb.append("<p>2. Incomplete/Inaccurate Information. The information available ");
		sb.append("on this website should not be considered complete or exhaustive ");
		sb.append("and may not reflect the most current information on pain. While ");
		sb.append(siteName).append(" attempts to make sure the information included in this ");
		sb.append("website is accurate, there may be delays, omissions, or inaccuracies ");
		sb.append("in the content provided on this website. As a result, ").append(siteName);
		sb.append(" does not represent that the information contained herein is complete, ");
		sb.append("accurate and up to date in every case.</p>");
		sb.append("<p>3. No Warranty. The information contained in this website and in ");
		sb.append("any other website accessed via links from this website is provided ");
		sb.append("as is or as available without representation or warranty of any kind, ");
		sb.append("express or implied. ALL SUCH REPRESENTATIONS AND WARRANTIES ");
		sb.append("ARE HEREBY DISCLAIMED, INCLUDING, WITHOUT LIMITATION, THE IMPLIED ");
		sb.append("WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. ");
		sb.append(siteName).append(" does not warrant or represent the timeliness, content, ");
		sb.append("sequence, accuracy, or completeness of any information or data ");
		sb.append("furnished hereunder.</p>");
		sb.append("<p>4. Liability Limitation. In no event shall ").append(siteName);
		sb.append(" be liable for any direct, indirect, incidental, special, exemplary, punitive, or ");
		sb.append("any other monetary or other damages, fees, fines, penalties or liabilities ");
		sb.append("arising out of or relating in any way to use of this website or websites ");
		sb.append("accessed through links and/or information contained in this website. ");
		sb.append("In no event shall ").append(siteName).append(" be liable for any lost profits, lost data or ");
		sb.append("business arising out of or relating in any way to use of this website ");
		sb.append("or websites accessed through such links and/or information.</p>");
		sb.append("<p>5. No Guarantee Against Errors, Delays, Losses. ");
		sb.append(siteName).append(" cannot guarantee and does not warrant against ");
		sb.append("human and/or machine errors, omissions, delays, interruptions or losses of information or ");
		sb.append("data, infringing material, or defamation. ").append(siteName).append(" cannot and does ");
		sb.append("not guarantee or warrant that files available for downloading on this ");
		sb.append("website will be free of infection or viruses, worms, Trojan horses or ");
		sb.append("other codes that manifest contaminating or destructive properties.</p>");
		sb.append("<p>6. No Guarantee About Linked Websites. ").append(siteName);
		sb.append(" does not endorse and is not responsible or liable for the ");
		sb.append("materials contained in or through, or products available through the ");
		sb.append("materials contained in other websites accessed via links from this ");
		sb.append("website. Linking to such websites is done at the user&#39;s own risk. ");
		sb.append(siteName).append(" cannot guarantee and does not warrant the availability of such ");
		sb.append("linked websites. ").append(siteName).append(" cannot guarantee and does not assume ");
		sb.append("responsibility for any damage incurred from accessing such linked ");
		sb.append("websites nor from use, browsing or downloading information from ");
		sb.append("such linked websites.</p>");
		sb.append("<p>The information we learn from customers helps us personalize and ");
		sb.append("continually improve your experience at our website. Here are the types ");
		sb.append("of information we gather. Information You Give Us: We receive and store ");
		sb.append("any information you enter on our Web site or give us in any other way. ");
		sb.append("We do not sell or rent your personal information to others without your ");
		sb.append("consent. We use the information we collect only for the purposes of ");
		sb.append("sending promotional information, enhancing the operation of our site, ");
		sb.append("serving advertisements, for statistical purposes and to administer ");
		sb.append("our systems. We DO NOT use third parties to provide customer service, ");
		sb.append("to serve site content, to serve the advertisements you see on our site, ");
		sb.append("to conduct surveys, to help administer promotional emails, or to ");
		sb.append("administer drawings or contests, but reserve the right to do so in the ");
		sb.append("future without advance notice. Our computer system protects personal ");
		sb.append("information using advanced firewall technology. Information from ");
		sb.append("Other Sources: For reasons such as improving personalization of our ");
		sb.append("service, we might receive information about you from other sources ");
		sb.append("and add it to our account information.</p></div>");

		return sb.toString();
	}
	
	/**
	 * Retrieves action id for 
	 * @param req
	 * @return
	 */
	private String retrieveDisclaimerContentActionId(ActionRequest req, String actionName) {
		
		String id = null;
		
		// retrieve the 'Privacy' content to update
		StringBuffer sql = new StringBuffer();
		sql.append("select action_id from sb_action ");
		sql.append("where module_type_id = 'CONTENT' ");
		sql.append("and organization_id = ? ");
		sql.append("and action_nm = ? ");
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, StringUtil.checkVal(req.getParameter("organizationId")));
			ps.setString(2, actionName);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				id = rs.getString(1);
			}
			
		} catch (SQLException sqle) {
			log.error("Error retrieving 'Privacy' content for org... ",sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
				
			}
		}
		
		return id;
	}
}
