package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.data.InventoryFormProcessor;
import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ResidenceVO;
import static com.rezdox.action.ResidenceAction.RESIDENCE_ID;
import com.rezdox.vo.InventoryItemVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

/***************************************************************************
 * <p><b>Title</b>: InventoryAction.java</p>
 * <p><b>Description:</b> Displays the user's Home Inventory (formerly Treasure Box)</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 14, 2018
 * <b>Changes:</b>
 ***************************************************************************/
public class InventoryAction extends SimpleActionAdapter {

	protected static final String REQ_TREASURE_ITEM_ID = "treasureItemId";
	private static final String EMPTY_RESID = "none";

	public InventoryAction() {
		super();
	}

	public InventoryAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public InventoryAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
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
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String treasureItemId = req.getParameter(REQ_TREASURE_ITEM_ID);

		//prepare the list page if not editing an item
		if (StringUtil.isEmpty(treasureItemId)) {
			//load a list of residences, for the dropdown
			List<ResidenceVO> residences =  loadResidences(req);
			mod.setAttribute("residences", residences);

			//if the user hasn't selected a residence yet then pick the first one as the default
			if (!req.hasParameter(RESIDENCE_ID) && !residences.isEmpty())
				req.setParameter(RESIDENCE_ID, residences.get(0).getResidenceId());
		}

		//prepare for edit form
		if (!StringUtil.isEmpty(treasureItemId)) {
			//load the form
			mod.setAttribute("dataContainer", loadForm(req));

			//load the photos/documents
			mod.setAttribute("photos", loadPhotos(req));
		}

		List<InventoryItemVO> data = loadBaseList(req, treasureItemId);
		mod.setActionData(data);

