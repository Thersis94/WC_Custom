package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException{
		//get the locations from the superclass. We call the dealer locator action
		super.retrieve(req);
		
		//if the request was for a VCard, skip the rest of this method
		String vcard = StringUtil.checkVal(req.getParameter("vcard"));
		String dli = StringUtil.checkVal(req.getParameter("dealerLocationId"));
		if (vcard.length() > 0 && dli.length() > 0) { 
			return; 
		}
		
		//get the list of results from the superclass
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		DealerLocatorVO loc = (DealerLocatorVO) mod.getActionData();
		List<DealerLocationVO> dealerList = loc.getResults();
		
		//no need to process an empty location list
		if (dealerList.isEmpty())
			return;
		
		List<DealerLocationVO> franchiseList = null;
		//Add the custom franchise data to the vo's
		try{
			franchiseList = getCustomFranchiseData(dealerList);
		} catch(SQLException sqle){
			log.error(sqle);
			throw new ActionException(sqle);
		}
		
		//add module data
		loc.setResults(franchiseList);
		mod.setActionData(loc);
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Gets the custom data for the franchises in the list and adds it.
	 * @param orig
	 * @return
	 * @throws SQLException
	 */
	private List<DealerLocationVO> getCustomFranchiseData(List<DealerLocationVO> orig)
	throws SQLException{
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String,FranchiseVO> franMap = new HashMap<>();
		List<DealerLocationVO> franList = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(1050);
		sql.append("select dl.dealer_location_id, ff.*, fri.*, frb.*, fld.desc_txt, ");
		sql.append("fld.franchise_id as desc_franchise_id,fld.country_code,fld.location_desc_option_id ");
		sql.append("from dealer_location dl ");
		sql.append("inner join ").append(customDb).append("fts_franchise ff ");
		sql.append("on dl.dealer_location_id = ff.franchise_id ");
		sql.append("left outer join ").append(customDb).append("fts_location_desc_option fld ");
		sql.append("on fld.franchise_id=ff.franchise_id ");
		sql.append("left outer join ").append(customDb).append("fts_right_image fri ");
		sql.append("on ff.right_image_id = fri.right_image_id ");
		sql.append("left outer join ").append(customDb).append("fts_reseller_button frb ");
		sql.append("on ff.reseller_button_id = frb.reseller_button_id ");
		sql.append("where dl.dealer_location_id in (");
		for (int index=0; index<orig.size(); index++){
			sql.append("?");
			if (index+1 < orig.size())
				sql.append(",");
		}
		sql.append(") ");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i=0;
			for (DealerLocationVO vo:orig){
				ps.setString(++i, vo.getDealerLocationId());
				//used to map DealerLocationVO to FranchiseVO
				franMap.put(vo.getDealerLocationId(), (FranchiseVO) vo);
			}
			ResultSet rs = ps.executeQuery();
			
			FranchiseVO fran = null;
			while (rs.next()){
				//Add the data to the existing VO's rather than replacing them
				//(keep whatever data was populated from DealerLocator)
				fran = franMap.get(rs.getString("dealer_location_id"));
				fran.assignData(rs, false);
			}
		}
		//Put results back into the order that they came in (since this is a distance search)
		for (DealerLocationVO dlvo : orig){
			franList.add(franMap.get(dlvo.getDealerLocationId()));
		}
		
		return franList;
	}

}
