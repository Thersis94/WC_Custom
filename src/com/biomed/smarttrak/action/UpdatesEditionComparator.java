package com.biomed.smarttrak.action;

import java.util.Comparator;

import com.biomed.smarttrak.vo.UpdateVO;

/****************************************************************************
 * <b>Title:</b> UpdatesDateComparator.java<br/>
 * <b>Description:</b> Compares Updates on the Updates Edition page - 
 * 		within a section we sort by Type, then date (newest first).
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 8, 2017
 ****************************************************************************/
public class UpdatesEditionComparator implements Comparator<UpdateVO> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(UpdateVO o1, UpdateVO o2) {
		//safety checks - these are nearly impossible scenarios in practical application
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		//first compare by type
		int val = o1.getType().compareTo(o2.getType());
		if (val != 0) return val;

		//same type - compare by date - newest first.
		val = o1.getPublishDate().compareTo(o2.getPublishDate());
		if (val != 0) return val;

		//same dates - compare by order - lowest # first.
		return Integer.valueOf(o1.getOrderNo()).compareTo(o2.getOrderNo());
	}
}