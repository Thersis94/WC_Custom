package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: TicketVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the ticket data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 13, 2018
 * @updates:
 ****************************************************************************/

public class TicketVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -288262467687670031L;
	
	// Member Variables
	private String ticketId;
	private String ticketIdText;
	private String ticketName;
	private String description;
	private StatusCode statusCode;
	
	/**
	 * 
	 */
	public TicketVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketVO(ActionRequest req) {
		super(req);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param rs
	 */
	public TicketVO(ResultSet rs) {
		super(rs);
		// TODO Auto-generated constructor stub
	}

}

