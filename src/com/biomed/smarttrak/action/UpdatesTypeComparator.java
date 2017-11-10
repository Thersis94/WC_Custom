package com.biomed.smarttrak.action;

import java.util.Comparator;

import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> UpdatesTypeComparator.java<br/>
 * <b>Description:</b> Compares Updates on the Updates Edition page - 
 * 		within a section we sort by Type, then order number, then date.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Oct 19, 2017
 ****************************************************************************/
public class UpdatesTypeComparator implements Comparator<UpdateVO> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(UpdateVO o1, UpdateVO o2) {
		//safety checks - these are nearly impossible scenarios in practical application
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		//First compare by type
		int typeNo = o1.getType().compareTo(o2.getType());
		if (typeNo != 0) return typeNo;

		//Second compare by order number
		if (o1.getOrderNo() != o2.getOrderNo()) {
			return Integer.valueOf(o1.getOrderNo()).compareTo(o2.getOrderNo());
		}

		//Same type and order - compare by date - newest first.
		int dates = o2.getPublishDate().compareTo(o1.getPublishDate());
		if (dates != 0) return dates;
		
		//If all else the same compare titles
		return StringUtil.checkVal(o1.getTitle()).compareTo(StringUtil.checkVal(o2.getTitle()));
	}
}