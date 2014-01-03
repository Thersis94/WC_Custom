package com.ansmed.sb.comparator.revenue;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RevenueActualVO;

/****************************************************************************
 * <b>Title</b>: RevenuePermsComparator.java<p/>
 * <b>Description: </b> Compares perms property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 17, 2009
 ****************************************************************************/

public class RevenuePermsComparator implements Comparator<RevenueActualVO> {
	
	public int compare(RevenueActualVO r1, RevenueActualVO r2) {
		
		int perms1 = r1.getActualsData().getTotalPerms();
		int perms2 = r2.getActualsData().getTotalPerms();
		
		if (perms1 < perms2) {
			return 1;
		} else if (perms1 > perms2) {
			return -1;
		} else {
			return 0;
		}
	}
}
