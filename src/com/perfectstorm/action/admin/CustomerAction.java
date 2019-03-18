package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

// PS Libs
import com.perfectstorm.data.CustomerVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CustomerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage the customers
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 10, 2019
 * @updates:
 ****************************************************************************/

public class CustomerAction extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "customer";
	
	/**
	 * 
	 */
	public CustomerAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public CustomerAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public CustomerAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
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
		BSTableControlVO bst = new BSTableControlVO(req, CustomerVO.class);
		setModuleData(getCustomers(bst, null, null));
	}
	
	/**
	 * Gets the attributes
	 * @return
	 */
	public GridDataVO<CustomerVO> getCustomers(BSTableControlVO bst, String customerType, String memberId) {
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("ps_customer ");
		sql.append("where 1=1 ");
		if (bst.hasSearch()) {
			sql.append("and lower(customer_nm) like ? ");
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		if (!StringUtil.isEmpty(customerType)) {
			sql.append("and customer_type_cd = ? ");
			vals.add(customerType);
		}
		
		if (!StringUtil.isEmpty(memberId)) {
			sql.append("and customer_id not in (");
			sql.append("select customer_id from ").append(getCustomSchema()).append("ps_customer_member_xr ");
			sql.append("where member_id = ?) ");
			vals.add(memberId);
		}
		
		sql.append("order by ").append(bst.getDBSortColumnName("customer_nm"));
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new CustomerVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		CustomerVO customer = new CustomerVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(customer);
			
			setModuleData(customer);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save attribute: " + customer, e);
			setModuleData(customer, 0, e.getLocalizedMessage());
		}
	}
}

