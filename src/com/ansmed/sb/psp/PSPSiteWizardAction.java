package com.ansmed.sb.psp;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ansmed.sb.psp.pages.SBModuleManager;
import com.ansmed.sb.psp.pages.SBWizardVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>:PSPSiteWizardAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 29, 2008
 * <b>Changes: DBargerhuff - customized for PSP site migration.</b>
 ****************************************************************************/
public class PSPSiteWizardAction {
	
	//public static final String SITE_ALIAS_STUB = ".sb.siliconmtn.com";
	public static final String SITE_ALIAS_STUB = ".sb.whiterabbit.com";
	
	private static Logger log = Logger.getLogger(PSPSiteWizardAction.class);
	private Connection mySQLConn = null;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "jdbc:mysql://localhost/ansdoctors?";
	private String dbUser = "root";
	private String dbPwd = "d0ntf0rget";
	private List<Integer> sites = null;
	private PspSiteVO psv = null;
	private Map<String, List<PSPContent>> pcv = null;
	private String siteCss = null;

	/**
	 * 
	 */
	public PSPSiteWizardAction() throws Exception {
		PropertyConfigurator.configure("scripts/pspWizard_log4j.properties");
		getMySQLConnection();
		sites = new ArrayList<Integer>();
		psv = new PspSiteVO();
		pcv = new HashMap<String,List<PSPContent>>();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SBModuleManager mm = null;
		//try to instantiate the module manager and obtain a SQL connection. 
		try {
			mm = new SBModuleManager("dave@siliconmtn.com","d0ntf0rget");
		} catch (IOException ioe) {
			log.error("Failed login. ", ioe);
			System.exit(-1);
		}
		
		//try to instantiate the psp wiz and obtain a MySQL connection.
		PSPSiteWizardAction pwa = null;
		try {
			pwa = new PSPSiteWizardAction();
		} catch(Exception e) {
			log.error("Could not obtain mySQL connection...Exiting...",e);
			try {
				mm.close();
			} catch (Exception e1) {
				log.error("Could not close SQL db connection...Exiting anyway...", e1);
			}
			System.exit(-1);
		}
		
		//get a mySQL connection and set up preliminary configuration
		PspSiteAction psa = new PspSiteAction(pwa.mySQLConn);
		
		//retrieve site list
		pwa.setSites(psa.retrieveMigrationList());
		
		// ******* BEGIN DEBUG BLOCK ********** 
		int siteVal = 261;		
		pwa.retrieveSite(siteVal);
		// ******* END DEBUG BLOCK ****** 
		
		// PROD - loop sites and process each
		//for (Integer siteId : psa.retrieveSiteList()) {
			//retrieve site
			//pwa.retrieveSite(siteId);
		//
			PspSiteVO sv = pwa.getSiteData();
			
			System.out.println("Org: " + sv.getSbOrgId());
			System.out.println("Site/Name: " + sv.getSbSiteId() + "/" + sv.getSbSiteName());
			System.out.println("Template/Type: " + sv.getTemplate().getThemeName() +
					"/" + sv.getTemplate().getProfileTypeFlg());
			
			//Create map of site page and content that goes on page.
			Map<String,List<PSPContent>> cm = pwa.getSiteContent();
			
			// more DEBUG stuff
			/*
			int cnt = 0;
			for(String s : cm.keySet()) {
				++cnt;
				System.out.println.("Key " + cnt + ": " + s);
				for (PSPContent p : cm.get(s)) {
					System.out.print("Name/Column: " + p.getContentName() + "/" + p.getColumn());
				}
				System.out.println("");
			}
			*/
			//Retrieve/remove the CSS content from the map for later use.  We don't 
			//want it submitted as page content for association.
			String cssTxt = cm.get("css").get(0).getContentText();
			System.out.println(cssTxt);
			cm.remove("css");
			
			
			//set the vo values we need
			SBWizardVO wiz = new SBWizardVO();	
			wiz = pwa.populateWizardVO(sv);
			
			// *************** TESTING ONLY **********************
			// OVERRIDE theme for testing purposes
			//wiz.setThemeId("ARCH_BLUE");
			// ***************************************************
			System.out.println("Checking aliases...");
			System.out.println("alias list size: " + sv.getSiteAliases().size());
			if (sv.getSiteAliases().size() > 0) {
				for(String s:sv.getSiteAliases()) {
					System.out.println("alias: " + s);
				}
			}
			
			//call the wizard to create org/site
			try {
				mm.callWizard(wiz);
			} catch(Exception e) {
				log.error("Error calling wizard...",e);
			}
			
			//associate content
			try {
				mm.associateContent(wiz.getOrganizationId(), pwa.getSiteContent());
			} catch(Exception e) {
				log.error("Error associating content...",e);
			}
			
			// create CSS using the module manager to call the admintool
			try {
				mm.callCSSWizard(sv.getSbOrgId(), sv.getSbSiteId(), cssTxt);
			} catch(Exception e) {
				log.error("Error creating CSS content...",e);
			}
			
			// create site aliases using the module manager to call the admintool
			try {
				mm.callSiteAliasWizard(sv.getSbSiteId(), sv.getSiteAliases());
			} catch(Exception e) {
				log.error("Error creating site aliases...",e);
			}
			
			//create org admin user entries
			try {
				mm.callOrgAdminUserWizard(sv.getSbOrgId());
			} catch(Exception e) {
				log.error("Error creating org admin user entries for this org...",e);
			}
			
		//
		// } // end of PROD for-loop...
		//
		
		
				
		//cleanup db connections
		try {
			pwa.close();
		} catch(Exception e) {
			log.error("Could not close mySQL connection...",e);
		}
		
		try {
			mm.close();
		} catch(Exception e) {
			log.error("Could not close module manager db connection...",e);
		}

	}
	
