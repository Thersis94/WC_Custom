package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.AlbumVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseNote;
import com.siliconmtn.db.DatabaseNote.DBType;
import com.siliconmtn.db.orm.DBProcessor;
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
	public void retrieve(ActionRequest req) throws ActionException {
		List<AlbumVO> albumList = retrieveAlbums(req);
		putModuleData(albumList, albumList.size(), false);
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
		AlbumVO albumOptions = new AlbumVO(req);
		
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.album_id, residence_id, business_id, album_nm, a.create_dt, a.update_dt, (array_agg(image_url))[1] as image_url ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_album a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_photo p on a.album_id = p.album_id ");
		sql.append("where 1=1 ");
		
		List<Object> params = new ArrayList<>();
		
		if (!StringUtil.isEmpty(albumOptions.getAlbumId())) {
			sql.append("and a.album_id = ? ");
			params.add(albumOptions.getAlbumId());	
		}
		
		if (!StringUtil.isEmpty(albumOptions.getResidenceId())) {
			sql.append("and residence_id = ? ");
			params.add(albumOptions.getResidenceId());
			
			ResidenceAction ra = new ResidenceAction(getActionInit());
			ra.setAttributes(getAttributes());
			ra.setDBConnection(getDBConnection());
			req.setAttribute(ResidenceAction.RESIDENCE_DATA, ra.retrieveResidences(req));
		}
		
		sql.append("group by a.album_id ");
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new AlbumVO());
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		AlbumVO album = new AlbumVO(req);
		DBProcessor dbp = new DBProcessor(dbConn);
		
		try {
			dbp.save(album);
		} catch(Exception e) {
			log.error("Couldn't save RezDox photo album. ", e);
		}
		
		this.putModuleData(album.getAlbumId(), 1, false);
	}
}