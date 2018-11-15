package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketAssignmentTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Modifies the ticket assignments
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 31, 2018
 * @updates:
 ****************************************************************************/

public class TicketAssignmentTransaction extends BaseTransactionAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "assignment";
	
	/**
	 * 
	 */
	public TicketAssignmentTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketAssignmentTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		TicketAssignmentVO tAss = new TicketAssignmentVO(req);
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		try {
			
			putModuleData(assign(tAss, ledger, user));
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves a Ticket Assignment when changing
	 * @param tAss
	 * @param ledger
	 * @param user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public Object assign(TicketAssignmentVO tAss, TicketLedgerVO ledger, UserVO user) 
	throws InvalidDataException, DatabaseException, SQLException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Add the ledger entry
		ledger.setDispositionBy(user.getUserId());
		ledger.setSummary(LedgerSummary.CAS_ASSIGNED.summary);
		
		// Only add a status code change to the ledger if this is the first
		// Assignment of a CAS
		if (StringUtil.isEmpty(tAss.getTicketAssignmentId()) && TypeCode.CAS.equals(tAss.getTypeCode())) {
			ledger.setStatusCode(StatusCode.CAS_ASSIGNED);
		}
		
		// Save the ledger and the assignment
		db.save(ledger);
		db.save(tAss);
		return getAssignmentData(tAss, db);
	}

	/**
	 * Gets the data for the location just assigned to the ticket
	 * @param tAss
	 * @param db
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	public Object getAssignmentData(TicketAssignmentVO tAss, DBProcessor db) 
	throws InvalidDataException, DatabaseException, SQLException {
		ProviderLocationVO vo = new ProviderLocationVO();
		
		// Load the provider or the user depending on the type
		if (! TypeCode.CALLER.equals(tAss.getTypeCode())) {
			vo.setLocationId(tAss.getLocationId());
			db.getByPrimaryKey(vo);

		} else {
			StringBuilder sql = new StringBuilder(128);
			sql.append("select * from ").append(getCustomSchema()).append("wsla_user a ");
			sql.append(DBUtil.INNER_JOIN).append("profile_address b ");
			sql.append("on a.profile_id = b.profile_id where user_id = ? ");
			
			try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, tAss.getUserId());
				
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						vo = new ProviderLocationVO(rs);
						vo.setLocationName(rs.getString("first_nm") + " " + rs.getString("last_nm"));
					}
				}
			}
		}
		
		return vo;
	}
}

