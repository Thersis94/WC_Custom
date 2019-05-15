package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.resource.ResourceBundleDataVO;
import com.siliconmtn.resource.ResourceBundleKeyVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.ResourceBundleManagerAction;
import com.wsla.common.WSLAConstants;
// WSLA Libs
import com.wsla.data.ticket.DefectVO;

/****************************************************************************
 * <b>Title</b>: DefectAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the defects
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 19, 2018
 * @updates:
 ****************************************************************************/

public class DefectAction extends SBActionAdapter {
	public static final String DEFECTS_TYPE = "defects";
	
	/**
	 * 
	 */
	public DefectAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DefectAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.  Note default access modifier
	 * @param attrs
	 * @param conn
	 */
	public DefectAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		log.debug("defects action retrieve");
		
		String providerId = req.getParameter("providerId");
		boolean hasActiveFlag = req.hasParameter("activeFlag");
		int activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
		setModuleData(getDefects(providerId, activeFlag, hasActiveFlag, new BSTableControlVO(req, DefectVO.class)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("defects action build");
		
		DefectVO dvo = new DefectVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if(StringUtil.isEmpty(req.getParameter("origDefectCode"))) {
				db.insert(dvo);
				addBundleKV(dvo);
			}else {
				db.save(dvo);
			}
			
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save defect attribute", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * On an insert the defect code is inserted into the resource bundle with a key id
	 * that matches the defect code
	 * @param dvo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void addBundleKV(DefectVO dvo) 
	throws InvalidDataException, DatabaseException {
		ResourceBundleManagerAction rbma = new ResourceBundleManagerAction(dbConn, attributes);

		// Save the bundle key
		ResourceBundleKeyVO key = new ResourceBundleKeyVO();
		key.setKeyId(dvo.getDefectCode());
		key.setKeyCode("defect." + dvo.getDefectCode());
		key.setKeyDesc(dvo.getDefectCode());
		key.setResourceBundleId(WSLAConstants.RESOURCE_BUNDLE_ID);
		rbma.saveBundleKey(key, true);
		
		// Save the bundle value
		ResourceBundleDataVO rbdv = new ResourceBundleDataVO();
		rbdv.setKeyId(key.getKeyId());
		rbdv.setLanguage("en");
		rbdv.setCountry("US");
		rbdv.setResourceBundleId(WSLAConstants.RESOURCE_BUNDLE_ID);
		rbdv.setValue(dvo.getDefectName());
		rbma.saveBundleKeyData(rbdv);
	}

	/**
	 * Gets a list of providers.  Since this list should be small (< 100)
	 * assuming client side pagination and filtering 
	 * @param providerType
	 * @param providerId
	 * @return
	 */
	public GridDataVO<DefectVO> getDefects(String providerId,  int activeFlag, boolean hasActiveFlag, BSTableControlVO bst) {
		
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(72);
		sql.append("select a.*, b.provider_nm from ").append(schema).append("wsla_defect a left outer join ").append(schema).append("wsla_provider b on a.provider_id = b.provider_id where 1=1 ");
		List<Object> params = new ArrayList<>();

		// Filter by provider id
		if (! StringUtil.checkVal(providerId).isEmpty()) {
			sql.append("and a.provider_id = ? ");
			params.add(providerId);
		}

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(defect_nm) like ? or lower(defect_cd) like ?)");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}

		// Filter by active flag
		if (hasActiveFlag &&  activeFlag >= 0 && activeFlag < 2) {
			sql.append("and a.active_flg = ? ");
			params.add(activeFlag);
		}
		log.info("sql " + sql.toString());
		sql.append(bst.getSQLOrderBy("defect_nm",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSQLWithCount(sql.toString(), params, new DefectVO(), bst.getLimit(), bst.getOffset());
	}
}