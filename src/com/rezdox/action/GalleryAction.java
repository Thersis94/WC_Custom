package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.PhotoVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GalleryAction.java<p/>
 * <b>Description: Manages varioius types of RezDox photo galleries.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 19, 2018
 ****************************************************************************/
public class GalleryAction extends SimpleActionAdapter {
	
	public GalleryAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public GalleryAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<PhotoVO> photoList = retrievePhotos(req);
		putModuleData(photoList, photoList.size(), false);
	}
	
	/**
	 * Retrives the selected photos
	 * 
	 * @param req
	 * @return
	 */
	protected List<PhotoVO> retrievePhotos(ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		PhotoVO photoOptions = new PhotoVO(req);
		
		StringBuilder sql = new StringBuilder(300);
		sql.append("select photo_id, business_id, residence_id, treasure_item_id, project_id, photo_nm, desc_txt, ");
		sql.append("image_url, thumbnail_url, order_no, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_photo ");
		sql.append("where 1=1 ");
		
		List<Object> params = new ArrayList<>();
		
		if (!StringUtil.isEmpty(photoOptions.getPhotoId())) {
			sql.append("and photo_id = ? ");
			params.add(photoOptions.getPhotoId());	
		}
		
		if (!StringUtil.isEmpty(photoOptions.getResidenceId())) {
			sql.append("and residence_id = ? ");
			params.add(photoOptions.getResidenceId());
			
			ResidenceAction ra = new ResidenceAction(getActionInit());
			ra.setAttributes(getAttributes());
			ra.setDBConnection(getDBConnection());
			req.setAttribute(ResidenceAction.RESIDENCE_DATA, ra.retrieveResidences(req));
		}
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new PhotoVO());
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		PhotoVO photo = new PhotoVO(req);
		DBProcessor dbp = new DBProcessor(dbConn);
		
		try {
			dbp.save(photo);
		} catch(Exception e) {
			log.error("Couldn't save RezDox photo. ", e);
		}
		
		this.putModuleData(photo.getPhotoId(), 1, false);
	}
}