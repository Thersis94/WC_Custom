package com.wsla.action.report;

// JDK 1.8.x
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <b>Title</b>: OpenPendingReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the open pending Reports
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class OpenPendingReport extends SBActionAdapter {
	/**
	 * Key to use for the report type
	 */
	public static final String AJAX_KEY = "openPending";
	public static final String DATE_WHERE = "and t.create_dt between ? and ? ";

	
	/**
	 * 
	 */
	public OpenPendingReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public OpenPendingReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("open pending report running");
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		String[] oemId = req.getParameterValues("oemId");
		List<String> statusCodes = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("statusCode"))) 
			statusCodes = Arrays.asList(req.getParameter("statusCode").split("\\,"));
		String appTypeCode = req.getParameter("appTypeCode");
		String disTypeCode = req.getParameter("disTypeCode");
		
		oemId = oemId[0].split(",");
		
		try {
			setModuleData(getOpenPendingData(oemId, startDate, endDate, statusCodes, appTypeCode, disTypeCode, new BSTableControlVO(req)));
		} catch (Exception e) {
			log.error("Unable to get pivot", e);
		}
	}
	
	/**
	 * gets the data for the failure report
	 * @param oemId
	 * @param sd
	 * @param ed
	 * @param StatusCode
	 * @param appTypeCode
	 * @param disTypeCode
	 * @param offset 
	 * @param order 
	 * @param limit 
	 * @return
	 */
	public GridDataVO<TicketVO> getOpenPendingData(String[] oemId, Date sd, Date ed, List<String> statusCodes, String appTypeCode, String disTypeCode, BSTableControlVO bstc) {
		
		List<Object> vals = new ArrayList<>();

		StringBuilder sql = new StringBuilder(2500);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_ticket_ref_rep rr on t.ticket_id = rr.ticket_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		
		if(statusCodes == null || statusCodes.isEmpty()) {
			sql.append("and status_cd != 'CLOSED' ");
		}else {
			sql.append(" and status_cd in ( ").append(DBUtil.preparedStatmentQuestion(statusCodes.size())).append(") ");
			vals.addAll(statusCodes);
		}
		
		if(!StringUtil.isEmpty(appTypeCode)) {
			sql.append("and approval_type_cd = ? ");
			vals.add(appTypeCode);
		}
		
		if(!StringUtil.isEmpty(disTypeCode)) {
			sql.append("and unit_disposition_cd = ? ");
			vals.add(disTypeCode);
		}
		
		sql.append(DATE_WHERE);
		
		vals.add(sd);
		vals.add(ed);
		if (oemId != null && oemId.length > 0&& !StringUtil.isEmpty(oemId[0])) {
			sql.append("and t.oem_id in ( ? ");
			int i =0;
			for( ;i < oemId.length-1; i++) {
				sql.append(", ?");
				vals.add(oemId[i]);
			}
			//add the tailing variable
			vals.add(oemId[i]);
			sql.append(" ) ");
		}

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		
		GridDataVO<TicketVO> data  = db.executeSQLWithCount(sql.toString(), vals, new TicketVO(), bstc);
		
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		List<TicketVO> completeTickets = new ArrayList<>();
		
		try {
			for (TicketVO t : data.getRowData()) {
				completeTickets.add(tea.getCompleteTicket(t.getTicketId()));
			}
		} catch (DatabaseException | SQLException e) {
			log.error("could not get complete ticket",e);
		}
		
		data.setRowData(completeTickets);
		return data;
	}
}

