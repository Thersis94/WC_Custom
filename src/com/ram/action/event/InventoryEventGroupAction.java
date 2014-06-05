package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



import java.util.Date;



// RAM Data Feed Libs
import com.ram.action.data.InventoryEventGroupVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventGroupAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 29, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventGroupAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryEventGroupAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventGroupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String id = req.getParameter("inventoryEventGroupId");
		
		try {
			InventoryEventGroupVO data = this.getGroupInfo(id);
			//this date is the selected Event's schedule date, all recurring events we create will be from this date forwards.
			data.setCreateDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("eventDate")));
			this.putModuleData(data);
		} catch (SQLException e) {
			throw new ActionException("unable to retrieve inventory group data", e);
		}
	}
	
	/**
	 * Gets a single VO based upon the provided ID
	 * @param id InventoryEventGroupId to retrieve
	 * @return
	 * @throws SQLException
	 */
	public InventoryEventGroupVO getGroupInfo(String id) throws SQLException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_inventory_event_group ");
		sql.append("where inventory_event_group_id = ?");
		log.info("SQL: " + sql);
		
		InventoryEventGroupVO vo = new InventoryEventGroupVO();
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, id);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			vo = new InventoryEventGroupVO(rs);
		}
		
		ps.close();
		return vo;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		boolean isRecurrence = Convert.formatBoolean(req.getParameter("isRecurrence"));
		int inventoryEventId = Convert.formatInteger(req.getParameter("inventoryEventId"));
		
		if (!isRecurrence && inventoryEventId > 0) {
			log.debug("not saving, no group info has changed");
			return;
		}

		// Insert or update the database
		InventoryEventGroupVO eventGroup = new InventoryEventGroupVO(req);
		try {
			DBProcessor db = new DBProcessor(dbConn, getAttribute(Constants.CUSTOM_DB_SCHEMA) + "");
			if (StringUtil.checkVal(eventGroup.getInventoryEventGroupId()).length() == 0) {
				eventGroup.setCreateDate(new Date());
				db.insert(eventGroup);
				
				// add the group id to the request object for further processing
				req.setParameter("inventoryEventGroupId", db.getGeneratedPKId());
				eventGroup.setInventoryEventGroupId(db.getGeneratedPKId());
			} else { 
				eventGroup.setUpdateDate(new Date());
				db.update(eventGroup);
			}
		} catch(Exception e) {
			throw new ActionException("Unable to update Event Group", e);
		}
		
		if (isRecurrence) //this code is unreachable if the above 'save' fails.
			refactorEvents(eventGroup, req);
		
	}
	
	
	/**
	 * forward the request to the action responsible for iterating the events and 
	 * updating them according to the specifed recurrence schedule. 
	 * @param eventGroup
	 * @param req
	 * @throws ActionException
	 */
	private void refactorEvents(InventoryEventGroupVO eventGroup, SMTServletRequest req) 
			throws ActionException {
		attributes.put(InventoryEventRecurrenceAction.EVENT_GRP_OBJ, eventGroup);
		SMTActionInterface action = new InventoryEventRecurrenceAction(actionInit);
		action.setDBConnection(dbConn);
		action.setAttributes(attributes);
		action.update(req);
	}
}
