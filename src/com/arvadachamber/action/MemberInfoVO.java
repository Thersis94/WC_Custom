package com.arvadachamber.action;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: MemberInfo.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 22, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MemberInfoVO extends DealerLocationVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<CategoryVO> categories = new ArrayList<CategoryVO>(); 
	private List<HotDealVO> deals = new ArrayList<HotDealVO>();
	private String hours = null;
	private String tollFree = null;
	private String keywords = null;
	private Date memberSince = null;
	private int hotDealsCount = 0;
	
	/**
	 * 
	 */
	public MemberInfoVO() {
		
	}

	/**
	 * @param rs
	 */
	public MemberInfoVO(ResultSet rs) {
		assignData(rs);
	}
	
	
	/**
	 * Assigns the DB data o the params
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		// Get the params from the parent class assigned
		super.setData(rs);
		
		DBUtil db = new DBUtil();
		hours = db.getStringVal("hours_txt", rs);
		memberSince = db.getDateVal("member_dt", rs);
		tollFree = db.getStringVal("toll_free_txt", rs);
		this.setLocationName(db.getStringVal("member_nm", rs));
		this.setDealerLocationId(db.getStringVal("member_id", rs));
		hotDealsCount = db.getIntegerVal("num_deals", rs);
		this.setLatitude(db.getDoubleVal("latitude_no", rs));
		this.setLongitude(db.getDoubleVal("longitude_no", rs));
		this.setPhone(db.getStringVal("primary_phone_txt", rs));
		this.setActiveFlag(db.getIntegerVal("member_status_flg", rs));
		this.setKeywords(db.getStringVal("keywords_txt", rs));
		
		// Add any categories
		this.addCategory(new CategoryVO(rs));
		
		// add any hot deals
		this.addHotDeal(new HotDealVO(rs));
	}
	
	/**
	 * Adds a single category to the list
	 * @param cat
	 */
	public void addHotDeal(HotDealVO deal) {
		if (deal != null && deal.getHotDealId() > 0)
			deals.add(deal);
	}
	
	/**
	 * Adds a single category to the list
	 * @param cat
	 */
	public void addCategory(CategoryVO cat) {
		if (cat != null && StringUtil.checkVal(cat.getCategoryId()).length() > 0)
			categories.add(cat);
	}
	
	/**
	 * 
	 * @return
	 */
	public CategoryVO getDefaultCategory() {
		if (categories == null) return null;
		CategoryVO cat = null;
		for (int i=0; i < categories.size(); i++) {
			CategoryVO tCat = categories.get(i);
			if (tCat.getPrimaryFlag() == 1) cat = tCat;
		}
		
		return cat;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasHotDeals() {
		if (deals != null && (deals.size() > 0 || hotDealsCount > 0)) 
			return true;
		else return false;
	}
	
	/**
	 * @return the categories
	 */
	public List<CategoryVO> getCategories() {
		return categories;
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(List<CategoryVO> categories) {
		this.categories = categories;
	}

	/**
	 * @return the deals
	 */
	public List<HotDealVO> getDeals() {
		return deals;
	}

	/**
	 * @param deals the deals to set
	 */
	public void setDeals(List<HotDealVO> deals) {
		this.deals = deals;
	}

	/**
	 * @return the hours
	 */
	public String getHours() {
		return hours;
	}

	/**
	 * @param hours the hours to set
	 */
	public void setHours(String hours) {
		this.hours = hours;
	}

	/**
	 * @return the tollFree
	 */
	public String getTollFree() {
		return tollFree;
	}

	/**
	 * @param tollFree the tollFree to set
	 */
	public void setTollFree(String tollFree) {
		this.tollFree = tollFree;
	}

	/**
	 * @return the keywords
	 */
	public String getKeywords() {
		return keywords;
	}

	/**
	 * @param keywords the keywords to set
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	/**
	 * @return the memberSince
	 */
	public Date getMemberSince() {
		return memberSince;
	}

	/**
	 * @param memberSince the memberSince to set
	 */
	public void setMemberSince(Date memberSince) {
		this.memberSince = memberSince;
	}

	/**
	 * @return the hasHotDeals
	 */
	public int getHotDealsCount() {
		return deals.size() > 0 ? deals.size() : hotDealsCount;
	}

}
