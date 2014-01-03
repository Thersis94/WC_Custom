/**
 * 
 */
package com.fastsigns.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.sql.ResultSet;

import javax.servlet.http.HttpServletResponse;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FranchiseReportAction.java<p/>
 * <b>Description: A simple Franchise report that shows the following:
 * 		WebId | Designator Name | Lat | Long | Address | City | State | Zip | 
 * 		Country | Locator Status | Store Hours</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 29, 2011
 ****************************************************************************/
public class FranchiseReportAction extends SimpleActionAdapter {

	
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("building FTS Franchise Report");
		Boolean activeOnly = Convert.formatBoolean(req.getParameter("activeOnly"));
		String countryCd = StringUtil.checkVal(req.getParameter("countryCd"));
		
		StringBuilder sql = new StringBuilder();
		sql.append("select * from DEALER a ");
		sql.append("inner join DEALER_LOCATION b on a.DEALER_ID=b.DEALER_ID ");
		sql.append("inner join COUNTRY c on b.COUNTRY_CD=c.COUNTRY_CD ");
		sql.append("where a.ORGANIZATION_ID=? ");
		if (activeOnly) sql.append(" and b.active_flg=? ");
		if (countryCd.length() > 0) sql.append("and b.country_cd=? ");
		sql.append("order by ").append(req.getParameter("order"));
		log.debug(sql);

		List<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, req.getParameter("organizationId"));
			if (activeOnly) ps.setInt(++i, 1);
			if (countryCd.length() > 0) ps.setString(++i, countryCd);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				data.add(new DealerLocationVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("loaded " + data.size() + " franchise records");
		this.putModuleData(data, data.size(), true);
		
		// Set the file name
		if ("excel".equalsIgnoreCase(req.getParameter("reportType"))) {
			HttpServletResponse response = (HttpServletResponse)getAttribute(GlobalConfig.HTTP_RESPONSE);
			response.setContentType("application/vnd.ms-excel");
			req.setParameter(AdminConstants.REDIRECT_EC_REPORT_NAME, "Franchise Report.xls"); //gets printed in data_tool.jsp
		}

		req.setAttribute(Constants.REDIRECT_DATATOOL, Boolean.TRUE);
	}
}
