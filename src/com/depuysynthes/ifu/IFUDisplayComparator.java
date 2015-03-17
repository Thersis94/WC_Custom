/**
 * 
 */
package com.depuysynthes.ifu;

import java.util.Comparator;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: IFUDisplayComparator.java<p/>
 * <b>Description: Compares two IFUs for public-side display, so they sort alphabetically
 * and then by version#.  This could not be done in the query because we need to group
 * like pieces of data together and iterate in a specific sequence.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 12, 2015
 ****************************************************************************/
public class IFUDisplayComparator implements Comparator<IFUDocumentVO> {

	public IFUDisplayComparator() {
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(IFUDocumentVO vo1, IFUDocumentVO vo2) {
		String name1 = StringUtil.checkVal(vo1.getTitleText());
		String name2 = StringUtil.checkVal(vo2.getTitleText());
		int nameComp = name1.compareTo(name2);
		
		//no need to compare versions if the names aren't equal
		if (nameComp != 0) return nameComp;
		
		//compare version#
		name1 = StringUtil.checkVal(vo1.getVersionText());
		name2 = StringUtil.checkVal(vo2.getVersionText());
		return name1.compareTo(name2);
	}
}