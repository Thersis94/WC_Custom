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
 * <b>Title</b>: PhotoAction.java<p/>
 * <b>Description: Manages varioius types of RezDox photo lists (albums, treasure box, projects, etc).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 19, 2018
 ****************************************************************************/
public class PhotoAction extends SimpleActionAdapter {
	
	public PhotoAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhotoAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * overloaded constructor to simplify invocation
	 * @param dbConnection
	 * @param attributes
	 */
	public PhotoAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<PhotoVO> photoList = retrievePhotos(req);
		putModuleData(photoList, photoList.size(), false);

		if (req.hasParameter("albumId")) {
			GalleryAction ga = new GalleryAction(getActionInit());
			ga.setAttributes(getAttributes());
			ga.setDBConnection(getDBConnection());
			req.setAttribute(GalleryAction.GALLERY_DATA, ga.retrieveAlbums(req));
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
		PhotoVO opts = new PhotoVO(req);
		
		StringBuilder sql = new StringBuilder(300);
		sql.append("select photo_id, album_id, treasure_item_id, project_id, photo_nm, desc_txt, ");
		sql.append("image_url, thumbnail_url, order_no, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_photo ");
		sql.append("where 1=0 ");
		
		List<Object> params = new ArrayList<>();
		
		if (!StringUtil.isEmpty(opts.getPhotoId())) {
			sql.append("or photo_id = ? ");
			params.add(opts.getPhotoId());	
		
		} else if (!StringUtil.isEmpty(opts.getTreasureItemId())) {
			sql.append("or treasure_item_id=? ");
			params.add(opts.getTreasureItemId());

		} else if (!StringUtil.isEmpty(opts.getAlbumId())) {
			sql.append("or album_id = ? ");
			params.add(opts.getAlbumId());
		}
		sql.append("order by coalesce(update_dt, create_dt) desc, photo_nm "); //most recent first, or by name?  Can be changed per UI reqs.
		
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
			if (req.hasParameter("isDelete")) {
				dbp.delete(photo);
			} else {
				dbp.save(photo);
			}
		} catch(Exception e) {
			log.error("Couldn't save RezDox photo. ", e);
		}
		
		this.putModuleData(photo.getPhotoId(), 1, false);
	}
}