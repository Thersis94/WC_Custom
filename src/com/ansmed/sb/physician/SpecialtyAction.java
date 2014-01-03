package com.ansmed.sb.physician;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// Sitebuilder API
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: SpecialtyAction.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 18, 2007
 Last Updated:
 ***************************************************************************/

public class SpecialtyAction extends SBActionAdapter {
	SiteBuilderUtil util = null;
	
	/**
	 * 
	 */
	public SpecialtyAction() {
		util =  new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public SpecialtyAction(ActionInitVO actionInit) {
		util =  new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String msg = null;
		PreparedStatement ps = null;
		if(Convert.formatBoolean(req.getParameter("reassign"))) {
			msg = "You have successfully reassigned the specialty";
			String s = "update " + schema + "ans_surgeon set specialty_id = ? " +
			           "where specialty_id = ?"; 
			
			log.debug("Specialty reassign sql: " + s);
			try {
				ps = dbConn.prepareStatement(s);
				ps.setString(1, req.getParameter("destId"));
				ps.setString(2, req.getParameter("sourceId"));
				ps.executeUpdate();
			} catch (Exception e) {
				msg = "Error reassining specialty information";
				log.error("Error reassigning specialty",e);
			} finally {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		} else if (Convert.formatBoolean(req.getParameter("deleteEle"))) {
			msg = "You have successfully deleted the specialty";
			String s = "delete from " + schema + "ans_specialty where specialty_id = ?";
			log.debug("Specialty delete sql: " + s);
			try {
				ps = dbConn.prepareStatement(s);
				ps.setString(1, req.getParameter("specialtyId"));
				ps.executeUpdate();
			} catch (Exception e) {
				String emsg = e.getMessage();
				if (emsg.indexOf("REFERENCE constraint") > -1) {
					msg = "You must reassign the surgeons using this specialty before it can be deleted";
				} else {
					msg = "Error deleting specialty information";
				}
				log.error("Error deleting specialty",e);
			} finally {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		} else {
			msg = "You have successfully updated the specialty";
			String s = null, id = StringUtil.checkVal(req.getParameter("specialtyId"));
			if (id.length() == 0) {
				s = "insert into " + schema + "ans_specialty (specialty_nm) values (?)";
			} else {
				s = "update " + schema + "ans_specialty set specialty_nm = ? where specialty_id = ?";
			}
			
			log.debug("Specialty Build sql: " + s);
			try {
				ps = dbConn.prepareStatement(s);
				ps.setString(1, req.getParameter("specialtyName"));
				if (id.length() > 0) ps.setString(2, id);
				ps.executeUpdate();
			} catch (Exception e) {
				msg = "Error updating specialty information";
				log.error("Error updating specialty",e);
				e.printStackTrace();
			} finally {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		
		// redirect back to the list of areas
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?msg=").append(msg);

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest arg0) throws ActionException {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer s = new StringBuffer();
		s.append("select * from ").append(schema).append("ans_specialty ");
		s.append("order by specialty_nm");
		
		PreparedStatement ps = null;
		Map<String, String> data = new TreeMap<String, String>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.put(rs.getString("specialty_nm"), rs.getString("specialty_id"));
			}
		} catch(Exception e) {
			log.error("Error retrieving specilties", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
 	}

}
