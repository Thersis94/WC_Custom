package com.depuysynthes.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.commerce.product.ProductController;
import com.smt.sitebuilder.action.tools.PageViewReportingAction;
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;


/****************************************************************************
 * <b>Title</b>: HCPLandingPageAction.java<p/>
 * <b>Description: loads all the data for the HCP division landing pages.  (each has it's own)</b>
 * retrieve method calls to pageViewReportingAction as well as ProductController 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 9, 2013
 ****************************************************************************/
public class HCPLandingPageAction extends SBActionAdapter {
	
	public HCPLandingPageAction() {
		super();
	}

	public HCPLandingPageAction(ActionInitVO avo) {
		super(avo);
	}

	
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		super.delete(req);
		
        String sbActionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
        log.info("Starting HCPLandingPageAction Action - Delete: " + sbActionId);
        
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sb.append("DPY_SYN_HCP_LANDING where action_id = ?");

        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, sbActionId);
            if (ps.executeUpdate() < 1) 
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
            
        } catch (SQLException sqle) {
            log.error("Error deleting content: " + sbActionId, sqle);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
        } finally {
            if (mod != null) mod.setErrorMessage((String)msg);
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // Redirect the user
        moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//load the action's data via a quick call to list()
		req.setParameter(SB_ACTION_ID, actionInit.getActionId());
		this.list(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		HCPLandingPageVO vo = (HCPLandingPageVO) mod.getActionData();
		if (vo == null) vo = new HCPLandingPageVO();
		
		ProductController pc = new ProductController(actionInit);
		pc.setDBConnection(dbConn);
		pc.setAttributes(attributes);
		req.setParameter("format", "list"); //tells ProductController how we want the data returned
		
		//load-up the product details for the 10 products and 2 procedures to appear on this page
		vo.setSelProducts(this.loadProductDetails(pc, vo.getAdminSelProds(), req));
		vo.setSelProcedures(this.loadProductDetails(pc, vo.getAdminSelProcs(), req));
		
		/***********************************************************************
		 *          everything below here is for 'our most popular'            *
		 ***********************************************************************/
		
		//load PageView states
		PageViewReportingAction pva = new PageViewReportingAction(actionInit);
		pva.setDBConnection(dbConn);
		pva.retrieve(req);
		ModuleVO pageViewMod = (ModuleVO) pva.getAttribute(Constants.MODULE_DATA);
		Map<String, StatVO> pageViews = (Map<String, StatVO>)pageViewMod.getActionData();
		
		//if there's no pageView data everything else is a waste of time!  fail fast, here
		if (pageViews == null || pageViews.size() == 0) {
			super.putModuleData(vo);
			return;
		}
		
		//load the two catalogs
		ProductCatalogUtil pcl = new ProductCatalogUtil(actionInit);
		pcl.setAttributes(attributes);
		pcl.setDBConnection(dbConn);
		
		String[] tokens = pcl.separateIds((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		mod.addCacheGroup(tokens[0]); //the catalogId
		Tree products = pcl.loadCatalog(tokens[0], tokens[1], false, req);
		
		tokens = pcl.separateIds((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		mod.addCacheGroup(tokens[0]); //the catalogId
		Tree procedures = pcl.loadCatalog(tokens[0], tokens[1], false, req);
		
		//prune the catalogs down to remove categories (categories are not pages, only products apepar on pages)
		List<Node> divProds = filterCatalog(products);
		List<Node> divProcs = filterCatalog(procedures);
		
		//assign pageView #s to each product/procedure in this Division
		divProds = pcl.assignPageviewsToCatalog(divProds, pageViews, page.getFullPath() + "/products/" + attributes.get(Constants.QS_PATH));
		divProcs = pcl.assignPageviewsToCatalog(divProcs, pageViews, page.getFullPath() + "/procedures/" + attributes.get(Constants.QS_PATH));
		
		//re-sort the Lists according to most pageViews.
		//This call places the top 10 (or top 2), in order, onto the VO
		sortAndTrimProdList(divProds, vo);
		sortAndTrimProcList(divProcs, vo);
		log.debug("popProdcs=" + StringUtil.getToString(vo.getAdminPopProcs()));
		
		//load the Product details for the ones we need to display
		vo.setPopProducts(this.loadProductDetails(pc, vo.getAdminPopProds(), req));
		vo.setPopProcedures(this.loadProductDetails(pc, vo.getAdminPopProcs(), req));
		
		log.debug("popProdcs=" + StringUtil.getToString(vo.getAdminPopProcs()));
		
		mod.setActionData(vo);
		mod.setCacheTimeout(86400 *2); //refresh every 48hrs
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	/**
	 * sorts the passed List<Node> using a PageViewComparator (inner class)
	 * and then returns the top 10 (max 10) returns...the 10 most popular products in the Collection.
	 * @param data
	 * @return
	 */
	private void sortAndTrimProdList(List<Node> nodeData, HCPLandingPageVO vo) {
		//sort the new list by pageView counts
		Collections.sort(nodeData, new ProductCatalogUtil().new PageViewComparator());
		
		//take the top 10 products and put them into the VO to be looked-up in the next step.
		int limit = (nodeData.size() < 10) ? nodeData.size() : 10;
		for (int x=0; x < limit; x++) {
			Node n = nodeData.get(x);
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			vo.addAdminPopProd(Long.valueOf(cat.getNumProdAssoc()), n.getNodeId());
			//log.debug("added  popProd " + n.getNodeId() + "=" + cat.getNumProdAssoc());
		}
	}
	
	/**
	 * sorts the passed List<Node> using a PageViewComparator (inner class)
	 * and then returns the top 10 (max 10) returns...the 10 most popular products in the Collection.
	 * @param data
	 * @return
	 */
	private void sortAndTrimProcList(List<Node> nodeData, HCPLandingPageVO vo) {
		//sort the new list by pageView counts
		Collections.sort(nodeData, new ProductCatalogUtil().new PageViewComparator());
		
		//take the top 4 procedures and put them into the VO to be looked-up in the next step.
		int limit = (nodeData.size() < 4) ? nodeData.size() : 4;
		for (int x=0; x < limit; x++) {
			Node n = nodeData.get(x);
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			vo.addAdminPopProc(Long.valueOf(cat.getNumProdAssoc()), n.getNodeId());
			//log.debug("added  popProc " + n.getNodeId() + "=" + cat.getNumProdAssoc());
		}
	}
	


	
	/**
	 * prunes the catalog of anything that's not a true product (or procedure)
	 * @param catalog
	 * @param divisionName
	 * @return
	 */
	private List<Node> filterCatalog(Tree catalog) {
		//remove anything that's not a product (all the categories); we don't display categories
		List<Node> prodNodes = new ArrayList<Node>();
		for (Node n : catalog.preorderList()) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			if (cat.getProductId() == null) continue;
			prodNodes.add(n);
		}
		
		log.debug("filteredSize=" + prodNodes.size());
		return prodNodes;
	}
	
	
	/**
	 * calls the ProductController action to load product details for the pased productIds
	 * @param pc
	 * @param orderedProdIds
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<ProductVO> loadProductDetails(ProductController pc, 
			Map<String, Long> orderedProdIds, SMTServletRequest req) {
		List<ProductVO> products = new ArrayList<ProductVO>();
			
		try {
			String[] selIds = orderedProdIds.keySet().toArray(new String[orderedProdIds.size()]);
			//log.debug(StringUtil.getToString(selIds, false, true, ","));
			req.setParameter("productIds", selIds, true);
			pc.retrieve(req);
			ModuleVO mod = (ModuleVO) pc.getAttribute(Constants.MODULE_DATA);
			
			List<Node> prodNodes = (List<Node>) mod.getActionData();
			for (String prodId : orderedProdIds.keySet()) {
				Long lng = orderedProdIds.get(prodId);
				//loop the list of Nodes until we find the one we need
				//this is important to ensure proper ordering
			
				if (req.getParameter("pagePreview") != null && !req.getParameter("pagePreview").isEmpty()){
					 log.info(" preview page active");	
					 for (Node n : prodNodes) {
						 if (!n.getNodeId().equals(prodId)) {
							 ProductVO prodVo = (ProductVO) n.getUserObject();
							 if (prodVo.getProductGroupId() != null && prodVo.getProductGroupId().equals(prodId) ) {
								 prodVo.setDisplayOrderNo(lng.intValue());//set the pageView count
								 products.add(prodVo);
								 log.debug("##### = id added " + prodVo.getFullProductName() + " product id " + prodVo.getProductId() );
								 log.debug("####product size " + products.size());								 break;
							 }
						 }else if (n.getNodeId().equals(prodId)) {
							 ProductVO prodVo = (ProductVO) n.getUserObject();
							 prodVo.setDisplayOrderNo(lng.intValue());//set the pageView count
							 products.add(prodVo);
							 log.debug("####### = id added " + prodVo.getFullProductName() + " product id " + prodVo.getProductId() );
							 log.debug("#####product size " + products.size());
							 break;
						 }
					 }
				
				} else {
					log.info("preview not active");
					for (Node n : prodNodes) {
						if (n.getNodeId().equals(prodId)) {
							ProductVO prodVo = (ProductVO) n.getUserObject();
							prodVo.setDisplayOrderNo(lng.intValue());//set the pageView count
							products.add(prodVo);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("could not load products for " + StringUtil.getToString(orderedProdIds), e);
		}
		
		log.debug(" product size " + products.size());
		return products;
	}
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select *, b.action_id as built ");
		sql.append("from sb_action a inner join ").append(customDb).append("DPY_SYN_HCP_LANDING b on a.action_id=b.action_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_HCP_LANDING_PROD_XR c ");
		sql.append("on b.action_id=c.action_id ");
		sql.append("where a.action_id=? order by c.procedure_flg, c.order_no");
		log.debug(sql + " " + req.getParameter(SB_ACTION_ID));
		
		Long procCnt= Long.valueOf(0);
		Long prodCnt = Long.valueOf(0);
		HCPLandingPageVO vo = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter(SB_ACTION_ID));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (vo == null) { //happens once, for 'this' Portlet
					vo = new HCPLandingPageVO(rs);
					//vo.setActionId(rs.getString("action_id"));
					vo.setActionName(rs.getString("action_nm"));
					vo.setOrganizationId(rs.getString("organization_id"));
	                vo.setActionDesc(rs.getString("action_desc"));
	                vo.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
	                vo.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
	                vo.setActionGroupId(rs.getString("action_group_id"));
	                vo.setPendingSyncFlag(rs.getInt("pending_sync_flg"));
	                //vo.setEducationText(rs.getString("education_txt"));
	                //vo.setSupportText(rs.getString("support_txt"));
					vo.setBuilt(rs.getString("built") != null);
				}
				if (Convert.formatInteger(rs.getInt("PROCEDURE_FLG")) == 1) {
					//add procedure
					vo.addAdminSelProcedure(++procCnt, rs.getString("product_id"));
					log.debug(procCnt + " proc=" + rs.getString("product_id"));
					
				} else if (rs.getString("product_id") != null) {
					//add product
					vo.addAdminSelProduct(++prodCnt, rs.getString("product_id"));
					log.debug(prodCnt + " prod=" + rs.getString("product_id"));
				}
			}
			
		} catch (SQLException sqle) {
			log.error("could not load data", sqle);
		}
		
		super.putModuleData(vo);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
		
        // Build the sql
        StringBuffer sql = new StringBuffer();
		if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
	        sql.append("DPY_SYN_HCP_LANDING (education_txt, support_txt, tracking_no_txt, ");
			sql.append("create_dt, action_id) values (?,?,?,?,?)");
			
		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
	        sql.append("DPY_SYN_HCP_LANDING set education_txt=?, support_txt=?, ");
	        sql.append("tracking_no_txt=?, update_dt=? where action_id=?");
		}
		
		// perform the execute
		PreparedStatement ps = null;
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, req.getParameter("educationText"));
			ps.setString(2, req.getParameter("supportText"));
			ps.setString(3, req.getParameter("trackingNoText"));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, (String) req.getAttribute(SB_ACTION_ID));
			
            if (ps.executeUpdate() < 1) {
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
                log.warn("No records updated: " + ps.getWarnings());
            }

    		//now save the associated Products & Procedures
            //we only attempt this if the above query succeeded (no SQLException)
    		saveProductXRs(req);
    		
		} catch (SQLException sqle) {
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
            log.error("Error Update HCP_LANDING_PG", sqle);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		
		// Redirect after the update
        moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		Boolean isWizard = Convert.formatBoolean(req.getParameter("isWizard"));
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//Call the Super method which will copy the sb_action entry for the class.
		super.copy(req);
		
		//Build our RecordDuplicatorUtility and set the where clause
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DPY_SYN_HCP_LANDING", "ACTION_ID", isWizard);
		if (req.hasParameter(SB_ACTION_ID)) {
			rdu.addWhereClause(DB_ACTION_ID, (String)req.getParameter(SB_ACTION_ID));
		} else {
			rdu.setWhereSQL(rdu.buildWhereListClause(DB_ACTION_ID));
		}
		rdu.copy();
		
		//copy the DPY_SYN_HCP_LANDING_PROD_XR records in the action's child table
		rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DPY_SYN_HCP_LANDING_PROD_XR", "PRODUCT_XR_ID", isWizard);
		if (req.hasParameter(SB_ACTION_ID)) {
			rdu.addWhereClause(DB_ACTION_ID, (String)req.getParameter(SB_ACTION_ID));
		} else {
			rdu.setWhereSQL(rdu.buildWhereListClause(DB_ACTION_ID));
		}
		rdu.copy();
		
		// Redirect the user
		moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	
	/**
	 * saves the HCP_PRODUCT_XR entries tied to 'this' action.
	 * deletes all existing entries, then re-inserts via a batch stmt.
	 * @param req
	 * @throws ActionException
	 */
	private void saveProductXRs(SMTServletRequest req) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String actionId = (String) req.getAttribute(SB_ACTION_ID);
		String sql = "delete from " + customDb + "DPY_SYN_HCP_LANDING_PROD_XR where action_id=?";
		PreparedStatement ps = null;
		
		//delete the existing data
		ps = dbConn.prepareStatement(sql);
		ps.setString(1, actionId);
		ps.executeUpdate();
		try { ps.close(); } catch (Exception e) {}
		
		//process the re-insertions
		UUIDGenerator uuid = new UUIDGenerator();
		sql = "insert into " + customDb + "DPY_SYN_HCP_LANDING_PROD_XR (PRODUCT_XR_ID, ACTION_ID, " +
				"PRODUCT_ID, ORDER_NO, PROCEDURE_FLG, CREATE_DT) values (?,?,?,?,?,?)";
		ps = dbConn.prepareStatement(sql);
		
		//add Products
		int cnt = 0;
		String[] prods = req.getParameterValues("selProducts");
		if (prods == null) prods = new String[0];
		for (String prodId : prods) {
			if (prodId.length() == 0) continue;
			ps.setString(1, uuid.getUUID());
			ps.setString(2, actionId);
			ps.setString(3, prodId);
			ps.setInt(4, ++cnt);
			ps.setInt(5, 0);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		log.debug("added " + cnt + " products");
		
		//add Procedures
		cnt = 0;
		String[] procs = req.getParameterValues("selProcedures");
		if (procs == null) procs = new String[0];
		for (String prodId : procs) {
			if (prodId.length() == 0) continue;
			ps.setString(1, uuid.getUUID());
			ps.setString(2, actionId);
			ps.setString(3, prodId);
			ps.setInt(4, ++cnt);
			ps.setInt(5, 1);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		log.debug("added " + cnt + " procedures");
		
		
		//commit them all to the DB at once.
		ps.executeBatch();
		log.debug("done saving _XR to prods & procs");
	}

}