	/**
	 * Retrieves site data and content
	 * @param practiceId
	 */
	public void retrieveSite(int practiceId) {
		
		//get a mySQL db connection
		PspSiteAction psa = new PspSiteAction(mySQLConn);
		
		//retrieve the site data
        psv = psa.retrieve(practiceId);
        
        //get the page content, base styles, and image content if applicable.
        PspContentAction pca = new PspContentAction();
        pcv = pca.retrieve(psv);
	}
	
	/**
	 * @return the psv
	 */
	public PspSiteVO getSiteData() {
		return psv;
	}

	/**
	 * @param psv the psv to set
	 */
	public void setPsv(PspSiteVO psv) {
		this.psv = psv;
	}

	/**
	 * @return the pcv
	 */
	public Map<String,List<PSPContent>> getSiteContent() {
		return pcv;
	}

	/**
	 * @param pcv the pcv to set
	 */
	public void setPcv(Map<String,List<PSPContent>> pcv) {
		this.pcv = pcv;
	}
	
	/**
	 * @return the sites
	 */
	public List<Integer> getSites() {
		return sites;
	}

	/**
	 * @param sites the sites to set
	 */
	public void setSites(List<Integer> sites) {
		this.sites = sites;
	}

	/**
	 * @return the siteCss
	 */
	public String getSiteCss() {
		return siteCss;
	}

	/**
	 * @param siteCss the siteCss to set
	 */
	public void setSiteCss(String siteCss) {
		this.siteCss = siteCss;
	}

	private void getMySQLConnection() throws Exception {
		// Get a database connection.
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPwd);
		this.mySQLConn = dbc.getConnection();
	}
	
	private void close() throws Exception {
		try {
			mySQLConn.close();
		} catch(Exception e) {
			throw new Exception("Error closing mySQL connection.", e);
		}
	}
	
	/**
	 * Populates the SBWizardVO with values for a given site that is to be created
	 * @param p
	 * @return
	 */
	private SBWizardVO populateWizardVO(PspSiteVO p) {
		SBWizardVO w = new SBWizardVO();
		w.setOrganizationId(p.getSbOrgId());
		w.setOrganizationName(p.getSbSiteName());
		w.setSiteId(p.getSbSiteId());
		w.setSiteName(p.getSbSiteName());
		w.setThemeId(p.getTemplate().getThemeName());
		//setting the primary URL to be an "sb.siliconmtn.com" url
		String siteAlias = p.getSbSiteId().replace("SJM_PSP_", "psp");
		w.setSiteAliasUrl(siteAlias + SITE_ALIAS_STUB);
		w.setAdminName("Liza Howard");
		w.setAdminEmail("liza.howard@sjmneuro.com");
		w.setOverrideSiteId("true");
		
		//set main email
		String mainEmail = StringUtil.checkVal(p.getEmail());
		//if there's no address, try to get a contact form address
		if (mainEmail.length() == 0) {
			//if there's not contact form address, use the admin email address
			if (StringUtil.checkVal(p.getContactFormEmail()).length() == 0) {
				mainEmail = w.getAdminEmail();
			} else {
				mainEmail = StringUtil.checkVal(p.getContactFormEmail());
			}
		}
		w.setMainEmail(mainEmail);
		
		w.setCopyright("Copyright " + new Integer(2009).toString());
		//w.setCPage("");
		//w.setDefaultFlag("");
		//w.setDefaultLocationFlag("");
		w.setNumberColumns(new Integer(p.getTemplate().getThemeColumns()).toString());
		w.setDefaultColumn(new Integer(p.getTemplate().getContColNo()).toString());

		return w;
	}
	
}
