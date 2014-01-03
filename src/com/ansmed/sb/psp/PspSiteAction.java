package com.ansmed.sb.psp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

import com.smt.sitebuilder.action.SBActionAdapter;


/****************************************************************************
 * <b>Title</b>: PspSiteAction.java<p/>
 * <b>Description</b>: Retrieves information about a PSP site from the db and
 * populates the PspSiteVO with that information..
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 16, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspSiteAction extends SBActionAdapter {
	
	public enum DataType {
		CONTACT,DOCUMENTS,IMAGES,LINKS,PROFILES,SERVICES,SITE,TEMPLATE,ALIAS
		};
	
	public static final String PSP_SITE_DATA = "pspSiteData";
	private Connection mySQLConn = null;
	
	public PspSiteAction() {
		super();
	}
	
	public PspSiteAction(ActionInitVO aiv) {
		super(aiv);
	}
	
	public PspSiteAction(Connection conn) {
		this();
		this.mySQLConn = conn;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		//assumption is that we have the site data now.
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		int practiceId = Convert.formatInteger(req.getParameter("practiceId"));
		//get the site data
		PspSiteVO psv = retrieveSiteData(practiceId);
		
		req.setAttribute(PSP_SITE_DATA, psv);
	}
	
	/**
	 * Overidden retrieve method to receive practice ID as a String value.
	 * @param practiceId
	 */
	public PspSiteVO retrieve(int practiceId) {
		PspSiteVO psv = retrieveSiteData(practiceId);
		return psv;
	}
	
	public PspSiteVO retrieveSiteData(int practiceId) {
		
		PspSiteVO psv = new PspSiteVO();
		String sql = "";
		PreparedStatement ps;
		//loop data types and populate the vo with data for each type.
		for(DataType d : DataType.values()) {
			
			switch(d) {
				case CONTACT:
					sql = PspSiteQueries.retrieveContactSql();
					break;
				case DOCUMENTS:
					sql = PspSiteQueries.retrieveDocumentsSql();
					break;
				case IMAGES:
					sql = PspSiteQueries.retrieveImagesSql();
					break;
				case LINKS:
					sql = PspSiteQueries.retrieveLinksSql();
					break;
				case PROFILES:
					sql = PspSiteQueries.retrieveProfileSql();
					break;
				case SERVICES:
					sql = PspSiteQueries.retrieveServicesSql();
					break;
				case SITE:
					sql = PspSiteQueries.retrieveSiteDataSql();
					break;
				case TEMPLATE:
					sql = PspSiteQueries.retrieveTemplateSql();
					break;
				case ALIAS:
					sql = PspSiteQueries.retrieveAliasesSql();
					break;
				default:
					sql = "";
					continue;
			}
			//System.out.println(d.name() + " SQL: " + sql + " | " + practiceId);
			ps = null;
			try {
				ps = mySQLConn.prepareStatement(sql);
				ps.setInt(1,practiceId);
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					psv.setData(rs,d.name());
				}
			} catch(SQLException sqle) {
				log.error("Error retrieving " + d.name() + "data for vo. " + sqle);
			}
			
		}		
		
		return psv;
	}
	
	/**
	 * Retrieves all approved site ids
	 * @return
	 */
	public List<Integer> retrieveMigrationList() {
		List<Integer> sites = new ArrayList<Integer>();
		String sql = PspSiteQueries.retrieveSitesSql();
		PreparedStatement ps;
		ps = null;
		try {
			ps = mySQLConn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				sites.add(rs.getInt(1));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving list sites for migration...", sqle);
		}
	
		return sites;
	}

}
