package com.ansmed.sb.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:SummaryReportAction.java<p/>
 * <b>Description: </b> Summary report of physicians by region and area
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 13, 2008
 ****************************************************************************/
public class SummaryReportAction extends SBActionAdapter {

	/**
	 * 
	 */
	public SummaryReportAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SummaryReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		
		StringBuffer sql = new StringBuffer();
		sql.append("select type_nm, region_nm, area_nm, count(*) total ");
		sql.append("from ").append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_surgeon_type b ");
		sql.append("on a.surgeon_type_id = b.surgeon_type_id ");
		sql.append("inner join ").append(schema).append("ans_sales_rep c ");
		sql.append("on a.sales_rep_id = c.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region d ");
		sql.append("on c.region_id = d.region_id ");
		sql.append("inner join ").append(schema).append("ans_sales_area e ");
		sql.append("on d.area_id = e.area_id ");
		sql.append("where area_nm not like '%Corp%' and status_id < 10 ");
		sql.append("group by area_nm, region_nm, type_nm ");
		sql.append("order by area_nm, region_nm, type_nm ");
		log.info("Summary SQL: " + sql);
		
		PreparedStatement ps = null;
		SummaryVO data = new SummaryVO();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.setData(rs);
			}
		} catch(Exception e) {
			throw new ActionException("Unable to generate summary report", e);
		}
		
		// Add the collection for viewing
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
	}

}
