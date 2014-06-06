package com.ram.action.user;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



// RAM Data Feed Libs
import com.ram.datafeed.data.AuditorVO;

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
 * <b>Title</b>: AuditorAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages the RAM Auditor data
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jun 1, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class AuditorAction extends SBActionAdapter {

	/**
	 * 
	 */
	public AuditorAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public AuditorAction(ActionInitVO actionInit) {
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
		int auditorId = Convert.formatInteger(req.getParameter("auditorId")); 
		boolean activeFlag = Convert.formatBoolean(req.getParameter("activeFlag"));
		StringEncrypter se = null;
		try {
			se = new StringEncrypter(encKey);
		} catch (Exception e) {}
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder();
		sql.append("select a.auditor_id, a.active_flg,a.profile_id, b.* from ");
		sql.append(schema).append("ram_auditor a inner join ");
		sql.append("profile b on a.profile_id = b.profile_id ");
		if (auditorId > 0) sql.append("and auditor_id = ? ");
		if (activeFlag) sql.append("and active_flg = 1 ");

		log.info("******* " + sql);
		PreparedStatement ps = null;
		List<AuditorVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (auditorId > 0) ps.setInt(1, auditorId);;
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AuditorVO vo = new AuditorVO(rs, true, encKey);
				
				//TODO this code should be fixed inside AuditorVO.setData() and removed from here
				//see also com.ram.event.InventoryEventAuditorAction
				//transpose auditor name from the field that has it correctly to the one that doesn't
				try {
					vo.setFirstName(se.decrypt(rs.getString("first_nm")));
					vo.setLastName(se.decrypt(rs.getString("last_nm")));
					
				} catch (Exception e) {
					vo.setFirstName(rs.getString("first_nm"));
					vo.setLastName(rs.getString("last_nm"));
				}
				
				data.add(vo);
			}
			
			this.putModuleData(data, data.size(), false);
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}

		log.info("Inventory Event Retrieve: " + sql);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
