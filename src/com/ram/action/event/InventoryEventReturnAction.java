package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// RAM Data Feed Libs
import com.ram.datafeed.data.InventoryEventReturnVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventReturnAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 31, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventReturnAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryEventReturnAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventReturnAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_event_return_xr a ");
		sql.append("inner join ").append(schema).append("ram_product b ");
		sql.append("on a.product_id = b.product_id and inventory_event_id = ? ");
		
		PreparedStatement ps = null;
		List<InventoryEventReturnVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("inventoryEventId")));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new InventoryEventReturnVO(rs, true));
			}
			
			this.putModuleData(data, data.size(), false);
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		int inventoryEventId = Convert.formatInteger(req.getParameter("inventoryEventId"));
		StringBuilder sql = null;
		List<String> ele = this.getParameters(req);
		log.info("Returns: " + inventoryEventId);
		for(String val : ele) {
			boolean insert = false;
			InventoryEventReturnVO vo = parseData(val, inventoryEventId);
			
			if (vo.getEventReturnId() == 0) {
				insert = true;
				sql = buildInsertRecord();
			} else sql = buildUpdateRecord();
			
			this.updateDatabase(vo, sql, insert);
		}
	}
	
	/**
	 * Takes the pipe delimited list of data and splits into the appropriate variables
	 * @param data
	 * @param inventoryEventId
	 * @return
	 */
	protected InventoryEventReturnVO parseData(String data, int inventoryEventId) {
		String[] items = data.split("\\|");
		InventoryEventReturnVO vo = new InventoryEventReturnVO();
		vo.setInventoryEventId(inventoryEventId);
		vo.setEventReturnId(Convert.formatInteger(items[0]));
		vo.setProductId(Convert.formatInteger(items[1]));
		vo.setLotNumber(items[2]);
		vo.setQuantity(Convert.formatInteger(items[3]));
		vo.setActiveFlag(Convert.formatInteger(items[4]));
		
		return vo;
	}
	
	/**
	 * Updates the database using the provided data and sql
	 * @param vo
	 * @param sql
	 * @throws ActionException
	 */
	public void updateDatabase(InventoryEventReturnVO vo, StringBuilder sql, boolean insert) 
	throws ActionException {
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, vo.getInventoryEventId());
			ps.setInt(2, vo.getProductId());
			ps.setInt(3, vo.getQuantity());
			ps.setString(4, vo.getLotNumber());
			ps.setInt(5, vo.getActiveFlag());
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			if (! insert) ps.setInt(7, vo.getEventReturnId());
			
			ps.executeUpdate();
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
	}
	
	/**
	 * Gets the pipe delimited list of values for the event returns
	 * @param req
	 * @return
	 */
	protected List<String> getParameters(SMTServletRequest req) {
		List<String> data = new ArrayList<>();
		List<String> vals = Collections.list(req.getParameterNames());
		
		for(String val : vals) {
			if (val.startsWith("eventReturns_"))  data.add(req.getParameter(val));
		}
		
		return data;
	}
	
	/**
	 * Builds the insert statement
	 * @return
	 */
	protected StringBuilder buildInsertRecord() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema).append("ram_event_return_xr ");
		sql.append("(inventory_event_id, product_id, quantity_no, lot_number_txt, ");
		sql.append("active_flg, create_dt) values(?,?,?,?,?,?)");
		
		return sql;		
	}
	
	/**
	 * Builds the update statement
	 * @return
	 */
	protected StringBuilder buildUpdateRecord() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(schema).append("ram_event_return_xr ");
		sql.append("set inventory_event_id = ?, product_id = ?, quantity_no = ?,");
		sql.append(" lot_number_txt = ?, active_flg = ?, update_dt = ? ");
		sql.append("where event_return_id = ?");
		
		return sql;		
	}
}
