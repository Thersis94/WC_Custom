package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.PhotoVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
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

	private static final String RELA_FLDR = "/photo/inventory/";
	public static final String UPLOAD_PATH = "uploadPath";
	public static final String URL_ROOT="urlRoot";

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

		} else if (!StringUtil.isEmpty(opts.getProjectId())) {
			sql.append("or project_id=? ");
			params.add(opts.getProjectId());

		} else if (!StringUtil.isEmpty(opts.getAlbumId())) {
			sql.append("or album_id = ? ");
			params.add(opts.getAlbumId());
		}
		sql.append("order by order_no, coalesce(update_dt, create_dt), photo_nm "); //most recent first, or by name?  Can be changed per UI reqs.
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), params, new PhotoVO());
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		PhotoVO photo = new PhotoVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(photo);
			} else if (req.hasParameter("partialEdit")) {
				savePhotoUpdate(photo, dbp);
			} else {
				dbp.save(photo);
			}
		} catch(Exception e) {
			log.error("Couldn't save RezDox photo. ", e);
		}

		this.putModuleData(photo.getPhotoId(), 1, false);
	}

	/**
	 * Saves edited data when only partial data is submitted
	 * 
	 * @param photo
	 */
	public void savePhotoUpdate(PhotoVO photo, DBProcessor dbp) {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_photo ");
		sql.append("set photo_nm = ?, desc_txt = ? ");
		sql.append("where photo_id = ? ");

		List<String> fields = Arrays.asList("photo_nm", "desc_txt", "photo_id");

		try {
			dbp.executeSqlUpdate(sql.toString(), photo, fields);
		} catch (Exception e) {
			log.error("Could not update RezDox photo data", e);
		}
	}


	/**
	 * Write uploaded files to disk, and then to DB.  
	 * Called from InventoryFormProcessor & BusinessFormProcessor
	 * @param req
	 * @param fpbdArr
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void saveFiles(ActionRequest req) {
		String uploadPath;
		String urlRoot;
		if (req.getAttribute(UPLOAD_PATH) != null) {
			uploadPath = (String)req.getAttribute(UPLOAD_PATH);
			urlRoot = (String) req.getAttribute(URL_ROOT);
		} else {
			String root = StringUtil.checkVal(getAttribute(Constants.SECURE_PATH_TO_BINARY));
			uploadPath = StringUtil.join(root, (String)getAttribute(Constants.ORG_ALIAS), req.getParameter("organizationId"), RELA_FLDR, req.getParameter("treasureItemId"), "/");
			log.debug("writing files to " + root);

			urlRoot = StringUtil.join(RELA_FLDR, req.getParameter("treasureItemId"), "/");
		}

		FileLoader fl = new FileLoader(getAttributes());
		for (FilePartDataBean fpdb : req.getFiles()) {
			try {
				String fileUrl = fl.writeFiles(fpdb.getFileData(), uploadPath, fpdb.getFileName(), true, true);
				fl.reorientFiles();
				String fPath = StringUtil.join(urlRoot, fileUrl);
				log.debug("file written: " + fPath);
				req.setParameter("photoName", fpdb.getFileName());
				req.setParameter("imageUrl", fPath);
				build(req);
			} catch (Exception e) {
				log.error("could not save inventory file", e);
			}
		}
	}
}
