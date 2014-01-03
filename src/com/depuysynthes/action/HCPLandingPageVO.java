package com.depuysynthes.action;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: HCPLandingPageVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 9, 2013
 ****************************************************************************/
public class HCPLandingPageVO extends SBModuleVO {
	private static final long serialVersionUID = 1L;
	
	private List<ProductVO> selProcedures = new LinkedList<ProductVO>();
	private List<ProductVO> selProducts = new LinkedList<ProductVO>();
	private List<ProductVO> popProcedures = new LinkedList<ProductVO>();
	private List<ProductVO> popProducts = new LinkedList<ProductVO>();
	
	private String educationText = null;
	private String supportText = null;
	private String trackingNoText = null;
	
	//these have to be Longs for JSTL reasons; they're used as Integers
	private Map<String, Long> adminSelProcs = new LinkedHashMap<String, Long>();
	private Map<String, Long> adminSelProds = new LinkedHashMap<String, Long>();
	private Map<String, Long> adminPopProcs = new LinkedHashMap<String, Long>();
	private Map<String, Long> adminPopProds = new LinkedHashMap<String, Long>();
	
	public HCPLandingPageVO() {
		super();
	}
	
	public HCPLandingPageVO(ResultSet rs) {
		this();
		DBUtil db = new DBUtil();
		educationText = db.getStringVal("education_txt", rs);
		supportText = db.getStringVal("support_txt", rs);
		trackingNoText = db.getStringVal("tracking_no_txt", rs);
		super.actionId = db.getStringVal("action_id", rs);
	}
	
	
	public List<ProductVO> getSelProcedures() {
		return selProcedures;
	}
	public void setSelProcedures(List<ProductVO> selProcedures) {
		this.selProcedures = selProcedures;
	}
	public void addSelProcedure(ProductVO p) {
		this.selProcedures.add(p);
	}
	public List<ProductVO> getSelProducts() {
		return selProducts;
	}
	public void setSelProducts(List<ProductVO> selProducts) {
		this.selProducts = selProducts;
	}
	public void addSelProduct(ProductVO p) {
		this.selProducts.add(p);
	}
	
	public String getEducationText() {
		return educationText;
	}
	public void setEducationText(String educationText) {
		this.educationText = educationText;
	}
	public String getSupportText() {
		return supportText;
	}
	public void setSupportText(String supportText) {
		this.supportText = supportText;
	}
		
	
	public List<ProductVO> getPopProcedures() {
		return popProcedures;
	}
	public void setPopProcedures(List<ProductVO> popProcedures) {
		this.popProcedures = popProcedures;
	}
	public void addPopProcedure(ProductVO p) {
		this.popProcedures.add(p);
	}
	
	
	public List<ProductVO> getPopProducts() {
		return popProducts;
	}
	public void setPopProducts(List<ProductVO> popProducts) {
		this.popProducts = popProducts;
	}
	public void addPopProduct(ProductVO p) {
		this.popProducts.add(p);
	}
	
	
	/*
	 * These 4 methods are for the admintool only; 
	 * they help us determine which products to "select" in the dropdowns
	 */
	
	public void addAdminSelProcedure(Long idx, String productId) {
		this.adminSelProcs.put(productId, idx);
	}

	public void addAdminSelProduct(Long idx, String productId) {
		this.adminSelProds.put(productId, idx);
	}

	public void addAdminPopProc(Long lng, String prodId) {
//		do {
//			++lng;
//		} while (adminPopProcs.containsKey(lng));
		
		this.adminPopProcs.put(prodId, lng);
	}
	
	public void addAdminPopProd(Long lng, String prodId) {
//		do {
//			++lng;
//		} while (adminPopProds.containsKey(lng));
		
		this.adminPopProds.put(prodId, lng);
	}

	public Map<String, Long> getAdminSelProcs() {
		return adminSelProcs;
	}

	public Map<String, Long> getAdminSelProds() {
		return adminSelProds;
	}
	
	public String[] getAdminSelProcsArr() {
		return adminSelProcs.keySet().toArray(new String[adminSelProcs.size()]);
	}

	public String[] getAdminSelProdsArr() {
		return adminSelProds.keySet().toArray(new String[adminSelProds.size()]);
	}

	public Map<String, Long> getAdminPopProcs() {
		return adminPopProcs;
	}

	public Map<String, Long> getAdminPopProds() {
		return adminPopProds;
	}

	public String getTrackingNoText() {
		return trackingNoText;
	}

	public void setTrackingNoText(String trackingNoText) {
		this.trackingNoText = trackingNoText;
	}

}
