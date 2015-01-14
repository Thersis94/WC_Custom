package com.sjm.corp.locator.action;

import java.util.List;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerVO;

/****************************************************************************
 * <b>Title</b>: ClinicReportVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Accepts a list of DealerVOs as data and creates a csv report from them.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Dec 17, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicReportVO extends AbstractSBReportVO {
	
	private static final long serialVersionUID = 534458026072511157L;
	private List<DealerVO> clinics = null;

	public ClinicReportVO() {
		setContentType("text/csv");
		isHeaderAttachment(Boolean.TRUE);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuilder csv = new StringBuilder();
		addHeader(csv);
		// Loop over all locations for all dealers and add those to the csv
		if (clinics.size() > 0) {
			for(DealerVO vo : clinics) {
				for (String key : vo.getLocations().keySet()) {
					DealerLocationVO loc = vo.getLocations().get(key);
					csv.append(vo.getDealerName()).append(",");
					csv.append(vo.getDealerId()).append(",");
					csv.append(StringUtil.checkVal(loc.getFax())).append(",");
					csv.append(StringUtil.checkVal(loc.getWebsite())).append(",");
					csv.append(StringUtil.checkVal(loc.getPhone())).append(",");
					csv.append(StringUtil.checkVal(loc.getLocationName())).append(",");
					csv.append(StringUtil.checkVal(loc.getDealerLocationId())).append(",");
					csv.append(StringUtil.checkVal(loc.getEmailAddress())).append(",");
					csv.append(StringUtil.checkVal(loc.getAddress())).append(",");
					csv.append(StringUtil.checkVal(loc.getAddress2())).append(",");
					csv.append(StringUtil.checkVal(loc.getCountry())).append(",");
					csv.append(StringUtil.checkVal(loc.getCity())).append(",");
					csv.append(StringUtil.checkVal(loc.getZip())).append(",");
					csv.append(StringUtil.checkVal(loc.getState())).append("\n");
				}
			}
		}
		return csv.toString().getBytes();
	}
	
	/**
	 * Create the headers that SJM will be using for their clinics using the 
	 * verbiage specified in the dealer info action's upload csv file.
	 * @param csv
	 */
	private void addHeader(StringBuilder csv) {
		csv.append("* = required field\n");
		
		csv.append("Dealer Name,");
		csv.append("Dealer Id*,");
		csv.append("Fax Number,");
		csv.append("Location Website,");
		csv.append("Phone Number,");
		csv.append("Location Name,");
		csv.append("Dealer Location Id*,");
		csv.append("Email Address,");
		csv.append("Address 1,");
		csv.append("Address 2,");
		csv.append("Country Code,");
		csv.append("City,");
		csv.append("Zip Code,");
		csv.append("State\n");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof List<?>) {
			if (((List<?>)o).size() > 0 && ((List<?>)o).get(0) instanceof DealerVO) {
				clinics = (List<DealerVO>) o;
			}
		}
	}
}
