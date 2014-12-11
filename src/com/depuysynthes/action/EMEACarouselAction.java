/**
 * 
 */
package com.depuysynthes.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EMEACarouselAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Custom action to retrieve EMEA Products for Child sites.
 * Will provide Carousel like function on the landing pages but have no write
 * access to the product Catalog itself.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 3, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class EMEACarouselAction extends SBActionAdapter {

	public static final String CATALOG_ID ="DS_PRODUCTS_EMEA";
	/**
	 * 
	 */
	public EMEACarouselAction() {
		super();
	}
	
	public EMEACarouselAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//load the action's data via a quick call to list()
		req.setParameter(SB_ACTION_ID, actionInit.getActionId());
		this.list(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		HCPLandingPageVO vo = (HCPLandingPageVO) mod.getActionData();
		if (vo == null) vo = new HCPLandingPageVO();
		
		//Load the product Catalog
		ProductCatalogAction pc = new ProductCatalogAction(actionInit);
		pc.setDBConnection(dbConn);
		pc.setAttributes(attributes);
		Tree products = pc.loadEntireCatalog(CATALOG_ID, true, req);
		
		//Add the products to the VO
		vo.setSelProducts(parseCatalog(vo.getAdminSelProds(), products.preorderList()));
		
		mod.setActionData(vo);
		mod.setCacheTimeout(86400 *2); //refresh every 48hrs
		mod.addCacheGroup("DS_PRODUCTS_EMEA"); //Add 
		setAttribute(Constants.MODULE_DATA, mod);
	}	
	
	/**
	 * Handle retrieving the ProductVOs from the Catalog that are listed in the 
	 * orderedPRodIDs map.
	 * @param orderedProdIds
	 * @return
	 */
	private List<ProductVO>parseCatalog(Map<String, Long> orderedProdIds, List<Node> catalog) {
		List<ProductVO> selProds = new ArrayList<ProductVO>(orderedProdIds.size());
		Node parent = null;
		ProductVO p = null;
		
		//Loop over the map of products we want.
		for(String s : orderedProdIds.keySet()) {
			
			//loop over the catalog products
			for(Node n : catalog) {
				
				//if this is one of the root level products store the parent.
				if(n.getDepthLevel() < 3) {
					parent = n;
				} else if(n.getNodeId().equals(s)){
					
					/*
					 * Merge the Product Data from the Parent Node, ProductVO
					 * and place the result on the selProds List.
					 */
					ProductCategoryVO o = (ProductCategoryVO) n.getUserObject();
					if(o.getProducts().size() > 0) {
						p = o.getProducts().get(0);
						p.setProductName(n.getNodeName());
						p.addProdAttribute("category_url", ((ProductCategoryVO)parent.getUserObject()).getUrlAlias());
						selProds.add(p);
						break;
					}
				}
			}
		}
		return selProds;
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
					vo.setActionName(rs.getString("action_nm"));
					vo.setOrganizationId(rs.getString("organization_id"));
	                vo.setActionDesc(rs.getString("action_desc"));
	                vo.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
	                vo.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
	                vo.setActionGroupId(rs.getString("action_group_id"));
	                vo.setPendingSyncFlag(rs.getInt("pending_sync_flg"));
					vo.setBuilt(rs.getString("built") != null);
				}
				if (rs.getString("product_id") != null) {
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
	        sql.append("DPY_SYN_HCP_LANDING (create_dt, action_id) values (?,?)");
			
		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
	        sql.append("DPY_SYN_HCP_LANDING set update_dt=? where action_id=?");
		}
		
		// perform the execute
		PreparedStatement ps = null;
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, (String) req.getAttribute(SB_ACTION_ID));
			
            if (ps.executeUpdate() < 1) {
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
                log.warn("No records updated: " + ps.getWarnings());
            }

    		//now save the associated Products & Procedures
            //we only attempt this if the above query succeeded (no SQLException)
    		saveProductXRs(req);
    		
		} catch (SQLException sqle) {
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
            log.error("Error Update HCP_LANDING_PG for EMEACarousel", sqle);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		
		// Redirect after the update
        sbUtil.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
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
		
		
		//commit them all to the DB at once.
		ps.executeBatch();
		log.debug("done saving _XR to prods & procs");
	}

}