		setAttribute(Constants.MODULE_DATA, mod);
	}


	/**
	 * @param req
	 * @return
	 */
	private DataContainer loadForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Inventory Form: " + formId);

		// Set the requried params
		QueryParamVO param = new QueryParamVO("TREASURE_ITEM_ID", Boolean.FALSE);
		param.setValues(req.getParameterValues(REQ_TREASURE_ITEM_ID));
		GenericQueryVO query = new GenericQueryVO(formId);
		query.addConditional(param);

		// Get the form and the saved data for re-display onto the form.
		DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
		return util.loadFormWithData(formId, req, query, InventoryFormProcessor.class);
	}


	/**
	 * retrieve a list of Residences for the given user - used on display if the user has multiple.
	 * @param req
	 * @return
	 */
	private List<ResidenceVO> loadResidences(ActionRequest req) {
		ResidenceAction ra = new ResidenceAction(getDBConnection(), getAttributes());
		List<ResidenceVO> data = ra.listMyResidences(RezDoxUtils.getMemberId(req), null); //always get all of them

		//add a placeholder for detached items if they exist.
		if (getUnattachedCnt(req) > 0) {
			ResidenceVO vo = new ResidenceVO();
			vo.setResidenceId(EMPTY_RESID);
			vo.setResidenceName("Other");
			data.add(vo);
		}
		return data;
	}


	/**
	 * Count inventory not tied to a residence.  Adds an "Unattached" residence to 
	 * the stack if so, so bound inventory is accessible
	 * @param req
	 * @return
	 */
	private int getUnattachedCnt(ActionRequest req) {
		String schema = getCustomSchema();
		String sql = StringUtil.join("select cast(count(*) as int) as key from ", schema, "REZDOX_TREASURE_ITEM where owner_member_id=? and residence_id is null");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = dbp.executeSelect(sql, Arrays.asList(RezDoxUtils.getMemberId(req)), new GenericVO());
		return !data.isEmpty() ? (Integer)data.get(0).getKey() : 0;
	}


	/**
	 * Loads the list of treasure items displayed on the list page.
	 * If a pkId is given we only load one Item - the data then gets extended, then used for form display.
	 * @param req
	 * @param treasureItemId
	 * @return
	 */
	private List<InventoryItemVO> loadBaseList(ActionRequest req, String treasureItemId) {
		String residenceId = req.getParameter(RESIDENCE_ID);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, b.category_nm, c.photo_id, c.photo_nm, c.image_url, r.room_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_TREASURE_ITEM a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_TREASURE_CATEGORY b on a.treasure_category_cd=b.treasure_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PHOTO c on a.treasure_item_id=c.treasure_item_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM r on a.room_id=r.room_id ");
		sql.append("where a.owner_member_id=? ");
		if (EMPTY_RESID.equals(residenceId)) {
			sql.append("and a.residence_id is null ");
		} else if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and a.residence_id=? ");
		}
		if (!StringUtil.isEmpty(treasureItemId)) sql.append("and a.treasure_item_id=? ");
		sql.append("order by a.item_nm");
		log.debug(sql);

		params.add(RezDoxUtils.getMemberId(req));

		if (!StringUtil.isEmpty(residenceId) && !EMPTY_RESID.equals(residenceId)) //EMPTY_RESID gets passed for orphan inventory (not attached to a residence)
			params.add(residenceId);

		if (!StringUtil.isEmpty(treasureItemId)) 
			params.add(treasureItemId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new InventoryItemVO());
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 */
	private List<PhotoVO> loadPhotos(ActionRequest req) {
		return new PhotoAction(getDBConnection(), getAttributes()).retrievePhotos(req);
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void savePhoto(ActionRequest req) throws ActionException {
		new PhotoAction(getDBConnection(), getAttributes()).build(req);
	}


	/*
	 * form-submittal - the user is cashing in points for a reward.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean doRedirect = false;

		if (req.hasParameter("deleteItem")) {
			req.setParameter("isDelete", "1");
			req.setParameter(REQ_TREASURE_ITEM_ID, req.getParameter("deleteItem"));
			save(req);
			doRedirect = true;

		} else if (req.hasParameter("savePhoto")) {
			savePhoto(req);

		} else {
			// Place ActionInit on the Attributes map for the Data Save Handler.
			setAttribute(Constants.ACTION_DATA, getActionInit());

			// Call DataManagerUtil to save the form.
			String formId = RezDoxUtils.getFormId(getAttributes());
			DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
			util.saveForm(formId, req, InventoryFormProcessor.class);
		}

		//redirect the user if the request wasn't made over ajax
		if (doRedirect) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			String url = StringUtil.join(page.getFullPath(), "?residenceId=", StringUtil.checkVal(req.getParameter("residenceId")));
			sendRedirect(url, (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE), req);
		}
	}


	/**
	 * Saves the Treasure Item (only)
	 * @param req
	 * @throws ActionException 
	 */
	public void save(ActionRequest req) throws ActionException {
		InventoryItemVO vo = InventoryItemVO.instanceOf(req);
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
				//set pkId for downstream _attribute saving
				req.setParameter(REQ_TREASURE_ITEM_ID, vo.getTreasureItemId());
			}

		} catch (Exception e) {
			throw new ActionException("could not save treasure item", e);
		}
	}


	/**
	 * Detach treasure items from a residence.  This occurs when a residence is 
	 * transfered between owners - "We've Moved!".  The homeowner typically takes their stuff with them.
	 * Called from ResidenceTransferAction
	 * @param residenceId
	 */
	protected void detachResidence(String residenceId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("REZDOX_TREASURE_ITEM ");
		sql.append("set residence_id=?, room_id=?, update_dt=? where residence_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setNull(1, Types.VARCHAR);
			ps.setNull(2, Types.VARCHAR);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, residenceId);
			int cnt = ps.executeUpdate();
			log.debug(String.format("removed %d items from residence %s", cnt, residenceId));

		} catch (SQLException sqle) {
			log.error("could not reset treasure box items", sqle);
		}
	}
}