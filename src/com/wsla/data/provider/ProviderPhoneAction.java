package com.wsla.data.provider;

import java.sql.PreparedStatement;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProviderPhoneAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the 800 numbers for a provider
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 25, 2019
 * @updates:
 ****************************************************************************/

public class ProviderPhoneAction extends SBActionAdapter {

	/**
	 * Key to utilize when calling this action via the ajax controller
	 */
	public static final String AJAX_KEY = "providerPhone";
	
	/**
	 * 
	 */
	public ProviderPhoneAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderPhoneAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(96);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_provider_phone ");
		sql.append("where provider_id = ? order by phone_number_txt");
		log.debug(sql.length() + "|" + sql);
		
		List<Object> vals = new ArrayList<>();
		vals.add(req.getParameter("providerId"));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		setModuleData(db.executeSelect(sql.toString(), vals, new ProviderPhoneVO()));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProviderPhoneVO ppvo = new ProviderPhoneVO(req);

		try {
			if (req.getBooleanParameter("updateActive")) {
				updateActiveFlag(ppvo);
			} else {
				updatePhoneEntry(ppvo);
			}
			
			setModuleData(ppvo);
		} catch (Exception e) {
			log.error("Unable to save Phone: " + ppvo.toString(), e);
			setModuleData(ppvo, 1, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Adds / saves a provider phone entry
	 * @param ppvo
	 * @throws SQLException
	 */
	public void updatePhoneEntry(ProviderPhoneVO ppvo) throws SQLException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(ppvo);
		} catch (Exception e) {
			throw new SQLException("Unable to save Provider Phone", e);
		}
	}
	
	/**
	 * Updates the active flag on the provider phone
	 * @param ppvo
	 * @throws SQLException
	 */
	public void updateActiveFlag(ProviderPhoneVO ppvo) throws SQLException {
		
		StringBuilder sql = new StringBuilder(96);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_phone ");
		sql.append("set active_flg = ?  where provider_phone_id = ?");
		log.debug(sql.length() + "|" + sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, ppvo.getActiveFlag());
			ps.setString(2, ppvo.getProviderPhoneId());
			ps.executeUpdate();
		}
	}
}

