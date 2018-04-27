package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.DocumentVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> DocumentAction.java<br/>
 * <b>Description:</b> Manages the add/update/edit/load transactions for RezDox Documents.
 * Used from Projects as well a Treasure Items.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
public class DocumentAction extends SimpleActionAdapter {

	public DocumentAction() {
		super();
	}

	public DocumentAction(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public DocumentAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * List the documents available for the given lookup column.  Return an empty list if undeterminable.  
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<DocumentVO> data = loadDocuments(req);
		putModuleData(data);
	}


	/**
	 * loads a list of Documents from the database
	 * @param req
	 * @return
	 */
	protected List<DocumentVO> loadDocuments(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("REZDOX_DOCUMENT ");
		sql.append("where 1=0 ");

		if (req.hasParameter("treasureItemId")) {
			params.add(req.getParameter("treasureItemId"));
			sql.append("or treasure_item_id=? ");

		} else if (req.hasParameter("projectId")) {
			params.add(req.getParameter("projectId"));
			sql.append("or project_id=? ");
		}
		sql.append("order by document_nm");


		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new DocumentVO());
	}


	@Override
	public void build(ActionRequest req) throws ActionException {
		save(req, req.hasParameter("isDelete"));

		//this action does not redirect because it is not directly invokable from the browser.
		//see ProjectAction or TreasureItemAction
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void save(ActionRequest req, boolean isDelete) throws ActionException {
		DocumentVO vo = DocumentVO.instanceOf(req);
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			if (isDelete) {
				db.delete(vo);
			} else {
				db.save(vo);
			}

		} catch (Exception e) {
			throw new ActionException("could not save document", e);
		}
	}
}