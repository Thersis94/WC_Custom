package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.List;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.provider.ProviderUserVO;

/****************************************************************************
 * <b>Title</b>: ProviderLocationUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the user accounts for the providers
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 25, 2018
 * @updates:
 ****************************************************************************/

public class ProviderLocationUserAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ProviderLocationUserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderLocationUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Retrieving users: ");
		
		setModuleData(getUsers(req.getParameter("locationId"), new BSTableControlVO(req, ProviderUserVO.class)));
	}
	
	/**
	 * Manages the user data for the grid
	 * @param locationId
	 * @param providerUserId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProviderUserVO> getUsers(String locationId, BSTableControlVO bst) {
		List<Object> params = new ArrayList<>();
		params.add(locationId);
		
		StringBuilder sql = new StringBuilder(320);
		sql.append("select a.*, b.*, e.role_nm from custom.wsla_provider_user_xr a ");
		sql.append("inner join custom.wsla_user b on a.user_id = b.user_id ");
		sql.append("inner join profile c on b.profile_id = c.profile_id ");
		sql.append("inner join profile_role d on c.profile_id = d.profile_id ");
		sql.append("inner join role e on d.role_id = e.role_id ");
		sql.append("where location_id = ? ");
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append(bst.getSQLOrderBy("last_nm",  "asc"));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new ProviderUserVO(), bst.getLimit(), bst.getOffset());

	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Saving user: ");
	}
}

