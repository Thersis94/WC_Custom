package com.fastsigns.action.franchise;

import java.sql.ResultSet;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorVO;

/****************************************************************************
 * <b>Title</b>: FranchiseLocatorAction.java <p/>
 * <b>Description: </b> Class used for including custom franchise information 
 * along with the standard Dealer Locator info. Used to avoid tampering with 
 * the intricacies of the core dealer locator.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since May 22, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseLocatorAction extends DealerLocatorAction {

	/**
	 * Default Constructor
	 */
	public FranchiseLocatorAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FranchiseLocatorAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#getDealerInfoContainer()
	 */
	@Override
	protected DealerLocationVO getDealerInfoContainer(ResultSet rs){
		return new FranchiseVO(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#getDealerInfoQuery(java.lang.String[])
	 */
	@Override
	protected String getDealerInfoQuery(String[] dlrLocnIds){
		
		StringBuilder sql = new StringBuilder();
		sql.append("select * from FTS_FRANCHISE_INFO_VIEW ");
		sql.append("where dealer_location_id in (''");
		for (int x=dlrLocnIds.length; x > 0; --x) sql.append(",?");
		sql.append(")");
		
		return sql.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#getLocationResultsQuery(com.smt.sitebuilder.action.dealer.DealerLocatorVO, int, com.siliconmtn.http.SMTServletRequest, java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	protected String getLocationResultsQuery(DealerLocatorVO locator, int type, 
			SMTServletRequest req, String country,String[] productIds, String locationName){
		Boolean useAttrib1Txt = Convert.formatBoolean((req.getParameter("useAttrib1Txt")));
		
		StringBuilder sb = new StringBuilder();
		sb.append("select fv.*, px.product_id from FTS_FRANCHISE_INFO_VIEW fv");
		sb.append("left join DEALER_LOCATION_PRODUCT_XR px on fv.dealer_location_id=px.dealer_location_id ");
		
		// append dealer types
		sb.append("where DEALER_TYPE_ID in (");
		for(int i = 0; i < locator.getDealerTypes().size(); i++){
			if(i > 0) sb.append(", ");
			sb.append("?");
		}		
		sb.append(") and PARENT_ID is null ");
		if (locator.getActiveOnlyFlag() == 1) sb.append("and ACTIVE_FLG = 1 ");
		if (locator.activePromotionsOnly()) sb.append(" and PROMOTIONS_FLG=1 ");
		// we need country code for all search types
		sb.append("and b.COUNTRY_CD = ? ");
		if (STATE_SEARCH_TYPE == type) sb.append("and STATE_CD = ? ");
		if (locationName.length() > 0)	 sb.append("and LOCATION_NM like ? ");
		if (useAttrib1Txt) sb.append("and ATTRIB1_TXT is not null and ATTRIB1_TXT != '' ");
		if (productIds.length > 1) {
			sb.append("and PRODUCT_ID in (");
			for (int i = 0; i < productIds.length; i++) {
				if (i > 0) sb.append(",");
				sb.append("?");
			}
			sb.append(") ");
		} else if (productIds.length > 0) {
			sb.append("and PRODUCT_ID = ? ");
		}
		sb.append("order by LOCATION_NM ");
		
		
		return sb.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#getResultsQuery(com.smt.sitebuilder.action.dealer.DealerLocatorVO, com.siliconmtn.http.SMTServletRequest, java.lang.String, java.lang.String[])
	 */
	@Override
	protected String getResultsQuery(DealerLocatorVO locator,SMTServletRequest req,
			String locationName,String[] productIds){
		Boolean useAttrib1Txt = Convert.formatBoolean((req.getParameter("useAttrib1Txt")));
		
		StringBuilder sb = new StringBuilder();
		sb.append("select dbo.geoCalcDistance(").append(locator.getSourceLatitude());
		sb.append(",").append(locator.getSourceLongitude());
		sb.append(", GEO_LAT_NO, GEO_LONG_NO, 'mi') as distance, fv.*, ");
		sb.append("da.ATTRIBUTE_NM, da.ATTRIBUTE_TXT, p.PRODUCT_ID ");
		sb.append("from FTS_FRANCHISE_INFO_VIEW fv ");
		sb.append("left outer join DEALER_LOCATION_ATTRIBUTE da on ");
		sb.append("da.DEALER_LOCATION_ID = fv.DEALER_LOCATION_ID ");
		sb.append("left outer join DEALER_LOCATION_PRODUCT_XR p on ");
		sb.append("p.DEALER_LOCATION_ID = fv.DEALER_LOCATION_ID ");
		sb.append("where DEALER_TYPE_ID in (");
				for (int i = 0; i < locator.getDealerTypes().size(); i++) {
					if (i > 0) sb.append(",");
					sb.append("?");
				}
		sb.append(") and ACTIVE_FLG=1 ");
		if (locator.activePromotionsOnly()) sb.append(" and PROMOTIONS_FLG=1 ");
		if (locationName.length() > 0)	sb.append("and LOCATION_NM like ? ");
		if (useAttrib1Txt) sb.append("and ATTRIB1_TXT is not null and ATTRIB1_TXT != '' ");
		if (productIds.length > 1) {
			sb.append("and PRODUCT_ID in (");
			for (int i = 0; i < productIds.length; i++) {
				if (i > 0) sb.append(",");
				sb.append("?");
			}
			sb.append(") ");
		} else if (productIds.length > 0) {
			sb.append("and PRODUCT_ID = ? ");
		}
		sb.append("order by distance ");
		
		return sb.toString();
	}
}
