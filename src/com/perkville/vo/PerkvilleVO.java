package com.perkville.vo;

import java.util.List;

/****************************************************************************
 * <b>Title:</b> PerkvilleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Perkville ResponseData.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class PerkvilleVO<T extends Object> {

	private PerkvilleMetaVO meta;
	private List<T> objects;

	/**
	 * @return the meta
	 */
	public PerkvilleMetaVO getMeta() {
		return meta;
	}
	/**
	 * @return the objects
	 */
	public List<T> getObjects() {
		return objects;
	}
	/**
	 * @param meta the meta to set.
	 */
	public void setMeta(PerkvilleMetaVO meta) {
		this.meta = meta;
	}
	/**
	 * @param objects the objects to set.
	 */
	public void setObjects(List<T> objects) {
		this.objects = objects;
	}
}