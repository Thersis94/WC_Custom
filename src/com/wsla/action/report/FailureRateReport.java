package com.wsla.action.report;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.FailureReportVO;

/****************************************************************************
 * <b>Title</b>: FailureRateReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the failure Reports
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class FailureRateReport extends SBActionAdapter {
	/**
	 * Key to use for the report type
	 */
	public static final String AJAX_KEY = "failure";
	public static final String LEFT_OUTER_JOIN_PARA = "left outer join ( ";
	public static final String DATE_WHERE = "and t.create_dt between ? and ? ";
	public static final String WHERE_CLOSED = "where t.status_cd = 'CLOSED' ";
	
	/**
	 * 
	 */
	public FailureRateReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FailureRateReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("failure report running");
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		String[] oemId = req.getParameterValues("oemId");
		oemId = oemId[0].split(",");
		
		try {
			setModuleData(getfailureData(oemId, startDate, endDate));
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
	public List<FailureReportVO> getfailureData(String[] oemId, Date sd, Date ed) 
	throws SQLException {
		List<FailureReportVO> data = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(2500);
		sql.append("select a.cust_product_id, b.provider_nm, a.product_nm, repair.total as repair_no, refund.total as refund_no, replacement.total as replace_no, CAS_firmware.total as cas_config_no, units.count_no as count_no, periodRepair.total as periodRepair_no "); 
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_product_master a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider b on a.provider_id = b.provider_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append(joinToTicketSql());
		sql.append(WHERE_CLOSED);
		sql.append("group by pm.product_id ) as repair  on repair.product_id = a.product_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append(joinToTicketSql());
		sql.append(WHERE_CLOSED);
		sql.append(DATE_WHERE);
		sql.append("group by pm.product_id ) as periodRepair  on periodRepair.product_id = a.product_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append(joinToTicketSql());
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_ref_rep rr on rr.ticket_id = t.ticket_id ");
		sql.append(WHERE_CLOSED).append(" and rr.approval_type_cd = 'REFUND_REQUEST' ");
		sql.append(DATE_WHERE);
		sql.append("group by pm.product_id ) as refund on refund.product_id = a.product_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append(joinToTicketSql());
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_ref_rep rr on rr.ticket_id = t.ticket_id ");
		sql.append(WHERE_CLOSED).append(" and rr.approval_type_cd = 'REPLACEMENT_REQUEST' ");
		sql.append(DATE_WHERE);
		sql.append("group by pm.product_id )as replacement on replacement.product_id = a.product_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append("select pm.product_id, count(*) as total from ").append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_data td on t.ticket_id = td.ticket_id and value_txt = 'repair-200' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment b on t.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_serial ps on t.product_serial_id = ps.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_master pm on ps.product_id = pm.product_id ");
		sql.append(WHERE_CLOSED);
		sql.append(DATE_WHERE);
		sql.append("group by pm.product_id )as CAS_firmware on CAS_firmware.product_id = a.product_id ");
		sql.append(LEFT_OUTER_JOIN_PARA);
		sql.append("select pm.product_id, coalesce( serial_no_count_no.count_no, serial_count_no,  0) as count_no from ").append(getCustomSchema()).append("wsla_product_master pm ");
		sql.append("left outer join ( select product_id, count(*) as count_no from ").append(getCustomSchema()).append("wsla_product_serial ");
		sql.append("group by product_id ");
		sql.append(") as serial_no_count_no on  serial_no_count_no.product_id = pm.product_id ");
		sql.append(")as units on units.product_id = a.product_id ");
		sql.append("where (refund.total is not null or repair.total is not null or replacement.total is not null) ");
		if (oemId != null && oemId.length > 0&& !StringUtil.isEmpty(oemId[0])) {
			sql.append("and a.provider_id in ( ");
			sql.append(DBUtil.preparedStatmentQuestion(oemId.length));
			sql.append(" ) ");
			
		}

		log.debug(sql.length() + "|" + sql);
		
		int ctr = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
 			ps.setTimestamp(ctr++, Convert.formatTimestamp(sd));
			ps.setTimestamp(ctr++, Convert.formatTimestamp(ed));
 			ps.setTimestamp(ctr++, Convert.formatTimestamp(sd));
			ps.setTimestamp(ctr++, Convert.formatTimestamp(ed));
 			ps.setTimestamp(ctr++, Convert.formatTimestamp(sd));
			ps.setTimestamp(ctr++, Convert.formatTimestamp(ed));
 			ps.setTimestamp(ctr++, Convert.formatTimestamp(sd));
			ps.setTimestamp(ctr++, Convert.formatTimestamp(ed));
			if(oemId != null)log.debug("oem id length "+ oemId.length );
			
			if (oemId != null && oemId.length > 0 && !StringUtil.isEmpty(oemId[0])) {
				for(String s :oemId) {
					log.debug("oem id " + oemId[0] );
					ps.setString(ctr++, s);
				}
			}
				

			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					FailureReportVO frvo = new FailureReportVO();
					frvo.setCustProductId(rs.getString("cust_product_id"));
					frvo.setProviderName(rs.getString("provider_nm"));
					frvo.setAllFailures(rs.getInt("repair_no"));
					frvo.setReplaceNumber(rs.getInt("replace_no"));
					frvo.setProductName(rs.getString("product_nm"));
					frvo.setCasConfigNumber(rs.getInt("cas_config_no"));
					frvo.setRefundNumber(rs.getInt("refund_no"));
					frvo.setTotalUnits(rs.getInt("count_no"));
					frvo.setPeriodFailure(rs.getInt("periodRepair_no"));
					int repairs = rs.getInt("periodRepair_no") - rs.getInt("refund_no") - rs.getInt("replace_no") -rs.getInt("cas_config_no");
					
					frvo.setRepairNumber(repairs);
					frvo.calculateRate();
					data.add(frvo);
				}
			}
		}
		
		return data;
	}



	/**
	 * returns the regularly used section of the left outer join
	 * @return
	 */
	private String joinToTicketSql() {
		StringBuilder sb = new StringBuilder(328);
		sb.append("select pm.product_id, count(*) as total from ").append(getCustomSchema()).append("wsla_ticket t ");
		sb.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment b on t.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");
		sb.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_serial ps on t.product_serial_id = ps.product_serial_id ");
		sb.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_master pm on ps.product_id = pm.product_id ");

		return sb.toString();
	}
}

