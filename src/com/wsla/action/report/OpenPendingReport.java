package com.wsla.action.report;

// JDK 1.8.x
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
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
		log.debug("oemId: " + oemId);
		oemId = oemId[0].split(",");
		
		try {
			setModuleData(getOpenPendingData(oemId, startDate, endDate));
		} catch (Exception e) {
			log.error("Unable to get pivot", e);
		}
	}
	
	/**
	 * gets the data for the failure report
	 * @param sd
	 * @param ed
	 * @return
	 * @throws SQLException
	 */
	public List<TicketVO> getOpenPendingData(String[] oemId, Date sd, Date ed) {
		List<Object> vals = new ArrayList<>();

		StringBuilder sql = new StringBuilder(2500);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.WHERE_CLAUSE).append("status_cd != 'CLOSED' ");
		sql.append(DATE_WHERE);
		vals.add(sd);
		vals.add(ed);
		if (oemId != null && oemId.length > 0&& !StringUtil.isEmpty(oemId[0])) {
			sql.append("and a.provider_id in ( ? ");
			for(int i = 0; i < oemId.length-1; i++) {
				sql.append(", ?");
				vals.add(oemId[i]);
			}
			sql.append(" ) ");
		}

		log.debug(sql.length() + "|" + sql +"|"+sd+"|"+ed+"|"+oemId);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		
		List<TicketVO> data  = db.executeSelect(sql.toString(), vals, new TicketVO());
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		List<TicketVO> completeTickets = new ArrayList<>();
		
		try {
			for (TicketVO t : data) {
				completeTickets.add(tea.getCompleteTicket(t.getTicketId()));
				log.info("@@@@@ ticket " + t.getTicketId()+"||"+t.getAssignments());
			}
		} catch (DatabaseException | SQLException e) {
			log.error("could not get complete ticket",e);
		}
		
		return completeTickets;
	}
}

