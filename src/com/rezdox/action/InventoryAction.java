package com.rezdox.action;

import static com.rezdox.action.ResidenceAction.RESIDENCE_ID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rezdox.action.RewardsAction.Reward;
import com.rezdox.data.InventoryFormProcessor;
import com.rezdox.vo.InventoryItemVO;
import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ResidenceVO;
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
	private static final String REQ_ITEMS = "items";
	private static final String EMPTY_RESID = "none";
	private static final String SES_HOMEOWNERS = "rezdox-homeowners";

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

			//create a list of residences that support moving items to
			mod.setAttribute("moveResidences", filterMovableResidences(residences, req));
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
	 * Inventory items cannot be moved across residences owned by different homeowners.
	 * Filter the list of residences the user can see down to the ones we can transfer to (based on residence being viewed)
	 * @param residences
	 * @param req
	 * @return
	 */
	private Object filterMovableResidences(List<ResidenceVO> residences, ActionRequest req) {
		//if the user can only see 'this' residence - this whole method is moot.
		if (residences == null || residences.size() < 2) return residences;

		String residenceId = req.getParameter(RESIDENCE_ID);
		Map<String, ResidenceVO> resMap = residences.stream().collect(Collectors.toMap(ResidenceVO::getResidenceId, Function.identity()));
		List<ResidenceVO> filteredList = new ArrayList<>(resMap.size());
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(400);
		sql.append("select xr2.residence_id from ").append(schema).append("REZDOX_RESIDENCE_MEMBER_XR xr ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR xr2 ");
		sql.append("on xr.member_id=xr2.member_id and xr2.residence_id != ? and xr2.status_flg=1");
		sql.append("where xr.residence_id=? and xr.status_flg=1");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, residenceId);
			ps.setString(2, residenceId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//this RS returned a list of residences owned by the same person.  That doesn't mean they're all shared w/me.  Only perserve the ones the user has visibility to.
				ResidenceVO vo = resMap.get(rs.getString(1));
				if (vo != null) filteredList.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load homeowners residences", sqle);
		}

		log.debug(String.format("Found %d residences we can move items to", filteredList.size()));
		return filteredList;
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
		if (!data.isEmpty() && getUnattachedCnt(req) > 0) {
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
		final String LOJ = DBUtil.LEFT_OUTER_JOIN + schema;
		String memberId = RezDoxUtils.getMemberId(req);
		List<Object> params = new ArrayList<>();
		params.add(memberId);
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, b.category_nm, c.photo_id, c.photo_nm, c.image_url, r.room_nm, tia.value_txt as warranty_exp ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_TREASURE_ITEM a ");
		sql.append(LOJ).append("REZDOX_TREASURE_CATEGORY b on a.treasure_category_cd=b.treasure_category_cd ");
		sql.append(LOJ).append("REZDOX_PHOTO c on a.treasure_item_id=c.treasure_item_id ");
		sql.append(LOJ).append("REZDOX_ROOM r on a.room_id=r.room_id ");
		sql.append(LOJ).append("REZDOX_TREASURE_ITEM_ATTRIBUTE tia on tia.treasure_item_id=a.treasure_item_id ");
		sql.append("and tia.slug_txt='").append(InventoryFormProcessor.WARRANTY_SLUG).append("' ");
		sql.append(LOJ).append("REZDOX_RESIDENCE_MEMBER_XR mxr on a.residence_id=mxr.residence_id and mxr.member_id=? ");
		sql.append("where 1=1 ");
		//load items that are shared but not private, or that are mine. -JM- 07.26.18 for Profile Sharing
		sql.append("and ((mxr.status_flg=2 and a.privacy_flg != 1) or mxr.status_flg=1 or mxr.member_id is null)");

		if (EMPTY_RESID.equals(residenceId) || StringUtil.isEmpty(residenceId)) {
			sql.append("and a.owner_member_id=? and a.residence_id is null ");
			params.add(memberId);
		} else {
			sql.append("and a.residence_id=? ");
			params.add(residenceId);
		}
		if (!StringUtil.isEmpty(treasureItemId)) {
			sql.append("and a.treasure_item_id=? ");
			params.add(treasureItemId);
		} else if (!StringUtil.isEmpty(req.getParameter(REQ_ITEMS))) {
			String[] items = req.getParameter(REQ_ITEMS).split(",");
			sql.append("and a.treasure_item_id in (");
			DBUtil.preparedStatmentQuestion(items.length, sql);
			sql.append(") ");
			params.addAll(Arrays.asList(items));
		}
		//add a stop-gap to prevent any one person from seeing ALL data (if none of the above conditions match)
		if (params.isEmpty())
			sql.append("and 1=0 ");

		sql.append(" order by a.item_nm");
		log.debug(sql);

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


	/**
	 * move items from on residence to another
	 * @param req
	 * @throws ActionException
	 */
	private void moveItems(ActionRequest req) {
		if (!req.hasParameter(REQ_ITEMS)) return;
		String[] items = req.getParameter(REQ_ITEMS).split(",");
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("REZDOX_TREASURE_ITEM ");
		sql.append("set residence_id=?, update_dt=? where treasure_item_id in (");
		DBUtil.preparedStatmentQuestion(items.length, sql);
		sql.append(") and owner_member_id=?");
		log.debug(sql);

		int x = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++x, req.getParameter(RESIDENCE_ID));
			ps.setTimestamp(++x, Convert.getCurrentTimestamp());
			for (String id : items) {
				ps.setString(++x, id);
			}
			ps.setString(++x, RezDoxUtils.getMemberId(req));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not move treasure items", sqle);
		}
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

		} else if (req.hasParameter("moveItems")) {
			moveItems(req);
			doRedirect = true;

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
				boolean isNew = StringUtil.isEmpty(vo.getTreasureItemId());

				if (isNew)
					ensureProperHomeowner(vo, req);

				db.save(vo);
				//set pkId for downstream _attribute saving
				req.setParameter(REQ_TREASURE_ITEM_ID, vo.getTreasureItemId());

				if (isNew)
					awardPoints(RezDoxUtils.getMemberId(req), req);
			}

		} catch (Exception e) {
			throw new ActionException("could not save treasure item", e);
		}
	}


	/**
	 * Lookup the proper homeowner for the given residence - make sure any new
	 * items added to it get owned by the homeowner.  (This is a factor for shared residences).
	 * Use session to cache a Map to make repeated calls faster.
	 * @param vo
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void ensureProperHomeowner(InventoryItemVO vo, ActionRequest req) {
		Map<String, String> owners = (Map<String, String>) req.getSession().getAttribute(SES_HOMEOWNERS);
		if (owners == null) owners = new HashMap<>();

		String ownerId = owners.get(vo.getResidenceId());
		if (StringUtil.isEmpty(ownerId)) {
			ownerId = lookupHomeowner(vo.getResidenceId());
			owners.put(vo.getResidenceId(), ownerId);
			req.getSession().setAttribute(SES_HOMEOWNERS, owners);
		}

		log.debug(String.format("homeowner for %s is %s", vo.getResidenceId(), ownerId));
		vo.setOwnerMemberId(ownerId);
	}


	/**
	 * Retrieve the homeowner's memberId for the given residence
	 * @param residenceId
	 * @return
	 */
	private String lookupHomeowner(String residenceId) {
		return new ResidenceAction(getDBConnection(), getAttributes()).getHomeownerMemberId(residenceId);
	}


	/**
	 * @param memberId
	 */
	private void awardPoints(String memberId, ActionRequest req) {
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		try {
			ra.applyReward(Reward.TREASURE_BOX.name(), memberId, req);
		} catch (ActionException e) {
			log.error("could not award reward points", e);
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