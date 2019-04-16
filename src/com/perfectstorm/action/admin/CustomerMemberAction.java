package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

// PS Libs
import com.perfectstorm.data.CustomerMemberVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CustomerMemberAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage the members for a customer
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 10, 2019
 * @updates:
 ****************************************************************************/

public class CustomerMemberAction extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "customer_member";
	
	/**
	 * 
	 */
	public CustomerMemberAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public CustomerMemberAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public CustomerMemberAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getCustomerMembers(req.getParameter("customerId"), req.getParameter("memberId")));
	}
	
	/**
	 * Gets the attributes
	 * @return
	 */
	public List<CustomerMemberVO> getCustomerMembers(String customerId, String memberId) {
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ps_customer_member_xr a ");
		
		if (!StringUtil.isEmpty(customerId)) {
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_member b on a.member_id = b.member_id ");
			sql.append(DBUtil.WHERE_CLAUSE).append("customer_id = ? ");
			sql.append(DBUtil.ORDER_BY).append("last_nm, first_nm");
			vals.add(customerId);
		} else {
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_customer b on a.customer_id = b.customer_id ");
			sql.append(DBUtil.WHERE_CLAUSE).append("member_id = ? ");
			sql.append(DBUtil.ORDER_BY).append("customer_nm");
			vals.add(memberId);
		}
		
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new CustomerMemberVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		CustomerMemberVO custMem = new CustomerMemberVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.getBooleanParameter("isDelete")) {
				db.delete(custMem);
			} else if (!StringUtil.isEmpty(req.getParameter("customerMemberId"))){
				assignDefaultMember(req.getParameter("customerMemberId"));
			} else {
				db.insert(custMem);
			}
			
			setModuleData(custMem);
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			log.error("Unable to insert / delete customer member: " + custMem, e);
			setModuleData(custMem, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param customerMemberId
	 * @throws SQLException 
	 */
	public void assignDefaultMember(String customerMemberId) 
	throws SQLException {
		// Clear out all of the default flags
		StringBuilder sql = new StringBuilder(96);
		sql.append("update ").append(getCustomSchema()).append("ps_customer_member_xr ");
		sql.append("set default_flg = 0 where customer_id = (");
		sql.append("select customer_id from ").append(getCustomSchema());
		sql.append("ps_customer_member_xr where customer_member_id = ?)");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customerMemberId);
			ps.executeUpdate();
		}
		log.debug(sql + "|" + customerMemberId);
		
		// Update the default
		sql = new StringBuilder(96);
		sql.append("update ").append(getCustomSchema()).append("ps_customer_member_xr ");
		sql.append("set default_flg = 1 where customer_member_id = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customerMemberId);
			ps.executeUpdate();
		}
		log.debug(sql + "|" + customerMemberId);
	}
}

