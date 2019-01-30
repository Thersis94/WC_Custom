package com.wsla.action.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.TicketLedgerAction;
import com.wsla.common.LocaleWrapper;
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
		Locale locale = new LocaleWrapper(req).getLocale();
		
		// Summary Reports
		if ("billablePhoneReport".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getByPhoneNumber(startDate, endDate, locale));
			
		} else if ("billableOEMReport".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getByOEM(startDate, endDate, locale));
			
		} 
		
		// Detail Reports
		if ("detail".equalsIgnoreCase(req.getParameter("type"))) {
			String id = req.getParameter("id");
			boolean isPhone = "billablePhoneReport".equalsIgnoreCase(req.getParameter("detailType"));
			setModuleData(getPhoneDetail(startDate, endDate, locale, id, isPhone));
		
		} else if ("soDetail".equalsIgnoreCase(req.getParameter("type"))) {
			TicketLedgerAction tla = new TicketLedgerAction(dbConn, attributes);
			setModuleData(tla.getLedgerForTicket(req.getParameter("ticketIdText")));
		}
	}
	
	/**
	 * 
	 * @param sd
	 * @param ed
	 * @param locale
	 * @param pn
	 * @return
	 */
	public List<BillingVO> getPhoneDetail(Date sd, Date ed, Locale locale, String id, boolean isPhone) {
		StringBuilder sql = new StringBuilder(1024);
		sql.append("select a.ticket_id, ticket_no, a.create_dt as opened_dt, b.create_dt as closed_dt, ");
		sql.append("date_part('day',age(b.create_dt, a.create_dt)) as days_open, total_billable as amount_no, ");
		sql.append("cast(coalesce(date_part('day',age(cas_assigned_dt, a.create_dt)), 0) as int) days_to_cas, ");
		sql.append("cast(coalesce(date_part('day',age(cas_complete_dt, a.create_dt)), 0) as int) as days_in_cas, ? as country, ");
		sql.append("creation_time_no as avg_create_time_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append("inner join ").append(getCustomSchema());
		sql.append("wsla_ticket_ledger b on a.ticket_id = b.ticket_id and b.status_cd = 'CLOSED' ");
		sql.append("inner join ( ");
		sql.append("select ticket_id, coalesce(sum(billable_amt_no), 0.0) as total_billable ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_ledger ");
		sql.append("group by ticket_id ");
		sql.append(") as c on a.ticket_id = c.ticket_id ");
		sql.append("left outer join ( ");
		sql.append("select ticket_id, create_dt as cas_assigned_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("wsla_ticket_ledger where status_cd = 'CAS_ASSIGNED' ");
		sql.append(") as d on a.ticket_id = d.ticket_id ");
		sql.append("left outer join ( ");
		sql.append("select ticket_id, create_dt as cas_complete_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("wsla_ticket_ledger where status_cd = 'CAS_REPAIR_COMPLETE' ");
		sql.append(") as e on a.ticket_id = e.ticket_id ");
		sql.append("where a.status_cd = 'CLOSED' ");
		
		if (isPhone) sql.append("and phone_number_txt = ? ");
		else sql.append("and oem_id = ? ");
		sql.append("and b.create_dt between ? and ? order by b.create_dt ");
				
		List<Object> vals = new ArrayList<>();
		vals.add(locale.getCountry());
		vals.add(id);
		vals.add(sd);
		vals.add(ed);
		log.debug(sql.length() + "|" + sql + vals);
		
		// Query the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new BillingVO(), "ticket_no");
	}
	
	/**
	 * Gets the data for the billing report grouped by phone number
	 * @param sd
	 * @param ed
	 * @return
	 */
	public List<BillingVO> getByPhoneNumber(Date sd, Date ed, Locale locale) {
		
		// Build the sql
		StringBuilder sql = new StringBuilder(640);
		sql.append("select phone_number_txt, count(distinct(a.ticket_id)) as total_tickets, ");
		sql.append("sum(total_billable) as amount_no, ? as country_cd, ");
		sql.append("round(cast(avg(date_part('day',age(b.create_dt, a.create_dt))) as numeric), 1) as avg_days_open_no, ");
		sql.append("avg(creation_time_no)::int avg_create_time_no ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ledger b on a.ticket_id = b.ticket_id and b.status_cd = 'CLOSED' ");
		sql.append("inner join ( ");
		sql.append("select ticket_id, coalesce(sum(billable_amt_no), 0.0) as total_billable ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_ledger ");
		sql.append("group by ticket_id ");
		sql.append(") as c on a.ticket_id = c.ticket_id ");
		sql.append("where 1=1 and b.create_dt between ? and ?  and a.status_cd = 'CLOSED' ");
		sql.append("group by phone_number_txt ");
		sql.append("order by phone_number_txt ");
		log.debug(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(locale.getCountry());
		vals.add(sd);
		vals.add(ed);
		
		// Query the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new BillingVO(), "phone_number_txt");
	}

	/**
	 * Gets the data for the billing report grouped by phone number
	 * @param sd
	 * @param ed
	 * @return
	 */
	public List<BillingVO> getByOEM(Date sd, Date ed, Locale locale) {
		
		// Build the sql
		StringBuilder sql = new StringBuilder(640);
		sql.append("select provider_nm, oem_id, count(distinct(a.ticket_id)) as total_tickets, ");
		sql.append("sum(total_billable) as amount_no, ? as country_cd, ");
		sql.append("round(cast(avg(date_part('day',age(b.create_dt, a.create_dt))) as numeric), 1) as avg_days_open_no, ");
		sql.append("avg(creation_time_no)::int avg_create_time_no ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ledger b on a.ticket_id = b.ticket_id and b.status_cd = 'CLOSED' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider c on a.oem_id = c.provider_id ");
		sql.append("inner join ( ");
		sql.append("select ticket_id, coalesce(sum(billable_amt_no), 0.0) as total_billable ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_ledger ");
		sql.append("group by ticket_id ");
		sql.append(") as d on a.ticket_id = d.ticket_id ");
		sql.append("where 1=1 and b.create_dt between ? and ?  and a.status_cd = 'CLOSED' ");
		sql.append("group by provider_nm, oem_id ");
		sql.append("order by provider_nm ");
		log.debug(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(locale.getCountry());
		vals.add(sd);
		vals.add(ed);
		
		// Query the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new BillingVO(), "provider_nm");
	}
}

