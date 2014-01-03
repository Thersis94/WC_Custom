package com.codman.cu.tracking.vo;

import java.util.List;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;


/****************************************************************************
 * <b>Title</b>: AccountUnitReportVO<p/>
 * <b>Description: expands upon the default UnitReport to do data extraction 
 * 		before iterating the data</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 ****************************************************************************/
public class AccountUnitReportVO extends UnitReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;
	
	public AccountUnitReportVO(SiteVO site) {
        super(site);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Unit Report");
		StringBuilder rpt = new StringBuilder(this.getHeader());
		
		//loop the accounts, physicians, units, and requests
		for (AccountVO acct : data) {
			for (TransactionVO t : acct.getTransactions()) {
				for (UnitVO v : t.getUnits()) {
					v.setAccountName(acct.getAccountName());
					v.setRepName(StringUtil.checkVal(acct.getRep().getFirstName()) + " " + StringUtil.checkVal(acct.getRep().getLastName()));
					v.setPhysicianName(StringUtil.checkVal(t.getPhysician().getFirstName()) + " " + StringUtil.checkVal(t.getPhysician().getLastName()));
					rpt.append(formatUnit(v));
				}
			}
		}
		
		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
	}
	

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<AccountVO>) o;
	}
}
