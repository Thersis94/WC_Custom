package com.biomed.smarttrak.admin.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.support.TicketVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SupportReportAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Simple Action responsible for retrieving all support
 * Data for the Report.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 13, 2017
 ****************************************************************************/
public class SupportReportAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public SupportReportAction() {
		super();
	}


	/**
	 * @param arg0
	 */
	public SupportReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	public List<TicketVO> retrieveSupportData(ActionRequest req) throws ActionException {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);
		
		Date startDt = Convert.formatDate(req.getParameter("startDt"));
		if(startDt == null) {
			startDt = c.getTime();
		}
		Date endDt = Convert.formatDate(req.getParameter("endDt"));
		return getTickets(startDt, endDt);
	}


	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<TicketVO> getTickets(Date startDt, Date endDt) {
		String sql = formatDBQuery(startDt, endDt);

		List<Object> params = new ArrayList<>();
		params.add(AdminControllerAction.BIOMED_ORG_ID);
		if(startDt != null) {
			params.add(startDt);
		}
		if(endDt != null) {
			params.add(endDt);
		}

		List<Object> tickets = new DBProcessor(dbConn).executeSelect(sql, params, new TicketVO()); 

		updateProfiles(tickets);

		return (List<TicketVO>)(List<?>)tickets;
	}

	/**
	 * Helper method that decrypts profile data on a Collection of Objects.
	 * @param items
	 */
	protected void updateProfiles(List<Object> tickets) {
		StringEncrypter se;
		try {
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));

			for (Object o : tickets) {
				TicketVO t  = (TicketVO)o;
				t.setFirstName(se.decrypt(t.getFirstName()));
				t.setLastName(se.decrypt(t.getLastName()));
				t.setAssignedFirstNm(se.decrypt(t.getAssignedFirstNm()));
				t.setAssignedLastNm(se.decrypt(t.getAssignedLastNm()));
			}
		} catch (EncryptionException e1) {
			return; //cannot use the decrypter, fail fast
		}
	}

	private String formatDBQuery(Date startDt, Date endDt) {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select a.ticket_id, ac.account_nm as organization_id, ");
		sql.append("d.first_nm as reporter_first_nm, d.last_nm as reporter_last_nm, ");
		sql.append("a.reporter_id, a.status_cd, a.referrer_url, a.create_dt, ");
		sql.append("a.desc_txt, c.first_nm as assigned_first_nm, ");
		sql.append("c.last_nm as assigned_last_nm, sum(b.cost_no) as total_cost_no, ");
		sql.append("sum(b.effort_no) as total_effort_no ");
		sql.append("from support_ticket a ");
		sql.append("left outer join support_activity b on a.ticket_id = b.ticket_id ");
		sql.append("left outer join profile c on a.assigned_id = c.profile_id ");
		sql.append("left outer join profile d on a.reporter_id = d.profile_id ");
		sql.append("left outer join custom.biomedgps_user u on d.profile_id = u.profile_id ");
		sql.append("left outer join custom.biomedgps_account ac on u.account_id = ac.account_id ");
		sql.append("where organization_id = ? ");

		if(startDt != null) {
			sql.append("and a.create_dt > ? ");
		}

		if(endDt != null) {
			sql.append("and a.create_dt < ? ");
		}

		sql.append("group by a.ticket_id, a.reporter_id, a.status_cd, ");
		sql.append("a.referrer_url, a.create_dt, a.desc_txt, c.first_nm, c.last_nm, ");
		sql.append("ac.account_nm, d.first_nm, d.last_nm order by a.create_dt");

		return sql.toString();
	}
}