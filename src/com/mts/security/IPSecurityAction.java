package com.mts.security;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: IPSecurityAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> manages the data for the list of companies using IP Address security
 * for the subscriber login
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 10, 2019
 * @updates:
 ****************************************************************************/

public class IPSecurityAction extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "ip-sec";
	
	/**
	 * 
	 */
	public IPSecurityAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public IPSecurityAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getListRanges());
	}
	
	/**
	 * Gets the security list of companies by IP address range
	 * @return
	 */
	public List<IPSecurityVO> getListRanges() {
		StringBuilder sql = new StringBuilder(192);
		sql.append("select * from ");
		sql.append(getCustomSchema()).append("mts_ip_security a ");
		sql.append("inner join ").append(getCustomSchema());
		sql.append("mts_user b on a.user_id = b.user_id ");
		sql.append("order by company_nm, ip_start_txt ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new IPSecurityVO());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		IPSecurityVO ip = new IPSecurityVO(req);
		
		try {
			if (req.hasParameter("delete")) deleteIPSecurity(ip);
			else saveIPSecurity(ip);

			setModuleData(ip);
		} catch (Exception e) {
			log.error("Unabel to save ip security object", e);
			setModuleData(ip, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param ip
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveIPSecurity(IPSecurityVO ip) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(ip);
	}
	
	/**
	 * 
	 * @param ip
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void deleteIPSecurity(IPSecurityVO ip) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.delete(ip);
	}
}

