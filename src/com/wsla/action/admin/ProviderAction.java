package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
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
		String providerId = req.getParameter("providerId");
		String providerTypeId = req.getParameter("providerTypeId");
		setModuleData(getProviders(providerId, providerTypeId, new BSTableControlVO(req, ProviderVO.class)));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProviderVO provider = new ProviderVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(provider);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save provider infromation", e);
		}
	}
	
	/**
	 * Gets a list of providers.  Since this list should be small (< 100)
	 * assuming client side pagination and filtering 
	 * @param providerType
	 * @param providerId
	 * @return
	 */
	public GridDataVO<ProviderVO> getProviders(String providerId, String providerType, BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(72);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_provider where 1=1 ");
		List<Object> params = new ArrayList<>();
		
		// Filter by provider id
		if (! StringUtil.checkVal(providerId).isEmpty()) {
			sql.append("and provider_id = ? ");
			params.add(providerId);
		}
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and lower(provider_nm) like ? ");
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		// Filter by provider type
		if (! StringUtil.isEmpty(providerType)) {
			sql.append("and provider_type_id = ? ");
			params.add(providerType);
		}
		
		sql.append(bst.getSQLOrderBy("provider_nm",  "asc"));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new ProviderVO(), bst.getLimit(), bst.getOffset());
	}
}

