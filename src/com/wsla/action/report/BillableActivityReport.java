package com.wsla.action.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.report.BillingVO;

/****************************************************************************
 * <b>Title</b>: BillableActivityReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Retrieves the data for the billable activity report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 25, 2019
 * @updates:
 ****************************************************************************/

public class BillableActivityReport extends SBActionAdapter {

	public static final String AJAX_KEY = "billing";
	
	/**
	 * 
	 */
	public BillableActivityReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BillableActivityReport(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		
		if (req.hasParameter("phone")) {
			setModuleData(getByPhoneNumber(startDate, endDate));
		}
	}
	
	/**
	 * Gets the data for the billing report grouped by phone number
	 * @param sd
	 * @param ed
	 * @return
	 */
	public List<BillingVO> getByPhoneNumber(Date sd, Date ed) {
		// Build the sql
		StringBuilder sql = new StringBuilder(352);
		sql.append("select phone_number_txt, count(distinct(a.ticket_id)) as total_tickets, ");
		sql.append("sum(billable_amt_no) as amount_no, 'MX' as country_cd ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ledger b on a.ticket_id = b.ticket_id and b.status_cd = 'CLOSED' ");
		sql.append("where 1=1 and b.create_dt between ? and ? ");
		sql.append("group by phone_number_txt ");
		sql.append("order by phone_number_txt ");
		log.debug(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(sd);
		vals.add(ed);
		
		// Query the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new BillingVO(), "phone_number_txt");
	}

}

