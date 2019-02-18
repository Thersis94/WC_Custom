package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.biomed.smarttrak.action.BiomedSiteSearchAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class AdminSiteSearchAction extends BiomedSiteSearchAction {
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		prepAttributeIds(req);
		req.setParameter("adminSearch", "true");
		super.retrieve(req);
	}
	

	/**
	 * Load the site search action's attribute ids so that the correct solr widgets
	 * can be loaded in the site search action.
	 * @param req
	 * @throws ActionException
	 */
	private void prepAttributeIds(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String actionId = mod.getIntroText();
		String sql = "select attrib1_txt, attrib2_txt from sb_action where action_id = ?";
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, actionId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				mod.setAttribute(ModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
				mod.setAttribute(ModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
				setAttribute(Constants.MODULE_DATA, mod);
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	

}
