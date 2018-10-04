package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.provider.ProviderType;
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

	/**
	 * Overloaded constructor used for calling between actions.  Note default access modifier
	 * @param attrs
	 * @param conn
	 */
	public ProviderAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
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
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(72);
		sql.append("select * from ").append(schema).append("wsla_provider where 1=1 ");
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
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProviderVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Generate a name-ordered list of providers for the given type.
	 * Called from SelectLookupAction.
	 * @return
	 */
	public List<GenericVO> getProviderOptions(ProviderType type) {
		if (type == null) return Collections.emptyList();

		StringBuilder sql = new StringBuilder(200);
		sql.append("select provider_id as key, provider_nm as value from ");
		sql.append(getCustomSchema()).append("wsla_provider where provider_type_id=? ");
		sql.append("order by provider_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		return db.executeSelect(sql.toString(), Arrays.asList(type.toString()), new GenericVO());
	}
	
	/**
	 * Grabs a unique list of OEMs by the product category
	 * @param productCategoryId
	 * @return
	 */
	public List<GenericVO> getOEMsByProductCategory(String productCategoryId) {
		List<Object> vals = new ArrayList<>();
		vals.add(productCategoryId);
		
		StringBuilder sql = new StringBuilder(368);
		sql.append("select a.provider_id as key, a.provider_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_master b on a.provider_id = b.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_category_xr c on b.product_id = c.product_id ");
		sql.append("where a.provider_type_id = 'OEM' and c.product_category_id = ? ");
		sql.append("group by key, value ");
		sql.append("order by a.provider_nm ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		return db.executeSelect(sql.toString(), vals, new GenericVO());
	}
	
}