package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.provider.ProviderVO;

/****************************************************************************
 * <b>Title</b>: ProviderAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the providers
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 19, 2018
 * @updates:
 ****************************************************************************/

public class ProviderAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ProviderAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		putModuleData(getProviders(req.getParameter("providerId")));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
	}
	
	/**
	 * Gets a list of providers.  Since this list should be small (< 100)
	 * assuming client side pagination and filtering 
	 * @param providerType
	 * @param providerId
	 * @return
	 */
	public List<ProviderVO> getProviders(String providerId) {
		StringBuilder sql = new StringBuilder(72);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_provider ");
		List<Object> params = new ArrayList<>();

		if (! StringUtil.checkVal(providerId).isEmpty()) {
			sql.append("where provider_id = ? ");
			params.add(providerId);
		}
		
		sql.append("order by provider_nm");
		log.info("SQL: " + sql.length() + "|" + sql);
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), params, new ProviderVO());
	}
}

