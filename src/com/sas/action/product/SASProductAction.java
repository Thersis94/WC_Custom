package com.sas.action.product;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sas.util.WebServiceAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SASProductAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 21, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SASProductAction extends SBActionAdapter {

	/**
	 * 
	 */
	public SASProductAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public SASProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("********** Building");
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String cat1 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		String cat2 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		String cat3 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "3"));
		
		String cat = cat1;
		if (cat2.length() > 0) cat += "|" + cat2;
		if (cat3.length() > 0) cat += "|" + cat3;

		try {
			List<ProductCategoryVO> data = this.retrieveCat(req, cat);
			this.putModuleData(data, data.size(), false);
			log.debug("Cat1: " + cat1);
			if (data.size() == 0 && cat2.length() > 1 && ! "detail".equalsIgnoreCase(cat1)) {
				log.debug("retrieve product info");
				Collection<ProductVO> prods = this.retrieveProductList(req, cat);
				
				// if the data is still empty, get the group data
				if (prods.size() == 0) prods = this.getGroupInfo(req);
				
				this.putModuleData(prods, prods.size(), false);
				req.setAttribute("sasProdList", Boolean.TRUE);
			} else if (data.size() == 0 ) {
				log.debug("Get detail info here");
				ProductVO detail = this.retrieveProductDetail(req);
				this.putModuleData(detail, detail == null ? 0:1, false);
				req.setParameter(SMTServletRequest.PARAMETER_KEY + "3", cat3);
				req.setAttribute("sasProdDetail", Boolean.TRUE);
			}
			
			String zip = StringUtil.checkVal(req.getParameter("zip")); 
			if (zip.length() > 0) {
				// Load the cart.
				Map<String, Integer> prod = new HashMap<String, Integer>();
				prod.put(req.getParameter("productId"), 1);

				WebServiceAction wsa = new WebServiceAction(this.actionInit);
				wsa.setAttributes(attributes);
				Map<String, ShippingInfoVO> so = wsa.retrieveShippingInfo(zip, prod);
				req.setAttribute("shippingOptions", so);
			}
			
		} catch (Exception e) {
			log.error("Unable to retrieve catalog data", e);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	public Collection<ProductVO> getGroupInfo(SMTServletRequest req) 
	throws SQLException {
		String url = StringUtil.checkVal(req.getRequestURL().toString());
		String cat = url.substring(url.lastIndexOf("/") + 1);
		log.debug("Cat: " + cat);
		
		String s = "select * from product where PRODUCT_URL = ? and parent_id is not null AND PRODUCT_GROUP_ID IS NULL ";
		
		Map<String, ProductVO>  data = new LinkedHashMap<String, ProductVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, cat);

		ResultSet rs = ps.executeQuery();
		String inQuery = "'test'";

		for (int i=0; rs.next(); i++) {
			// set the page info
			if (i == 0)
				this.setPageData(req, rs.getString("title_nm"), rs.getString("meta_kywd_txt"), rs.getString("meta_desc"));
			
			String prodId = rs.getString("product_id");
			inQuery += ",'" + prodId + "'";
			data.put(prodId, new ProductVO(rs));
		}
		
		getProductAttributes(data, inQuery);
		return data.values();
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	public ProductVO retrieveProductDetail(SMTServletRequest req) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select * from product a ");
		s.append("inner join product_attribute_xr b on a.PRODUCT_ID = b.PRODUCT_ID ");
		s.append("inner join PRODUCT_ATTRIBUTE c on b.ATTRIBUTE_ID = c.ATTRIBUTE_ID ");
		s.append("inner join PRODUCT_CATALOG d on a.product_catalog_id = d.product_catalog_id ");
		s.append("and d.status_no = ? and d.organization_id=? where cust_product_no = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		log.debug("Prod Detail SQL: " + s + req.getParameter(SMTServletRequest.PARAMETER_KEY + "3"));
		
		String id = null;
		if (req.getParameter(SMTServletRequest.PARAMETER_KEY + "3") != null) {
			id = req.getParameter(SMTServletRequest.PARAMETER_KEY + "3");
		} else { 
			id = req.getParameter("productId");
		}
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setInt(1, ProductCatalogAction.STATUS_LIVE);
		ps.setString(2, "SAS");
		ps.setString(3, id);
		
		ResultSet rs = ps.executeQuery();
		ProductVO product = new ProductVO();
		while (rs.next()) {
			if (StringUtil.checkVal(product.getProductId()).length() == 0) 
				product = new ProductVO(rs);
			
			product.addProdAttribute(rs.getString("attribute_nm"), rs.getString("value_txt"));
		}
		log.debug(product);
		this.setPageData(req, product.getTitle(), product.getMetaKywds(), product.getMetaDesc());
		return product;
	}
	
	/**
	 * 
	 * @param req
	 * @param cat
	 * @return
	 * @throws SQLException
	 */
	public Collection<ProductVO> retrieveProductList(SMTServletRequest req, String cat) 
	throws SQLException {
		String cat4 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4"));
		StringBuilder s = new StringBuilder();
		s.append("select c.*, ");
		
		if (cat4.length() > 0) {
			s.append("c.title_nm as title, c.meta_desc as description, c.meta_kywd_txt as kywd ");
		} else {
			s.append("a.title_nm as title, a.meta_desc as description, a.meta_kywd_txt as kywd ");
		}
		s.append("from product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		
		if (cat4.length() > 0) {
			s.append("inner join product c on b.product_id = c.parent_id ");
		} else {
			s.append("inner join product c on b.product_id = c.product_id ");
		}
		s.append("inner join product_catalog d on c.product_catalog_id=d.product_catalog_id and ");
		s.append("d.status_no=? and d.organization_id=? ");
		s.append("where a.short_desc = ? ");
		
		if (cat4.length() > 0) s.append("and c.parent_id = '" + cat4 + "' ");
		s.append("order by product_nm ");
		log.debug("product list select: " + s + "|" + cat);
		log.debug("URL: " + cat + "|" + req.getRequestURL());
		
		int page = Convert.formatInteger(req.getParameter("page"),1); 
		int rpp = Convert.formatInteger(req.getParameter("rpp"),10);
		int start = (page - 1) * rpp + 1;
		int end = rpp * page;
		
		Map<String, ProductVO>  data = new LinkedHashMap<String, ProductVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setInt(1, ProductCatalogAction.STATUS_LIVE);
		ps.setString(2, "SAS");
		ps.setString(3, cat);
		
		ResultSet rs = ps.executeQuery();
		String inQuery = "'test'";
		int ctr = 0;
		for (int i=0; rs.next(); i++) {
			// set the page info
			if (i == 0)
				this.setPageData(req, rs.getString("title"), rs.getString("kywd"), rs.getString("description"));
			
			ctr = i + 1;
			if (ctr >= start && ctr <= end) {
				String prodId = rs.getString("product_id");
				inQuery += ",'" + prodId + "'";
				data.put(prodId, new ProductVO(rs));
			}
		}
		
		// Get the attributes and add the nav piece
		getProductAttributes(data, inQuery);
		req.setAttribute("prodNav", new NavManager(ctr, rpp, page, req.getRequestURL() + ""));
		
		return data.values();
	}
	
	/**
	 * 
	 * @param req
	 * @param title
	 * @param kywd
	 * @param desc
	 */
	public void setPageData(SMTServletRequest req, String title, String kywd, String desc) {
		PageVO sPage = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		sPage.setTitleName(title);
		sPage.setMetaDesc(kywd);
		sPage.setMetaKeyword(desc);
		req.setAttribute(Constants.PAGE_DATA, sPage);
	}
	
	/**
	 * 
	 * @param data
	 * @param inList
	 * @throws SQLException
	 */
	public void getProductAttributes(Map<String, ProductVO> data, String inList)
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select * from product_attribute a inner join ");
		s.append("product_attribute_xr b on a.attribute_id = b.attribute_id ");
		s.append("where product_id in (").append(inList).append(") ");
		s.append("order by product_id, a.attribute_id");
		log.debug("Attribute SQL: " + s);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			String prodId = rs.getString("product_id");
			data.get(prodId).addProdAttribute(rs.getString("attribute_id"), rs.getString("value_txt"));
		}
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<ProductCategoryVO> retrieveCat(SMTServletRequest req, String cat) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select a.category_nm,a.parent_cd, a.category_url, ");
		s.append("a.product_category_cd, a.title_nm, a.meta_desc, a.meta_kywd_txt, ");
		s.append("count(product_category_xr) as total, 1 as ordering  ");
		s.append("from product_category a ");
		s.append("inner join product_catalog c on a.product_catalog_id=c.product_catalog_id ");
		s.append("and c.status_no=? and c.organization_id=? ");
		s.append("left outer join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("where 1=1 ");
		if (cat.length() == 0) {
			s.append("and parent_cd is null ");
		} else {
			s.append("and PARENT_CD in ( ");
			s.append("select PRODUCT_CATEGORY_CD from PRODUCT_CATEGORY ");
			s.append("where SHORT_DESC=?) "); //TODO this is not a safe nested query!
		}
		s.append("group by a.category_nm,a.parent_cd, a.category_url, ");
		s.append("a.product_category_cd, a.title_nm, a.meta_desc, a.meta_kywd_txt  ");
		s.append("union ");
		s.append("select category_nm,parent_cd, category_url, product_category_cd, "); 
		s.append("title_nm, meta_desc, meta_kywd_txt, 0, 0  ");
		s.append("from PRODUCT_CATEGORY a ");
		s.append("inner join product_catalog b on a.product_catalog_id=b.product_catalog_id ");
		s.append("and b.status_no=? and b.organization_id=? ");
		s.append("where SHORT_DESC = ? ");
		s.append("order by ordering, category_nm ");
		
		log.debug("Prod Cat SQL: " + s + "|" + cat);
		
		List<ProductCategoryVO>  data = new ArrayList<ProductCategoryVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		int i=0;
		ps.setInt(++i, ProductCatalogAction.STATUS_LIVE);
		ps.setString(++i, "SAS");
		if (cat.length() > 0) ps.setString(++i, cat);
		ps.setInt(++i, ProductCatalogAction.STATUS_LIVE);
		ps.setString(++i, "SAS");
		ps.setString(++i, cat);
		
		ResultSet rs = ps.executeQuery();
		for (i=0; rs.next(); i++) {
			if (i == 0 && cat.length() > 0) {
				this.setPageData(req, rs.getString("title_nm"), rs.getString("meta_kywd_txt"), rs.getString("meta_desc"));
			} else {
				ProductCategoryVO vo = new ProductCategoryVO(rs);
				vo.setNumProdAssoc(rs.getInt("total"));
				data.add(vo);
			}
		}
		
		return data;
	}

}
