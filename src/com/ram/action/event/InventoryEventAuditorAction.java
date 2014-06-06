package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



// RAM Data Feed
import com.ram.datafeed.data.AuditorVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventAuditorAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages Auditors for a given event
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 31, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventAuditorAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryEventAuditorAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventAuditorAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String encKey = (String) attributes.get(Constants.ENCRYPT_KEY);
		StringEncrypter se = null;
		try {
			se = new StringEncrypter(encKey);
		} catch (Exception e) {}
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, p.first_nm, p.last_nm from ").append(schema).append("ram_inventory_event_auditor_xr a ");
		sql.append("inner join ").append(schema).append("ram_auditor b ");
		sql.append("on a.auditor_id = b.auditor_id and inventory_event_id = ? ");
		sql.append("inner join profile p on b.profile_id=p.profile_id");
		log.debug(sql);
		PreparedStatement ps = null;
		List<InventoryEventAuditorVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("inventoryEventId")));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				InventoryEventAuditorVO vo = new InventoryEventAuditorVO(rs, true, encKey);
				
				//transpose auditor name from the field that has it correctly to the one that doesn't
				try {
					String firstNm= se.decrypt(rs.getString("first_nm"));
					String lastNm = se.decrypt(rs.getString("last_nm"));
					vo.setAuditorName(firstNm + " " + lastNm);
				} catch (Exception e) {
				     vo.setAuditorName(vo.getAuditor().getFirstName() + " " + vo.getAuditor().getLastName());
				}
				
				log.debug(vo.getAuditorName());
				data.add(vo);
			}
			
			this.putModuleData(data, data.size(), false);
		} catch(SQLException sqle) {
			throw new ActionException("could not load auditors for Event", sqle);
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
		for(String val : ele) {
			boolean insert = false;
			InventoryEventAuditorVO vo = parseData(val, inventoryEventId);
			
			if (vo.getInventoryEventAuditorId() == 0) {
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
	protected InventoryEventAuditorVO parseData(String data, int inventoryEventId) {
		String[] items = data.split("\\|");
		InventoryEventAuditorVO vo = new InventoryEventAuditorVO();
		AuditorVO auditor = new AuditorVO();
		vo.setInventoryEventId(inventoryEventId);
		vo.setInventoryEventAuditorId(Convert.formatInteger(items[0]));
		auditor.setAuditorId(Convert.formatInteger(items[1]));
		vo.setActiveFlag(Convert.formatInteger(items[2]));
		vo.setEventLeaderFlag(Convert.formatInteger(items[3]));
		vo.setAuditor(auditor);
		
		return vo;
	}
	
	/**
	 * Updates the database using the provided data and sql
	 * @param vo
	 * @param sql
	 * @throws ActionException
	 */
	public void updateDatabase(InventoryEventAuditorVO vo, StringBuilder sql, boolean insert) 
	throws ActionException {
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, vo.getInventoryEventId());
			ps.setInt(2, vo.getAuditor().getAuditorId());
			ps.setInt(3, vo.getEventLeaderFlag());
			ps.setInt(4, vo.getActiveFlag());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			if (! insert) ps.setInt(6, vo.getInventoryEventAuditorId());
			
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
			if (val.startsWith("eventAuditors_"))  data.add(req.getParameter(val));
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
		sql.append("insert into ").append(schema).append("ram_inventory_event_auditor_xr ");
		sql.append("(inventory_event_id, auditor_id, event_leader_flg, ");
		sql.append("active_flg, create_dt) values(?,?,?,?,?)");
		
		return sql;		
	}
	
	/**
	 * Builds the update statement
	 * @return
	 */
	protected StringBuilder buildUpdateRecord() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(schema).append("ram_inventory_event_auditor_xr ");
		sql.append("set inventory_event_id = ?, auditor_id = ?, ");
		sql.append("event_leader_flg = ?, active_flg = ?, update_dt = ? ");
		sql.append("where inventory_event_auditor_xr_id = ? ");
		
		return sql;		
	}
}
