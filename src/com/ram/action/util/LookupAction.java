package com.ram.action.util;


import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/********************************************************************
 * <b>Title: </b>LookupAction.java<br/>
 * <b>Description: </b>Utilit action to manage data lookups for selects and other items<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jul 8, 2017
 * Last Updated: 
 *******************************************************************/
public class LookupAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public LookupAction() {
		super();
	}
	
	/**
	 * 
	 * @param actionInit
	 */
	public LookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		String type = req.getParameter("type", "none");
		SMTSession ses = req.getSession();
		SBUserRole role = (SBUserRole)ses.getAttribute(Constants.ROLE_DATA);
		
		switch(type) {
			case "providers":
				getProviders(role);
				break;
			case "surgeons":
				getSurgeons(role);
				break;
			case "orRooms":
				getORRooms(role, Convert.formatInteger(req.getParameter("selected")));
				break;
			default:
				log.debug("can't find list type");
			
		}
		
	}

	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 */
	public void getORRooms(SBUserRole role, int customerId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select or_room_id as key, or_name as value from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_or_room a ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer_location b on a.customer_location_id = b.customer_location_id ");
		sql.append("where customer_id = cast(? as int) ");
		log.debug(sql + "|" + role.getAttribute(0));
		
		List<Object> params = new ArrayList<>();
		params.add(role.getAttribute(0));
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of providers for a given user
	 * @param role
	 */
	public void getProviders(SBUserRole role) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_id as key, customer_nm as value from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_customer ");
		sql.append("where customer_id = cast(? as int) ");
		
		List<Object> params = new ArrayList<>();
		params.add(role.getAttribute(0));
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of surgeons for a given customer
	 * @param role
	 */
	public void getSurgeons(SBUserRole role) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select a.surgeon_id as key, coalesce(first_nm, '') || ' ' || coalesce(last_nm, '') as value from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon a ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon_customer_xr b ");
		sql.append("on a.surgeon_id = b.surgeon_id where customer_id = cast(? as int) order by last_nm, first_nm ");
		log.debug(sql + "|" + role.getAttribute(0));
		
		List<Object> params = new ArrayList<>();
		params.add(role.getAttribute(0));
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		
		this.putModuleData(data);
	}
}
