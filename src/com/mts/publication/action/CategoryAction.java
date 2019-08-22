package com.mts.publication.action;

// JDK 1.8.x
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.db.orm.*;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.metadata.MetadataVO;
import com.smt.sitebuilder.action.metadata.OrgMetadataAction;

/****************************************************************************
 * <b>Title</b>: CategoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to manage document widget meta data.  Wrapper to 
 * ensure the data is correctly assigned
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class CategoryAction extends SBActionAdapter {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "category";
	
	/**
	 * 
	 */
	public CategoryAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public CategoryAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public CategoryAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		OrgMetadataAction oma = new OrgMetadataAction(getDBConnection(), getAttributes());
		setModuleData(oma.getOrgMetadata("MTS", null, false));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		MetadataVO mdvo = new MetadataVO(req);
		if (StringUtil.isEmpty(mdvo.getParentId())) mdvo.setParentId(null);
		mdvo.setFieldDesc(mdvo.getFieldName());
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection());
			
			if (req.getBooleanParameter("isInsert")) {
				db.insert(mdvo);
			} else if (req.getBooleanParameter("isDelete")) {
				db.delete(mdvo);
			} else {
				db.update(mdvo);
			}
		} catch (Exception e) {
			log.error("Unable to save category", e);
			putModuleData(mdvo, 1, false, e.getLocalizedMessage(), true);
		}
	}

}

