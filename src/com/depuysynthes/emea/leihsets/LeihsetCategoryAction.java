package com.depuysynthes.emea.leihsets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LeihsetCategoryAction.java<p/>
 * <b>Description: Handles reading/writing the DPY_SYN_LEIHSET_CATEGORY table.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 9, 2015
 ****************************************************************************/
public class LeihsetCategoryAction extends SBActionAdapter {

	public LeihsetCategoryAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public LeihsetCategoryAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		if (!req.hasParameter("addCategory")) return;
		
		String catId = addCategory(req.getParameter("addCategory"), req.getParameter("parentId"));
		Map<String, String> data = new HashMap<>();
		data.put("catId", catId);
		data.put("parentId", req.getParameter("parentId"));
		data.put("catNm", req.getParameter("addCategory"));
		putModuleData(data);
	}


	/**
	 * loads the entire category tree, joined to the _XR table for the given Leihset
	 * @param leihsetId
	 * @return
	 */
	protected Tree loadCategoryTree(String leihsetId) {
		List<Node> data = new ArrayList<>();
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select c.leihset_category_id, c.parent_id, c.category_nm, xr.leihset_category_xr_id, c.order_no ");
		sql.append("from ").append(customDb).append("DPY_SYN_LEIHSET_CATEGORY c ");
		sql.append("left outer join " ).append(customDb).append("DPY_SYN_LEIHSET_CATEGORY_XR xr ");
		sql.append("on c.leihset_category_id=xr.leihset_category_id and xr.leihset_id=? ");
		sql.append("order by c.parent_id, c.order_no, c.category_nm ");
		log.debug(sql + "|" + leihsetId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, leihsetId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString(1), rs.getString(2));
				n.setNodeName(rs.getString(3));
				n.setUserObject(Boolean.valueOf(rs.getInt(4) > 0)); //boolean here is simple "is selected for this leihset"
				data.add(n);
				if (log.isDebugEnabled())
						log.debug("added " + n.getNodeId() + " with parent " + n.getParentId() + " xrId=" + rs.getInt(4));
			}
		} catch (SQLException sqle) {
			log.error("could not load category tree", sqle);
		}

		return new Tree(data);
	}
	
	
	/**
	 * loads the full Tree, but with the # of leihsets in each instead of a boolean selected (above).
	 * @return
	 */
	protected Tree loadCategoryTreeWithCounts(boolean isPreview) {
		List<Node> data = new ArrayList<>();
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select c.leihset_category_id, c.parent_id, c.category_nm, count(l.leihset_id), c.order_no ");
		sql.append("from ").append(customDb).append("DPY_SYN_LEIHSET_CATEGORY c ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_LEIHSET_CATEGORY_XR xr on c.leihset_category_id=xr.leihset_category_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_LEIHSET l on xr.LEIHSET_ID=l.LEIHSET_ID ");
		sql.append("and l.archive_flg=0 "); //ignore anything tied to deleted Liehsets
		if (!isPreview) sql.append("and l.LEIHSET_GROUP_ID is null ");
		sql.append("group by c.leihset_category_id, c.parent_id, c.category_nm, c.order_no ");
		sql.append("order by c.parent_id, c.order_no, c.category_nm ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString(1), rs.getString(2));
				n.setNodeName(rs.getString(3));
				n.setUserObject(rs.getInt(4)); //# of leihsets in this category.
				data.add(n);
				if (log.isDebugEnabled())
					log.debug("added " + n.getNodeName() + " with parent " + n.getParentId());
			}
		} catch (SQLException sqle) {
			log.error("could not load category tree", sqle);
		}

		return new Tree(data);
	}


	/**
	 * adds a new category to an existing rung of the hierachy
	 * @param catNm
	 * @param parentId
	 * @return
	 */
	protected String addCategory(String catNm, String parentId) {
		String newCategoryId = new UUIDGenerator().getUUID();
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(customDb).append("DPY_SYN_LEIHSET_CATEGORY ");
		sql.append("(PARENT_ID, CATEGORY_NM, CREATE_DT, LEIHSET_CATEGORY_ID) ");
		sql.append("values (?,?,?,?)");
		log.debug(sql + "|" + parentId + "|" + catNm);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, StringUtil.checkVal(parentId, null));
			ps.setString(2, StringUtil.checkVal(catNm, null));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, newCategoryId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to add category: " + catNm + " with parentId: " + parentId, e);
			newCategoryId = null;
		}

		return newCategoryId;
	}


	/**
	 * saves the _XR entries binding a Leihset to it's categories
	 * @param vo
	 */
	protected void saveXRCategories(LeihsetVO vo) {
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		StringBuilder sql = new StringBuilder(100);
		sql.append("insert into ").append(customDb).append("DPY_SYN_LEIHSET_CATEGORY_XR ");
		sql.append("(LEIHSET_ID, LEIHSET_CATEGORY_ID, CREATE_DT) values (?,?,?)");
		log.debug(sql);

		deleteXRCategories(vo);

		//test whether we have anything to save
		if (vo.getCategories().size() == 0)
			return;
		
		//save any new categories being added
		List<String> saveCats = new ArrayList<String>();
		for (String cat : vo.getCategories())
			saveCats.add(cat);

		java.sql.Timestamp ts = Convert.getCurrentTimestamp();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String catNm : saveCats) {
				ps.setString(1, vo.getLeihsetId());
				ps.setString(2, catNm);
				ps.setTimestamp(3, ts);
				ps.addBatch();
			}
			ps.executeBatch();

		} catch (SQLException e) {
			log.error("Unable to save categories for leihset: " + vo.getLeihsetId(), e);
		}
	}


	/**
	 * purge all category values for the given Leihset
	 * @param vo
	 * @param customDb
	 */
	private void deleteXRCategories(LeihsetVO vo) {
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		String sql = "delete from " + customDb + "DPY_SYN_LEIHSET_CATEGORY_XR where LEIHSET_ID=?";
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, vo.getLeihsetId());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to delete categories for leihset: " + vo.getLeihsetId(), e);
		}
	}
}