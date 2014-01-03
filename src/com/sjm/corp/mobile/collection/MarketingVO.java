package com.sjm.corp.mobile.collection;

import com.smt.sitebuilder.action.SBModuleVO;


/****************************************************************************
 * <b>Title</b>: MarketingVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM related to marketing and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class MarketingVO extends SBModuleVO{

	private static final long serialVersionUID = 1;
	private String marketingId;
	private MarketingWantsVO wants;
	private MarketingUsingVO using;
	
	public MarketingVO(){
		super();
		wants = new MarketingWantsVO();
		using = new MarketingUsingVO();
	}
	
	public String getMarketingId() {
		return marketingId;
	}

	public void setMarketingId(String marketingId) {
		this.marketingId = marketingId;
	}

	public MarketingWantsVO getWants() {
		return wants;
	}

	public void setWants(MarketingWantsVO wants) {
		this.wants = wants;
	}

	public MarketingUsingVO getUsing() {
		return using;
	}

	public void setUsing(MarketingUsingVO using) {
		this.using = using;
	}
}