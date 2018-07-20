package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.AlbumVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseNote;
import com.siliconmtn.db.DatabaseNote.DBType;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: GalleryAction.java<p/>
 * <b>Description: Manages varioius types of RezDox photo albums.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 19, 2018
 ****************************************************************************/
public class GalleryAction extends SimpleActionAdapter {

	public static final String GALLERY_DATA = "galleryData";

	public GalleryAction() {
		super();
	}

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
		List<AlbumVO> albumList = retrieveAlbums(req);
		putModuleData(albumList, albumList.size(), false);

		// Get required data for the type of album
		if (req.hasParameter("residenceId")) {
			ResidenceAction ra = new ResidenceAction(dbConn, attributes);
			req.setAttribute(ResidenceAction.RESIDENCE_DATA, ra.retrieveResidences(req));

		} else if (req.hasParameter("businessId")) {
			BusinessAction ba = new BusinessAction(dbConn, attributes);
			req.setAttribute(BusinessAction.BUSINESS_DATA, ba.retrieveBusinesses(req));
		}
	}


	/**
	 * Retrives the selected photo albums
	 * 
	 * @param req
	 * @return
	 */
	@DatabaseNote(type = DBType.POSTGRES)
	protected List<AlbumVO> retrieveAlbums(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		AlbumVO opts = new AlbumVO(req);

		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.album_id, residence_id, a.business_id, album_nm, a.create_dt, ");
		sql.append("a.update_dt, (array_agg(image_url))[1] as image_url ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_album a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_photo p on a.album_id=p.album_id ");
		sql.append("where 1=0 ");

		if (!StringUtil.isEmpty(opts.getAlbumId())) {
			sql.append("or a.album_id=? ");
			params.add(opts.getAlbumId());

		} else if (!StringUtil.isEmpty(opts.getResidenceId())) {
			sql.append("or residence_id=? ");
			params.add(opts.getResidenceId());

		} else if (!StringUtil.isEmpty(opts.getBusinessId())) {
			sql.append("or a.business_id=? ");
			params.add(opts.getBusinessId());
		}

		sql.append("group by a.album_id");

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), params, new AlbumVO());
	}


	/* 
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		AlbumVO album = new AlbumVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(album);
			} else {
				dbp.save(album);
			}
		} catch(Exception e) {
			log.error("could not save photo album", e);
		}

		putModuleData(album, 1, false);
	}
}