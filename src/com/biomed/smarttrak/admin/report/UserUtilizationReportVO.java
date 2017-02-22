package com.biomed.smarttrak.admin.report;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;

	/**
	* Constructor
	*/
	public UserUtilizationReportVO() {
		// TODO Auto-generated constructor stub
		/* Sorted by company, showing:
		 * User Name
		 * User Title
		 * User Phone
		 * User Email
		 * User Update Email Preferences
		 * Monthly page views for last 12 months
		 * Summary at bottom of totals.
		 */
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		// TODO Auto-generated method stub

	}
	
}
