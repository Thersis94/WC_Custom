package com.fastsigns.action.franchise;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

//WC_customs
import com.fastsigns.action.approval.WebeditApprover.WebeditType;

//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.approval.ApprovalAction;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KeystoneApprovalAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2011
 ****************************************************************************/
public class KeystoneApprovalAction extends SimpleActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		String franchiseId = req.getParameter("apprFranchiseId");
		if (AdminConstants.REQ_LIST.equalsIgnoreCase(req.getParameter(AdminConstants.REQUEST_TYPE))) {
			super.retrieve(req);
		} else if  (franchiseId == null || franchiseId.length() == 0) {
			getApprovableFranchises(req); 
		}  else  {
			getApprovalsByCenter(req, franchiseId);
		}
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		ApprovalAction a = new ApprovalAction(actionInit);
		a.setDBConnection(dbConn);
		a.setAttributes(getAttributes());
		a.update(req);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.FALSE);
	}

	/**
	 * Get all the franchises that have approvable content.
	 * @param req
	 */
	private void getApprovableFranchises(SMTServletRequest req) {
		SiteVO siteData = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		if (siteData == null) return;
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		PreparedStatement ps = null;
		Set<String> approvalNeeded = new TreeSet<String>();
		try {
			ps = dbConn.prepareStatement(buildFranchiseQuery());
			ps.setString(1,"Webedit");
			ps.setString(2,"Page");
			ps.setString(3,WebeditType.CenterPage.toString());
			ps.setString(4, SyncStatus.Approved.toString());
			ps.setString(5, SyncStatus.InProgress.toString());
			ps.setString(6, SyncStatus.Declined.toString());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (StringUtil.checkVal(rs.getString("ORGANIZATION_ID")).contains(orgId+"_")) {
					approvalNeeded.add(rs.getString("ORGANIZATION_ID").replace(orgId+"_", ""));
				} else {
					approvalNeeded.add(rs.getString("ORGANIZATION_ID"));
				}
			}
		} catch (SQLException e) {
			log.error("Could not get list of franchises with items needing approval. ", e);
		}
		putModuleData(approvalNeeded);
	}
	
	/**
	 * Create the query that gathers all centers that have modules, whiteboard changes, sub-pages,
	 * center images, asset updates, and job postings that are awaiting approval right now.
	 * @return
	 */
	private String buildFranchiseQuery() {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("SELECT * FROM WC_SYNC WHERE (MODULE_TYPE_ID = ? or (MODULE_TYPE_ID = ? and PORTLET_DESC = ?)) ");
		sql.append("and WC_SYNC_STATUS_CD not in (?,?,?)");
		log.debug(sql);
		
		
		return sql.toString();
	}

	/**
	 * Get all the approvable items related to a franchise
	 * @param req
	 * @throws ActionException
	 */
	private void getApprovalsByCenter(SMTServletRequest req, String franchiseId) throws ActionException {
		log.debug("retrieving");

		SiteVO siteData = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		if (siteData == null) return;
		req.setAttribute("isKeystone", true); //passes through to CenterPageAction
		List<ApprovalVO> approvables = new ArrayList<>();
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		String siteAlias;
		if (franchiseId.equals(orgId)) {
			siteAlias = getDefaultSiteAlias();
		} else {
			siteAlias = getSiteAlias(franchiseId);
		}
		StringBuilder sql = new StringBuilder(190);
		sql.append("SELECT  ws.*, p.FULL_PATH_TXT ");
		sql.append("FROM WC_SYNC ws ");
		sql.append("left join PAGE p on p.page_id = ws.wc_key_id ");
		sql.append("WHERE ORGANIZATION_ID = ? and (MODULE_TYPE_ID = ? or ");
		sql.append("(MODULE_TYPE_ID=? and PORTLET_DESC=?))  and WC_SYNC_STATUS_CD not in (?,?,?) ");
		sql.append("ORDER BY PORTLET_DESC");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			//log.debug(franchiseId+"|"+orgId+"|"+franchiseId.equals(orgId));
			if (franchiseId.equals(orgId)) {
				ps.setString(1, franchiseId);
			} else {
				log.debug(orgId+"_"+franchiseId);
				ps.setString(1, orgId+"_"+franchiseId);
			}
			ps.setString(2, "Webedit");
			ps.setString(3, "Page");
			ps.setString(4, WebeditType.CenterPage.toString());
			ps.setString(5, SyncStatus.Approved.toString());
			ps.setString(6, SyncStatus.Declined.toString());
			ps.setString(7, SyncStatus.InProgress.toString());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				ApprovalVO app = new ApprovalVO(rs);
				WebeditType type = null;
				try {
					type = WebeditType.valueOf(app.getItemDesc());
				} catch (Exception e) {
					log.error("null or unknown type (portlet_desc) for syncId=" + app.getWcSyncId());
					continue;
				}
				
				switch(type) {
					case Career:
						app.setPreviewUrl("/careers?jobPostingId="+app.getWcKeyId());
						break;
					case CenterPage:
						log.debug("centerpage type");
						app.setPreviewUrl("/"+siteAlias+rs.getString("FULL_PATH_TXT"));
						break;
					default:app.setPreviewUrl("/"+siteAlias);;
				}
				
				String newSiteAlias = getModSpecificAlias(app.getWcKeyId());
				if (newSiteAlias != null && !siteAlias.equals(newSiteAlias)) {
					 app.setPreviewUrl("/"+newSiteAlias);
				} else {
					app.setPreviewUrl("");
				}
				 
				 approvables.add(app);
			}
		} catch(SQLException e) {
			log.error("Could not get list of approval items for " + franchiseId, e);
		}
		
		String previewApiKey = ApprovalController.generatePreviewApiKey(attributes);
		req.setParameter(Constants.PAGE_PREVIEW, previewApiKey);
		putModuleData(approvables);
	}
		
	/**
	 * This method takes a wc key id and finds a center where that particular 
	 * center page module is active to preview.
	 * @param wcKeyId
	 * @return
	 */
	private String getModSpecificAlias(String wcKeyId) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build Lookup Query
		StringBuilder sql = new StringBuilder(500);
		sql.append("SELECT fclmxr.FRANCHISE_ID from ");
		sql.append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR fcmfxr ");
		sql.append("inner join ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR fclmxr ");
		sql.append("on fcmfxr.CP_LOCATION_MODULE_XR_ID = fclmxr.CP_LOCATION_MODULE_XR_ID ");
		sql.append("inner join WebCrescendo_fs.dbo.WC_SYNC wc on (wc.WC_ORIG_KEY_ID = fcmfxr.CP_MODULE_OPTION_ID ");
		sql.append("or wc.WC_KEY_ID = fcmfxr.CP_MODULE_OPTION_ID) ");
		sql.append(" where wc.WC_KEY_ID = ? ");

		//Query for Franchise Id if available.
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, wcKeyId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) return rs.getString(1);
		} catch(SQLException e) {
			log.error("Could not get default location alias", e);
		}
		return null;
	}

	/**
	 * Get the default site alias for a center that has global modules enabled 
	 * so that changes to global assets can be properly previewed
	 */
	private String getDefaultSiteAlias() {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(220);
		sql.append("SELECT LOCATION_ALIAS_NM FROM DEALER_LOCATION dl left join ");
		sql.append(customDb).append("FTS_FRANCHISE f on f.FRANCHISE_ID = dl.DEALER_LOCATION_ID ");
		sql.append("WHERE USE_GLOBAL_MODULES_FLG = 1");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getString(1);
		} catch(SQLException e) {
			log.error("Could not get default location alias", e);
		}
		return "";
	}

	/**
	 * Get the site alias for the submitted franchise id
	 */
	private String getSiteAlias(String franchiseId) {
		StringBuilder sql = new StringBuilder(105);
		sql.append("select location_alias_nm from dealer_location where dealer_location_id=?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, franchiseId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getString(1);
		} catch(SQLException e) {
			log.error("Could not get location alias for " + franchiseId, e);
		}
		return "";
	}
	
}
