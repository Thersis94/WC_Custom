package com.wsla.action.ticket.transaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: ProviderLocationTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes for the user information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 23, 2018
 * @updates:
 ****************************************************************************/

public class ProviderLocationTransaction extends SBActionAdapter {

	/**
	 * Transaction key for the facade
	 */
	public static final String AJAX_KEY = "providerLocation";
	
	/**
	 * 
	 */
	public ProviderLocationTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderLocationTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			putModuleData(this.saveLocation(req));
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Stores the user information
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException
	 * @throws SQLException 
	 */
	public ProviderLocationVO saveLocation(ActionRequest req) 
	throws InvalidDataException, DatabaseException, SQLException {
		ProviderLocationVO loc = new ProviderLocationVO(req);
		loc.setLocationId(req.getParameter("retailerId"));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.getByPrimaryKey(loc);
		
		// Update the ticket retailer
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket ");
		sql.append("set retailer_id = ? where ticket_id = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, loc.getLocationId());
			ps.setString(2, req.getParameter("ticketId"));
			ps.executeUpdate();
		}
		
		// Update the Ticket Assignment
		sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket_assignment ");
		sql.append("set location_id = ? where ticket_id = ? and assg_type_cd = 'RETAILER'");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, loc.getLocationId());
			ps.setString(2, req.getParameter("ticketId"));
			ps.executeUpdate();
		}
		return loc;
	}
}

