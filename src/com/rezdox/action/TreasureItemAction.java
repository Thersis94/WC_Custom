package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.TreasureItemVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> TreasureItemAction.java<br/>
 * <b>Description:</b> Overseer of the Treasure Box RezDox component - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
public class TreasureItemAction extends SimpleActionAdapter {

	public TreasureItemAction() {
		super();
	}

	public TreasureItemAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Load a Treasure Box from the DB - including all it's sub-components if a pkId was passed
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String treasureItemId = req.getParameter("treasureItemId");
		boolean loadSingleItem = !StringUtil.isEmpty(treasureItemId);

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, b.category_nm, c.attribute_id, c.slug_txt, c.value_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_TREASURE_ITEM a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_TREASURE_CATEGORY b on a.treasure_category_cd=b.treasure_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_TREASURE_ATTRIBUTE c on a.treasure_item_id=c.treasure_item_id ");
		sql.append("where 1=0 ");

		if (loadSingleItem) {
			params.add(treasureItemId);
			sql.append("or a.treasure_item_id=? ");
		}
		sql.append("order by a.item_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<TreasureItemVO> data = db.executeSelect(sql.toString(), params, new TreasureItemVO());

		//load ancilary details if they were requested and we indeed matched a single record in the lookup query.
		if (loadSingleItem && data != null && data.size() == 1) {
			TreasureItemVO vo = data.get(0);
			loadDocuments(vo, req);
			loadPhotos(vo, req);
			putModuleData(vo);
		} else {
			putModuleData(data);
		}
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 */
	private void loadPhotos(TreasureItemVO vo, ActionRequest req) {
		PhotoAction pa = new PhotoAction(getDBConnection(), getAttributes());
		vo.setPhotos(pa.retrievePhotos(req));
	}


	/**
	 * load a list of documents tied to this treasure box item
	 * @param vo
	 * @param req
	 */
	private void loadDocuments(TreasureItemVO vo, ActionRequest req) {
		DocumentAction da = new DocumentAction(getDBConnection(), getAttributes());
		vo.setDocuments(da.loadDocuments(req));
	}


	/*
	 * Save the Treasure Box.  
	 * Note: This could be "save everything" or "add a photo", "add a document", etc. (individual pieces)
	 * 		use request parameters to determine which transaction to run.  Default to none.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("saveItem")) {
			save(req);
		} else if (req.hasParameter("savePhoto")) {
			savePhoto(req);
		} else if (req.hasParameter("saveDocument")) {
			saveDocument(req);
		}
	}


	/**
	 * Saves the Treasure Item and it's attributes (only) - (2 DB tables)
	 * @param vo
	 * @param isDelete
	 * @throws ActionException 
	 */
	protected void save(ActionRequest req) throws ActionException {
		TreasureItemVO vo = TreasureItemVO.instanceOf(req);
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}

		} catch (Exception e) {
			throw new ActionException("could not save treasure item", e);
		}
	}


	/**
	 * Calls to save the item's documents
	 * @param vo
	 * @param isDelete
	 */
	private void saveDocument(ActionRequest req) throws ActionException {
		new DocumentAction(getDBConnection(), getAttributes()).build(req);
	}


	/**
	 * Calls to save the item's photos
	 * @param vo
	 * @param isDelete
	 * @throws ActionException 
	 */
	private void savePhoto(ActionRequest req) throws ActionException {
		new PhotoAction(getDBConnection(), getAttributes()).build(req);
	}
}