package com.biomed.smarttrak.action.rss.util;

import java.util.ArrayList;
import java.util.List;

/***************************************************************************
 * <b>Title:</b> PubMedSearchResultVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages PubMed Search Results.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 22, 2017
 ***************************************************************************
 */
public class PubMedSearchResultVO {

	private int count;
	private int retMax;
	private int retStart;
	private int queryKey;
	private String webEnv;
	private List<Integer> idList;

	public PubMedSearchResultVO() {
		idList = new ArrayList<>();
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}


	/**
	 * @return the retMax
	 */
	public int getRetMax() {
		return retMax;
	}


	/**
	 * @return the retStart
	 */
	public int getRetStart() {
		return retStart;
	}


	/**
	 * @return the queryKey
	 */
	public int getQueryKey() {
		return queryKey;
	}


	/**
	 * @return the webEnv
	 */
	public String getWebEnv() {
		return webEnv;
	}


	/**
	 * @return the idList
	 */
	public List<Integer> getIdList() {
		return idList;
	}


	/**
	 * @param count the count to set.
	 */
	public void setCount(int count) {
		this.count = count;
	}


	/**
	 * @param retMax the retMax to set.
	 */
	public void setRetMax(int retMax) {
		this.retMax = retMax;
	}


	/**
	 * @param retStart the retStart to set.
	 */
	public void setRetStart(int retStart) {
		this.retStart = retStart;
	}


	/**
	 * @param queryKey the queryKey to set.
	 */
	public void setQueryKey(int queryKey) {
		this.queryKey = queryKey;
	}


	/**
	 * @param webEnv the webEnv to set.
	 */
	public void setWebEnv(String webEnv) {
		this.webEnv = webEnv;
	}


	/**
	 * @param idList the idList to set.
	 */
	public void setIdList(List<Integer> idList) {
		this.idList = idList;
	}


	/**
	 * @param id the id to add to idList
	 */
	public void addId(int id) {
		idList.add(Integer.valueOf(id));
	}
}