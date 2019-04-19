package com.mts.publication.action;

import java.nio.file.Files;
import java.nio.file.Paths;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// MTS Libs
import com.mts.publication.data.AssetVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.*;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AssetAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to manage MTS assets for a given publication,
 * issue or document
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class AssetAction extends SBActionAdapter {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "asset";
	
	/**
	 * 
	 */
	public AssetAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AssetAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String keyId = req.getParameter("objectKeyId");
		setModuleData(getAssets(keyId));
	}
	
	/**
	 * 
	 * @return
	 */
	public List<AssetVO> getAssets(String objectKeyId) {
		StringBuilder sql = new StringBuilder(96);
		sql.append("select *  from ").append(getCustomSchema()).append("mts_document_asset a ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_asset_type b ");
		sql.append("on a.asset_type_cd = b.asset_type_cd ");
		sql.append("where object_key_id = ? order by document_nm ");
		log.debug(sql.length() + "|" + sql + "|" + objectKeyId);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(objectKeyId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new AssetVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.getBooleanParameter("isDelete")) {
			delete(req);
			return;
		}
		
		AssetVO asset = new AssetVO(req);
		asset.setDocumentName(req.getParameter("fileName"));
		asset.setDocumentPath(req.getParameter("value"));
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(asset);
			putModuleData(asset);
		} catch (Exception e) {
			log.error("Unable to save asset info", e);
			putModuleData(asset, 1, false, e.getLocalizedMessage(), true);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		AssetVO avo = new AssetVO(req);
		String path = getAttribute(Constants.BINARY_PATH) + "/file_transfer" + avo.getDocumentPath();
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			// Delete the item from the DB
			db.delete(avo);
			
			// Delete the item from the file path
			Files.deleteIfExists(Paths.get(path));
			log.info("Delete file: " + path);
			
			setModuleData(avo);
		} catch(Exception e) {
			log.error("Unable to remove asset", e);
			setModuleData(avo, 0, e.getLocalizedMessage());
		}
	}
}

