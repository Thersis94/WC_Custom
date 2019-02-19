package com.wsla.action.ticket.transaction;
//JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <b>Title</b>: TicketSearchTransaction.java <b>Project</b>: WC_Custom
 * <b>Description: </b> a transaction that searched for the ticket numbers.
 * <b>Copyright:</b> Copyright (c) 2018 <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author Ryan Riker
 * @version 3.0
 * @since Nov 19, 2018
 * @updates:
 ****************************************************************************/

public class TicketSearchTransaction extends BaseTransactionAction {
	
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "ticketSearch";
	public static final String EMAIL = "email";
	public static final String PHONE = "mainPhone";
	public static final String TICKET_NUMBER = "ticketNumber";
	public static final String REDIRECT_QUERY_STRING = "?type=editServiceOrder&ticketIdText=";
	public static final String PAGE_PATH = "/user-portal";

	/**
	 * 
	 */
	public TicketSearchTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketSearchTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.
	 * ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("ticket search build called");
		
		if(req.hasParameter("isContact")) {
			sendTicketSearchNotification(req.getParameter(EMAIL), req.getParameter(PHONE), req.getParameter(TICKET_NUMBER));
			return;
		}
		
		List<TicketVO> data = new ArrayList<>();
		if(req.hasParameter(TICKET_NUMBER)) {
			data = ticketNumberConfirm(req.getParameter(TICKET_NUMBER));
		}else {
			//search for ticket number
			try {
				data = phoneEmailticketSearch(req.getStringParameter(EMAIL), req.getStringParameter(PHONE));
			} catch (Exception e) {
				log.error("could not search for ticket Number",e);
				putModuleData("", 0, false, (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE), true);
			}
			
		}
		log.debug("count " + data.size());
		putModuleData(data);
	}

	/**
	 * this method sends a notification to wsla with the information ented into the form.  
	 * @param email
	 * @param phone
	 * @param ticketNumber
	 */
	private void sendTicketSearchNotification(String email, String phone, String ticketNumber) {
		//new map of data
		Map<String, Object> mData = new HashMap<>();
		mData.put(EMAIL, StringUtil.checkVal(email));
		mData.put(PHONE, StringUtil.checkVal(phone));
		mData.put(TICKET_NUMBER, StringUtil.checkVal(ticketNumber));
		
		processNotification(null,null, StatusCode.SERVICE_ORDER_NUMBER_LOOKUP, mData);
	}

	/**
	 * confirms that there is a ticket no in the database matching the seared for term
	 * @param parameter
	 * @return 
	 */
	private List<TicketVO> ticketNumberConfirm(String ticketNumber) {
		
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(75);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_serial s on t.product_serial_id = s.product_serial_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" t.ticket_no = ? and t.status_cd != 'CLOSED' ");
		params.add(ticketNumber);
		
		return db.executeSelect(sql.toString(), params, new TicketVO());

	}

	/**
	 * searches the user table for emails and phone numbers
	 * @param stringParameter
	 * @param stringParameter2
	 * @return 
	 * @throws InvalidDataException 
	 */
	private List<TicketVO> phoneEmailticketSearch(String email, String phone) throws InvalidDataException {
		if(email == null && phone == null) {
			throw new InvalidDataException("can not search with both null email and null phone strings ");
		}
		
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(205);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user u on t.originator_user_id = u.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_serial s on t.product_serial_id = s.product_serial_id ");
		
		buildWhere(sql, email, phone, params);
		
		log.debug("sql: " + sql.toString() +"|" + params);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new TicketVO());
		
	}


	/**
	 * builds the where clause for the ticket search sql
	 * @param sql 
	 * @param email
	 * @param phone
	 * @param params 
	 * @return
	 */
	private void buildWhere(StringBuilder sql, String email, String phone, List<Object> params) {
	
		sql.append("where 1=1 and t.status_cd != 'CLOSED' ");
		
		if (email != null) {
			sql.append(" and u.email_address_txt = ? ");
			params.add(email);
		}
		
		if(phone != null) {
			sql.append(" and u.main_phone_txt = ? ");
			params.add(phone);
		}
		
		sql.append("order by t.create_dt desc ");
		
	}

}
