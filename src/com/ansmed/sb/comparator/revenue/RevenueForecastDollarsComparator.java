package com.ansmed.sb.comparator.revenue;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RevenueActualVO;

/****************************************************************************
 * <p><b>Title</b>: RevenueForecastDollarsComparator.java<p/>
 * <p><b>Description: </b> Compares forecast dollars property which is the annual
 * revenue dollars forecast for a physician.<p/>
 * <p><b>Copyright:</b> Copyright (c) 2009<p/>
 * <p><b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 17, 2009
 ****************************************************************************/

public class RevenueForecastDollarsComparator implements Comparator<RevenueActualVO> {
	
	public int compare(RevenueActualVO r1, RevenueActualVO r2) {
		
		int target1 = r1.getForecastDollars();
		int target2 = r2.getForecastDollars();
		
		if (target1 < target2) {
			return 1;
		} else if (target1 > target2) {
			return -1;
		} else {
			return 0;
		}
	}
}
