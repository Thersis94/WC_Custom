package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Looking up Franchise info ...");
		super.retrieve(req);
		
		String vcard = StringUtil.checkVal(req.getParameter("vcard"));
		String dli = StringUtil.checkVal(req.getParameter("dealerLocationId"));
		//if the request was for a VCard, it has already been handled by the call to super
		if (vcard.length() > 0 && dli.length() > 0) { 
			return;
		}
		
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		DealerLocatorVO loc = (DealerLocatorVO) mod.getActionData();
		try {
			if (loc.getResultCount() > 0)
				loc.setResults(getCustomFranchiseData(loc.getResults()));
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerLocatorAction#getDealerInfoContainer()
	 */
	@Override
	protected DealerLocationVO getDealerInfoContainer(ResultSet rs){
		return new FranchiseVO(rs);
	}
	
	/**
	 * Adds custom franchise data from the database view to the retrieved centers
	 * @param orig
	 * @return
	 * @throws SQLException
	 */
	public List<DealerLocationVO> getCustomFranchiseData(List<DealerLocationVO> orig)
	throws SQLException{
		Map<String,Object> franMap = new LinkedHashMap<>();
		List<DealerLocationVO> franList = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		sql.append("select dealer_location_id, franchise_id, use_raqsaf, country_cd ");
		sql.append("from FTS_FRANCHISE_INFO_VIEW ");
		sql.append("where dealer_location_id in (");
		for (int index = 0; index < orig.size(); index++){
			sql.append("?");
			if (index+1<orig.size())
				sql.append(",");
			//add the vo's to the map here so we don't have to iterate a second time
			franMap.put(orig.get(index).getDealerLocationId(), orig.get(index));
		}
		sql.append(")");
		
		log.debug(sql.toString());
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			for(DealerLocationVO vo:orig){
				ps.setString(++i, vo.getDealerLocationId());
			}
			
			ResultSet rs = ps.executeQuery();
			FranchiseVO fran = null;
			while (rs.next()){
				fran = (FranchiseVO) franMap.get(rs.getString("dealer_location_id"));
				fran.assignData(rs, false);
			}
			
			for(Object obj:franMap.values())
				franList.add((DealerLocationVO) obj);
			
		}
		return franList;
	}
}
