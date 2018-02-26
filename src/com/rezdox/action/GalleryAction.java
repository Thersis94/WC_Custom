package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.PhotoVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

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

	/**
	 * overloaded constructor to simplify invocation
	 * @param dbConnection
	 * @param attributes
	 */
	public GalleryAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<PhotoVO> photoList = retrievePhotos(req);
		putModuleData(photoList, photoList.size(), false);

		if (req.hasParameter("residenceId")) {
			ResidenceAction ra = new ResidenceAction(getActionInit());
			ra.setAttributes(getAttributes());
			ra.setDBConnection(getDBConnection());
			req.setAttribute(ResidenceAction.RESIDENCE_DATA, ra.retrieveResidences(req));
		}
	}

	/**
	 * Retrives the selected photos
	 * 
	 * @param req
	 * @return
	 */
	protected List<PhotoVO> retrievePhotos(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		PhotoVO opts = new PhotoVO(req);

		StringBuilder sql = new StringBuilder(300);
		sql.append("select photo_id, business_id, residence_id, treasure_item_id, project_id, photo_nm, desc_txt, ");
		sql.append("image_url, thumbnail_url, order_no, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_photo ");
		sql.append("where 1=0 ");

		if (!StringUtil.isEmpty(opts.getPhotoId())) {
			sql.append("or photo_id=? ");
			params.add(opts.getPhotoId());

		} else if (!StringUtil.isEmpty(opts.getTreasureItemId())) {
			sql.append("or treasure_item_id=? ");
			params.add(opts.getTreasureItemId());

		} else if (!StringUtil.isEmpty(opts.getResidenceId())) {
			sql.append("or residence_id=? ");
			params.add(opts.getResidenceId());
		}
		sql.append("order by coalesce(update_dt, create_dt) desc, photo_nm "); //most recent first, or by name?  Can be changed per UI reqs.

		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new PhotoVO());
	}


	/* 
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		PhotoVO photo = new PhotoVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(photo);
			} else {
				dbp.save(photo);
			}
		} catch(Exception e) {
			log.error("Couldn't save RezDox photo. ", e);
		}

		putModuleData(photo.getPhotoId(), 1, false);
	}
}