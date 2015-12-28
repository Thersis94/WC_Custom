package com.depuysynthes.emea.leihsets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LeihsetDisplayAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Searches the database for all items pertaining to the given 
 * search parameters and creates a list of Leihset documents from those results.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 09, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class LeihsetDisplayAction extends SBActionAdapter {

	public LeihsetDisplayAction() {
		super();
	}

	public LeihsetDisplayAction(ActionInitVO init) {
		super(init);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {		
		// Get the default language - give the user a list to choose from if one wasn't passed
		String category = StringUtil.checkVal(req.getParameter("category"), null);
		if (category == null) {
			super.putModuleData(this.loadCategoryTree());
		} else {
			//grab PageVo for preview mode
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			super.putModuleData(loadLeihsets(site.getOrganizationId(), category, page.isPreviewMode()));
		}
	}


	/**
	 * load the list of leihsets.  This uses a Map, onto which approved records 
	 * get replaced with pending ones if we're in preview mode.
	 * @param orgId
	 * @param bodyArea
	 * @param isPreviewMode
	 * @return
	 */
	protected List<LeihsetVO> loadLeihsets(String orgId, String bodyArea, boolean isPreviewMode) {
		String sql = getLeihsetQuery(bodyArea, isPreviewMode);
		log.debug(sql);

		Map<String,LeihsetVO> data = new LinkedHashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, bodyArea);
			ps.setString(2, orgId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String groupId = rs.getString("leihset_group_id");
				if (groupId == null || groupId.length() == 0) groupId = rs.getString("leihset_id");
				LeihsetVO vo = data.get(groupId);
				if (vo == null)
					vo = new LeihsetVO(rs, false);
				
				vo.setCategoryName(rs.getString("category_nm"));
				vo.setParentCategoryName(rs.getString("parent_category_nm"));

				if (vo.getLeihsetGroupId() == null) vo.setLeihsetGroupId(rs.getString("leihset_group_id"));
				if (rs.getString("leihset_asset_id") != null) vo.addResource(new LeihsetVO(rs, true));
				data.put(groupId, vo);
			}
		} catch (SQLException e) {
			log.error("Unable to load leihsets", e);
		}

		log.debug("loaded " + data.size() + " liehsets");
		List<LeihsetVO> list = new ArrayList<LeihsetVO>(data.values());
		Collections.sort(list);
		return list;
	}


	/**
	 * builds the complex union query that loads the language-specific IFUs
	 * as well as the default-language IFUs (in their absense)
	 * @param lang
	 * @return
	 */
	private String getLeihsetQuery(String bodyArea, boolean isPreviewMode) {
		String cDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		sql.append("SELECT l.*, la.*, lc.CATEGORY_NM, lc2.category_nm as parent_category_nm, ");
		sql.append("dsm.TITLE_TXT, dsm.TRACKING_NO_TXT ");
		sql.append("FROM ").append(cDb).append("DPY_SYN_LEIHSET l ");
		sql.append("LEFT JOIN ").append(cDb).append("DPY_SYN_LEIHSET_ASSET la on la.leihset_id=l.leihset_id ");
		sql.append("INNER JOIN ").append(cDb).append("DPY_SYN_LEIHSET_CATEGORY_XR xr on xr.leihset_id=l.leihset_id ");
		sql.append("INNER JOIN ").append(cDb).append("DPY_SYN_LEIHSET_CATEGORY lc on lc.leihset_category_id=xr.leihset_category_id "); //cat at level 3
		sql.append("INNER JOIN ").append(cDb).append("DPY_SYN_LEIHSET_CATEGORY lc2 on lc.parent_id=lc2.leihset_category_id and lc2.leihset_category_id=? "); //cat at level 2, parent of level 3
		sql.append("LEFT JOIN ").append(cDb).append("DPY_SYN_MEDIABIN dsm on dsm.DPY_SYN_MEDIABIN_ID = la.DPY_SYN_MEDIABIN_ID ");
		sql.append("WHERE l.archive_flg=0 and l.ORGANIZATION_ID=? ");
		if (!isPreviewMode) sql.append("and l.leihset_group_id is null ");
		//putting groupId first ensures we get live records before pending ones, which then get replaced on our Map and sorted by the Comparator
		sql.append("ORDER BY lc.category_nm, l.leihset_group_id, l.ORDER_NO, l.LEIHSET_NM, la.ORDER_NO, la.ASSET_NM");
		return sql.toString();
	}


	/**
	 * loads the categories tree for printing the selector hierarchy
	 * @return
	 */
	private Tree loadCategoryTree() {
		LeihsetCategoryAction ca = new LeihsetCategoryAction();
		ca.setDBConnection(dbConn);
		ca.setAttributes(getAttributes());
		return ca.loadCategoryTreeWithCounts();
	}
}