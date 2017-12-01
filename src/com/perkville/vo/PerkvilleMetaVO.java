package com.perkville.vo;

/****************************************************************************
 * <b>Title:</b> PerkvilleMetaVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Perkville Meta Response Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class PerkvilleMetaVO {

	private int limit;
	private int offset;
	/**
	 * @return the limit
	 */
	public int getLimit() {
		return limit;
	}
	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}
	/**
	 * @param limit the limit to set.
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}
	/**
	 * @param offset the offset to set.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
}