package com.depuysynthes.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: DismantlingAdminAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Media Bin "Dismantling" data
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 10, 2020
 * @updates:
 ****************************************************************************/
public class DismantlingAdminAction extends SBActionAdapter {

	/**
	 * 
	 */
	public DismantlingAdminAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DismantlingAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		StringBuilder sql = new StringBuilder();
		sql.append("select * from custom.dpy_syn_mediabin dsm "); 
		sql.append("where literature_type_txt = 'Dismantling' ");
		sql.append("order by title_txt");
		
		List<MediaBinAssetVO> data = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				data.add(new MediaBinAssetVO(rs));
			}
			
			setModuleData(data);
		} catch (Exception e) {
			log.error("Unable to retrieve media bin data", e);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		MediaBinAssetVO asset = new MediaBinAssetVO(req);
		asset.setAssetNm("Synthes International/Product Support Material/legacy_Synthes_PDF/" + asset.getFileNm());
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			if (req.getBooleanParameter("isInsert")) db.insert(asset);
			else db.update(asset);
			
			setModuleData(asset);
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			setModuleData(asset, 1, e.getLocalizedMessage());
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		MediaBinAssetVO asset = new MediaBinAssetVO(req);
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(asset);
			
			setModuleData("success");
		} catch (Exception e) {
			log.error("Unable to delete asset", e);
			setModuleData(asset, 1, e.getLocalizedMessage());
		}
	}
}
