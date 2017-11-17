package com.mindbody.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mindbodyonline.clients.api._0_5_1.MBResult;
import com.mindbodyonline.clients.api._0_5_1.StatusCode;
import com.mindbodyonline.clients.api._0_5_1.XMLDetailLevel;

/****************************************************************************
 * <b>Title:</b> MindBodyResponseVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 16, 2017
 ****************************************************************************/
public class MindBodyResponseVO {
	private StatusCode.Enum status;
	private XMLDetailLevel.Enum detailLevel;
	private String message;
	private int errorCode;
	private int resultCount;
	private int currentPageIndex;
	private int totalPageCount;
	private List<Object> results;

	/**
	 * 
	 */
	public MindBodyResponseVO() {
		results = new ArrayList<>();
	}

	public MindBodyResponseVO(MBResult res) {
		this();
		populateResponseFields(res);
	}

	/**
	 * @param res
	 */
	public void populateResponseFields(MBResult res) {
		this.status = res.getStatus();
		this.errorCode = res.getErrorCode();
		this.message = res.getMessage();
		this.detailLevel = res.getXMLDetail();
		this.resultCount = res.getResultCount();
		this.currentPageIndex = res.getCurrentPageIndex();
		this.totalPageCount = res.getTotalPageCount();
	}

	/**
	 * @return the status
	 */
	public StatusCode.Enum getStatus() {
		return status;
	}

	/**
	 * @return the detailLevel
	 */
	public XMLDetailLevel.Enum getDetailLevel() {
		return detailLevel;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * @return the resultCount
	 */
	public int getResultCount() {
		return resultCount;
	}

	/**
	 * @return the currentPageIndex
	 */
	public int getCurrentPageIndex() {
		return currentPageIndex;
	}

	/**
	 * @return the totalPageCount
	 */
	public int getTotalPageCount() {
		return totalPageCount;
	}

	/**
	 * @return the results
	 */
	public List<Object> getResults() {
		return results;
	}

	/**
	 * @param status the status to set.
	 */
	public void setStatus(StatusCode.Enum status) {
		this.status = status;
	}

	/**
	 * @param detailLevel the detailLevel to set.
	 */
	public void setDetailLevel(XMLDetailLevel.Enum detailLevel) {
		this.detailLevel = detailLevel;
	}

	/**
	 * @param message the message to set.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @param errorCode the errorCode to set.
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @param resultCount the resultCount to set.
	 */
	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	/**
	 * @param currentPageIndex the currentPageIndex to set.
	 */
	public void setCurrentPageIndex(int currentPageIndex) {
		this.currentPageIndex = currentPageIndex;
	}

	/**
	 * @param totalPageCount the totalPageCount to set.
	 */
	public void setTotalPageCount(int totalPageCount) {
		this.totalPageCount = totalPageCount;
	}

	/**
	 * @param results the results to set.
	 */
	public void addResults(Object... results) {
		this.results.add(Arrays.asList(results));
	}

	/**
	 * @return
	 */
	public boolean isValid() {
		return this.errorCode == 200;
	}
}