package com.mts.security;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public IPSecurityAction(Connection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(new SMTDBConnection(dbConn));
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
	
		if (req.getBooleanParameter("ipCheck")) {
			setModuleData(checkIpAddress(req.getParameter("ipAddress")));
		} else if (role != null && "100".equals(role.getRoleId())) {
			setModuleData(getListRanges());
		}
	}
	
	/**
	 * Checks to see if the user's ip address is assigned in the Ip Security table
	 * @param ipAddress
	 * @return
	 */
	public boolean checkIpAddress(String ipAddress) {
		log.debug(ipAddress);
		if (StringUtil.isEmpty(ipAddress)) return false;
		
		// Convert the ips to numbers in the case of a leading zero
		int[] ip = Arrays.stream(ipAddress.split("\\."))
				.mapToInt(Integer::parseInt)
				.toArray();
		
		// Build the ip back with the first 2 numbers as the base
		String ipBase = ip[0] + "." + ip[1] + ".%";
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_ip_security where ip_base_txt like ? ");
		log.debug(sql.length() + "|" + sql + "|" + ipBase);
		
		// Search the DB for matches for each element.  Return true when matched
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ipBase);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					IPSecurityVO vo = new IPSecurityVO(rs);
					if (vo.insideIPRange(ipAddress)) return true;
				}
			}
		} catch (Exception e) {
			log.error("Unable to retrieve ip address check", e);
		}
		
		return false;
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
		sql.append("order by company_nm, ip_base_txt ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new IPSecurityVO());
	}
	
	/**
	 * Gets the profile id of the user for the matching ip address 
	 * @param ip request IP address
	 * @return profile id, null if not in range
	 */
	public String getProfileIdByIP(String ip) {
		List<IPSecurityVO> ips = getListRanges();
		for (IPSecurityVO ipvo : ips) {
			if (ipvo.insideIPRange(ip)) return ipvo.getUser().getProfileId();
		}
		
		return null;
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

