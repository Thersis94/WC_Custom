package com.orthopediatrics.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// SitebuilderII 2.x
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SalesRepReportAction.java<p/>
 * <b>Description: </b> Summary report of sales rep users
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 29, 2012
 ****************************************************************************/
	public class SalesRepReportAction extends SBActionAdapter {
				
	/**
	 * 
	 */
	public SalesRepReportAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SalesRepReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.region_id, a.region_nm, b.territory_id, b.territory_nm, ");
		sql.append("c.last_nm, c.first_nm, c.email_address_txt, c.phone_number_txt, c.class_id, ");
		sql.append("e.role_nm ");
		sql.append("from ").append(schema).append("op_region a inner join ");
		sql.append(schema).append("op_territory b on a.region_id = b.region_id ");
		sql.append("inner join ").append(schema).append("op_sales_rep c on ");
		sql.append("b.region_id = c.region_id inner join profile_role d on ");
		sql.append("c.profile_role_id = d.profile_role_id inner join role e on ");
		sql.append("d.role_id = e.role_id where 1 = 1	");
		sql.append("and (a.region_id = c.region_id) and (b.territory_id = c.territory_id) ");
		sql.append("order by ");
		String orderBy = StringUtil.checkVal(req.getParameter("orderBy"));
		if (orderBy.length() > 0) {
			if (orderBy.equals("territoryId")) {
				sql.append("b.territory_id, ");
			} else if (orderBy.equals("regionId")) {
				sql.append("a.region_id, ");
			}
		}
		sql.append("last_nm, first_nm");
		log.info("Sales Rep report SQL: " + sql);
		
		List<SalesRepVO> reps = new ArrayList<SalesRepVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				SalesRepVO rep = new SalesRepVO();
				rep.setData(rs);
				reps.add(rep);
			}
		} catch(SQLException sqle) {
			throw new ActionException("Unable to retrieve territory summary report data.", sqle);
		}
		
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		mod.setActionData(reps);
		AbstractSBReportVO rpt = new SalesRepReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName("OPSalesRepUsersReport." + req.getParameter("reportType", "xls"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

}
