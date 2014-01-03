package com.ram.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// WC Custom Lib
import com.ram.action.data.EventVO;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages the Inventory Events and items in the database.
 * This action also uploads an inventory item files and loads it into the 
 * database.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 12, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String dbs = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user == null) return;
		
		String customerId = StringUtil.checkVal(user.getUserExtendedInfo());
		String inventoryEventId = StringUtil.checkVal(req.getParameter("inventoryEventId"));
		
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(dbs).append("ram_inventory_event a ");
		s.append("inner join ").append(dbs).append("ram_inventory_location b ");
		s.append("on a.inventory_location_id = b.inventory_location_id ");
		s.append("where schedule_dt > ? ");
		if (customerId.length() > 0) s.append("and customer_id = ? ");
		if (inventoryEventId.length() > 0) s.append("and inventory_event_id = ? ");
		s.append("order by schedule_dt ");
		
		log.debug("Event SQL: " + s);
		
		PreparedStatement ps = null;
		List<EventVO> data = new ArrayList<EventVO>();
		int ctr = 1;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setDate(ctr++, Convert.formatSQLDate(new Date()));
			if (customerId.length() > 0) ps.setString(ctr++, customerId);
			if (inventoryEventId.length() > 0) ps.setString(ctr++, inventoryEventId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new EventVO(rs));
			}

			this.putModuleData(data, data.size(), false);
		} catch (Exception e) {
			log.error("Unable to Retrieve inventory event data", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
	}
}
