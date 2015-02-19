package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.franchise.vo.MetroCategoryVO;
import com.fastsigns.action.franchise.vo.MetroProductVO;
import com.fastsigns.product.FSProductAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageParser;

/****************************************************************************
 * <b>Title</b>: MetroProductAction.java <p/>
 * <b>Description: </b> Seperates out the product related methods for the metro action
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Sep 15, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class MetroProductAction extends SBActionAdapter {
	
	/**
	 * deletes a metro area's product pages
	 * @param req
	 * @throws ActionException
	 */
	public void delProduct(SMTServletRequest req) throws ActionException {
		log.debug("deleting Metro Area Product: " + req.getParameter("metroProductId"));
		String s = "delete from " + (String)getAttribute(Constants.CUSTOM_DB_SCHEMA) + 
					"fts_metro_area_product where metro_product_id = ?";
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, req.getParameter("metroProductId"));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new ActionException("Unable to delete metro product", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
	}
	
	/**
	 * Get the MetroProductVOs for the metro area
	 * @param alias
	 * @param isLst
	 * @return
	 */
	public Map<String,MetroProductVO> getProductPages(String metroAreaId, boolean isAdminReq, String metroProdId) {
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("FTS_METRO_AREA_PRODUCT ").append("where metro_area_id = ? ");
		if (!isAdminReq) s.append("and visible_flg=1 "); //limit public's view to approved pages
		if (metroProdId != null) s.append("and metro_product_id=? ");
		s.append("order by order_no ");
		//log.debug(s + "|" + metroAreaId);
		
		Map<String,MetroProductVO> data = new HashMap<String,MetroProductVO>();
		log.debug("Metro SQL: " + s.toString() + " | " + metroAreaId + " | " + metroProdId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, metroAreaId);
			if (metroProdId != null) ps.setString(2, metroProdId); 
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				MetroProductVO vo = new MetroProductVO(rs);
				data.put(vo.getAliasNm(),vo);
			}
			
		} catch (SQLException sqle) {
			log.error("Unable to retrieve metro products for " + metroAreaId, sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}

		//log.debug("loaded " + data.size() + " metro product pages");
		return data;
	}
	
	/**
	 * Delete all the products for a metro area so that we can just add everything
	 * @param metroId
	 */
	public void deleteMetroProducts(String metroId) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE ").append(customDb).append("FTS_METRO_CATEGORY ");
		sql.append("WHERE METRO_AREA_ID = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareCall(sql.toString());
			
			ps.setString(1, metroId);
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			log.error("Unable to delete all products and categories for " + metroId, e);
		} finally {
			try{
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Get the products from the request and add them to the metro area
	 * @param req
	 */
	public void addMetroProducts(SMTServletRequest req) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String, MetroCategoryVO> prodList = buildProdMap(req);
		
		StringBuilder catSql = new StringBuilder(150);
		catSql.append("INSERT INTO ").append(customDb).append("FTS_METRO_CATEGORY ");
		catSql.append("(METRO_CATEGORY_ID, METRO_AREA_ID, CATEGORY_NM, CATEGORY_ALIAS, CATEGORY_DESC, TITLE_TXT, ");
		catSql.append("META_DESC, META_KYWD_TXT, ORDER_NO, CREATE_DT) ");
		catSql.append("VALUES (?,?,?,?,?,?,?,?,?,?)");
		
		StringBuilder prodSql = new StringBuilder(160);
		prodSql.append("INSERT INTO ").append(customDb).append("FTS_METRO_PRODUCT_XR ");
		prodSql.append("(METRO_PRODUCT_XR_ID, METRO_CATEGORY_ID, PRODUCT_ID, FULL_ALIAS, ORDER_NO, CREATE_DT) ");
		prodSql.append("VALUES (?,?,?,?,?,?)");

		PreparedStatement cat = null;
		PreparedStatement prod = null;
		try {
			cat = dbConn.prepareStatement(catSql.toString());
			prod = dbConn.prepareStatement(prodSql.toString());
			MetroCategoryVO vo = null;
			String catId;
			for (String key : prodList.keySet()) {
				vo = prodList.get(key);
				catId = new UUIDGenerator().getUUID();
				cat.setString(1, catId);
				cat.setString(2, req.getParameter("metroAreaId"));
				cat.setString(3, vo.getMetroCategoryNm());
				cat.setString(4, key);
				cat.setString(5, vo.getMetroCategoryDesc());
				cat.setString(6, vo.getTitleTxt());
				cat.setString(7, vo.getMetaDesc());
				cat.setString(8, vo.getMetaKywd());
				cat.setInt(9, vo.getOrderNo());
				cat.setTimestamp(10, Convert.getCurrentTimestamp());
				cat.addBatch();
				for (ProductVO p: vo.getProducts()) {
					prod.setString(1, new UUIDGenerator().getUUID());
					prod.setString(2, catId);
					prod.setString(3, p.getProductId());
					prod.setString(4, p.getProductUrl());
					prod.setInt(5, p.getDisplayOrderNo());
					prod.setTimestamp(6, Convert.getCurrentTimestamp());
					prod.addBatch();
				}
			}
			int cats[] = cat.executeBatch();
			int prods[] = prod.executeBatch();
			
			log.debug(cats.length +" Categories added. " + prods.length + " products added.");
			
		} catch (SQLException e) {
			log.error("Unable to add items", e);
		} finally {
			try {
				cat.close();
				prod.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Builds a map of the categories and their products.
	 * @param req
	 * @return
	 */
	private Map<String, MetroCategoryVO> buildProdMap(SMTServletRequest req) {
		String vals[];
		Map<String, MetroCategoryVO> prodList = new HashMap<String, MetroCategoryVO>();
		MetroCategoryVO vo;
		ProductVO p;
		StringEncoder se = new StringEncoder();
		for(int i=1; req.hasParameter("category"+i); i++) {
			vals = se.decode(req.getParameter("category" + i)).split("\\|");
			
			vo = new MetroCategoryVO();
			vo.setMetroCategoryAlias(vals[0]);
			vo.setMetroCategoryNm(vals[1]);
			vo.setTitleTxt(vals[2]);
			vo.setMetaDesc(vals[3]);
			vo.setMetaKywd(vals[4]);
			vo.setMetroCategoryDesc(vals[5]);
			vo.setOrderNo(Convert.formatInteger(vals[6]));
			prodList.put(vals[0], vo);
			
			//Grab the products for the category before we move on
			for(int j=1; req.hasParameter("product" + j + "c" + i); j++) {
				vals = se.decode(req.getParameter("product" + j + "c" + i)).split("\\|");
				p = new ProductVO();
				p.setDisplayOrderNo(Convert.formatInteger(vals[3]));
				p.setProductUrl(vals[1]);
				p.setProductId(vals[0]);
				prodList.get(vals[2]).addProduct(p);
			}
		}
		return prodList;
	}
	

	/**
	 * Prepare the list of product categories for the chosen metro area
	 * @param metroId
	 * @param orgId
	 * @return
	 * @throws ActionException
	 */
	public List<Node> getProductList(String metroId, String orgId) throws ActionException {
		FSProductAction prod = new FSProductAction();
		prod.setActionInit(actionInit);
		prod.setDBConnection(dbConn);
		
		Tree fullList = prod.loadEntireCatalog(getCatalogId(orgId));
		List<Node> metroProd = getProductCategories(metroId);
		List<String> prodUrls = getProductUrls(metroProd);
		List<Node> orderedList = fullList.preorderList();
		assignFullPath(orderedList);
		for (Node n : orderedList) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			int levels = StringUtil.checkVal(n.getFullPath()).split("/").length;

			//TODO - VALIDATE THAT METRO CHECK STILL WORKS WITH QS GONE!!!!!!
			Boolean hasQs = StringUtil.checkVal(n.getFullPath()).contains((String)attributes.get(Constants.QS_PATH));
			
			//Set the extra values, such as whether this is a subproduct or is part of the metro area
			if (((hasQs && levels == 6) || (!hasQs && levels == 3))) {
				cat.setAttrib1Txt("secondary");
			}
			if (prodUrls.contains(n.getFullPath())) {
				cat.setActiveFlag(1);
			}
		}
		orderedList.addAll(metroProd);
		return orderedList;
	}
	
	 /**
     * Assigns the full path based upon the pre-order list
     * @param data
     * @return
     */
    public void assignFullPath(List<Node> categories) {
		String fullPath = null;
    	Map<String, String> fp = new LinkedHashMap<String, String>();
    	for (Node n : categories) {
    		ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
    		
    		//omit all top-level PRODUCTS. they're not used on the Page's listing directly like this
    		if (n.getDepthLevel() == 2 && cat.getProductId() != null) 
    			continue;
    		
    		if (fp.containsKey(n.getParentId())) {
    			String prefix = fp.get(n.getParentId());
				String catUrl = StringUtil.checkVal(cat.getCustCategoryId()).contains("-") ? cat.getCustCategoryId() : cat.getUrlAlias() + "/";
				fullPath = prefix + catUrl;
        		fp.put(n.getNodeId(), fullPath);
        		
    		} else {
    			String preservedPath = null;
    			if (cat.getCustCategoryId() != null) {
    				//these are our abbreviated category URLs rewritten by Apache
    				fullPath = "/" + cat.getCustCategoryId() + cat.getUrlAlias();
    				preservedPath = "/" + cat.getCustCategoryId();
    			} else {
    				fullPath = "/" + cat.getUrlAlias() + "/" + attributes.get(Constants.QS_PATH);
    				preservedPath = fullPath;
    			}
    			fp.put(n.getNodeId(), preservedPath);
    		}
    		n.setFullPath(fullPath);
    	}
    }

	/**
	 * Get the active catalog id for the organization that we are on
	 * @param orgId
	 * @return
	 * @throws ActionException
	 */
	private String getCatalogId(String orgId) throws ActionException {
		String catId = null;
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT PRODUCT_CATALOG_ID FROM PRODUCT_CATALOG ");
		sql.append("WHERE ORGANIZATION_ID = ? AND STATUS_NO = 5");
		log.debug(sql+"|"+orgId);
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
		ps = dbConn.prepareStatement(sql.toString());
			
			ps.setString(1, orgId);
			
			rs = ps.executeQuery();
			
			if (rs.next()) {
				catId = rs.getString(1);
			} else {
				throw new ActionException("Unable to get catalog id for products.");
			}
		}  catch (SQLException e) {
			throw new ActionException("Unable to get catalog id for products.");
		}finally {
			try {
				ps.close();
				rs.close();
			} catch (Exception e) {}
		}
		
		return catId;
	}
	
	/**
	 * Get the list of categories and their products for the provided metro area
	 * @param metroAreaId
	 * @return
	 */
	public List<Node> getProductCategories(String metroAreaId) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(350);
		sql.append("SELECT fmc.*, fmpx.*, p.PRODUCT_NM, p.SHORT_DESC, p.IMAGE_URL FROM ").append(customDb).append("FTS_METRO_CATEGORY fmc ");
		sql.append("left join ").append(customDb).append("FTS_METRO_PRODUCT_XR fmpx ");
		sql.append("on fmpx.METRO_CATEGORY_ID = fmc.METRO_CATEGORY_ID ");
		sql.append("left join PRODUCT p on p.PRODUCT_ID = fmpx.PRODUCT_ID ");
		sql.append("where fmc.metro_area_id = ? ORDER BY fmc.ORDER_NO, fmpx.ORDER_NO");
		log.debug(sql.toString()+"|"+metroAreaId);

		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Node> data = new ArrayList<Node>();
		MetroCategoryVO vo = null;
		ProductVO prod = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			ps.setString(1, metroAreaId);
			
			rs = ps.executeQuery();
			
			String catUrl = null;
			Node n;
			while (rs.next()) {
				if (!StringUtil.checkVal(rs.getString("CATEGORY_ALIAS")).equals(catUrl)) {
					if (vo != null) {
						log.debug("Added " + vo.getMetroCategoryNm());
						n = new Node();
						n.setUserObject(vo);
						data.add(n);
					}
					vo  = new MetroCategoryVO(rs);
					catUrl = vo.getMetroCategoryAlias();
				}
				if (rs.getString("FULL_ALIAS") == null) continue;
				prod = new ProductVO(rs);
				prod.addProdAttribute("fullUrl", rs.getString("FULL_ALIAS"));
				
				prod.setParentId(parseParent(rs.getString("FULL_ALIAS")));
				vo.addProduct(prod);
			}
			
			// Add the trailing node
			n = new Node();
			n.setUserObject(vo);
			data.add(n);
			
		} catch (SQLException e) {
			log.error("Could not retrieve product pages", e);
		} finally {
			try {
				rs.close();
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}

	/**
	 * Get the parent category url
	 * TODO: Need to investigate effect of removing qs from the URI here.
	 * @param url
	 * @return
	 */
	private String parseParent(String url) {
		int levels = url.split("/").length;
		Boolean hasQs = url.contains((String)attributes.get(Constants.QS_PATH));
		if (hasQs) {
			int first = url.indexOf("/")+1;
			return url.substring(first, url.lastIndexOf("/", url.lastIndexOf("/")-1));
		} else if (levels == 3){
			return url.substring(0, url.indexOf("/", 1)+1);
		} else {
			return url.substring(0, url.indexOf("-")+1);
		}
	}
	
	/**
	 * handles the "update" behavior when someone edits an existing metro product page
	 * no template loading, no freemarker...just save it to the database
	 * @param req
	 */
	public void editProduct(SMTServletRequest req) {
		MetroProductVO p = new MetroProductVO(req);
		this.saveMetroProduct(p, false);
	}

	/**
	 * Set the full url of each product in the list 
	 * @param metroProd
	 * @return
	 */
	private List<String> getProductUrls(List<Node> metroProd) {
		List<String> urlList = new ArrayList<String>();
		for(Node n : metroProd) {
			if (n.getUserObject() == null) continue;
			for (ProductVO p : ((MetroCategoryVO)n.getUserObject()).getProducts()) {
				urlList.add(StringUtil.checkVal(p.getProdAttributes().get("fullUrl")));
			}
		}
		return urlList;
	}
	
	/**
	 * retrieves the metroProduct template from the shared org, 
	 * performs freemarker replacements as appropriate
	 * calls to insert the record into the _metro_product table.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public String addProduct(SMTServletRequest req) throws ActionException {
		log.debug("adding Metro Area Product");
		String metroProductId = new UUIDGenerator().getUUID();
		MetroProductVO pg = new MetroProductVO();

		StringBuilder sql = new StringBuilder();
		sql.append("select * from sb_action a inner join content b on a.action_id=b.action_id and a.action_id=?");

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("productActionId"));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				pg.setMetroProductId(metroProductId);
				pg.setMetroAreaId(req.getParameter("metroAreaId"));
				pg.setProductNm(rs.getString("action_desc"));
				pg.setAliasNm(StringUtil.replace(rs.getString("action_nm"), "metroProductTemplate-", ""));
				pg.setMetaDesc(rs.getString("intro_txt"));
				pg.setBodyTxt(rs.getString("article_txt"));
				pg.setTitleTxt(rs.getString("attrib1_txt"));
				pg.setVisibleFlg(Boolean.TRUE);
				
			} else {
				throw new SQLException();
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException("Template not found " + req.getParameter("productActionId"));
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//parse through Freemarker to customize the template
		Map<String, Object> vals = new HashMap<String, Object>();
		//add the tags that need replaced to the Map.
		vals.put("metroAlias", req.getParameter("metroName"));
		vals.put("metroLocation", req.getParameter("metroLocation"));
		
		try {
			pg.setBodyTxt(MessageParser.getParsedMessage(pg.getBodyTxt(), vals, "metroProd_" + pg.getAliasNm() + "_body").toString());
			pg.setMetaDesc(MessageParser.getParsedMessage(pg.getMetaDesc(), vals, "metroProd_" + pg.getAliasNm() + "_mDesc").toString());
			pg.setTitleTxt(MessageParser.getParsedMessage(pg.getTitleTxt(), vals, "metroProd_" + pg.getAliasNm() + "_title").toString());
		} catch (Exception e) {
			log.error("could not make freemarker replacements", e);
		}
		
		//insert the new metro product page
		this.saveMetroProduct(pg, true);
	
		return metroProductId;
	}
	
	/**
	 * adds or updates the Metro Product record
	 * @param pg
	 * @param isInsert
	 */
	private void saveMetroProduct(MetroProductVO pg, boolean isInsert) {
		log.debug("saving metro product");
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		
		if (isInsert) {
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("fts_metro_area_product (metro_area_id, product_nm, ");
			sql.append("alias_txt, body_txt, meta_desc, visible_flg, order_no, create_dt,");
			sql.append("title_txt, metro_product_id) values (?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("fts_metro_area_product set metro_area_id=?, product_nm=?, ");
			sql.append("alias_txt=?, body_txt=?, meta_desc=?, visible_flg=?, order_no=?, update_dt=?, ");
			sql.append("title_txt=? where metro_product_id=?");
		}
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, pg.getMetroAreaId());
			ps.setString(2, pg.getProductNm());
			ps.setString(3, pg.getAliasNm());
			ps.setString(4, pg.getBodyTxt());
			ps.setString(5, pg.getMetaDesc());
			ps.setInt(6, pg.getVisibleFlg() ? 1 : 0);
			ps.setInt(7, pg.getOrderNo());
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setString(9, pg.getTitleTxt());
			ps.setString(10, pg.getMetroProductId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
}