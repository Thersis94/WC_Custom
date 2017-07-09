package com.ram.action.util;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

import com.ram.action.or.vo.RAMCaseKitVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
// WC Libs 3.2
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
	@Override
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
				getORRooms(role);
				break;
			case "kits":
				getKits(req);
				break;
			default:
				log.debug("can't find list type");
			
		}
		
	}

	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 */
	public void getORRooms(SBUserRole role) {
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
	 * Gets a list of or rooms for a given provider
	 * @param role
	 * @param req 
	 */
	public void getKits(ActionRequest req) {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select ck.case_kit_id, ck.case_id, p.product_id, p.product_nm, ck.processed_flg, lm.serial_no_txt from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_case_kit ck ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_location_item_master lm on ck.location_item_master_id = lm.location_item_master_id ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_product p on lm.product_id = p.product_id ");
		sql.append("where case_id = ? and processed_flg = 0 ");
		
		log.debug(sql + "|" + req.getParameter("caseId"));

		String caseID = StringUtil.checkVal(req.getParameter("caseId"));
		List<Object> params = new ArrayList<>();
		params.add(caseID);
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new RAMCaseKitVO());
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
