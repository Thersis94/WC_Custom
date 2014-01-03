package com.ansmed.sb.comparator;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RankActualVO;

/****************************************************************************
 * <b>Title</b>: ActualDollarsComparator.java<p/>
 * <b>Description: </b> Compares dollars property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 ****************************************************************************/

public class ActualDollarsComparator implements Comparator<RankActualVO> {
	
	public int compare(RankActualVO r1, RankActualVO r2) {
		
		// getDollars returns a float.  Casting to int because we don't
		// need the cents portion of the value.
		int dollars1 = (int)r1.getActualsData().getDollars();
		int dollars2 = (int)r2.getActualsData().getDollars();
		
		if (dollars1 < dollars2) {
			return 1;
		} else if (dollars1 > dollars2) {
			return -1;
		} else {
			return 0;
		}
	}
}
