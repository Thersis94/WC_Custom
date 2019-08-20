package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;

import com.biomed.smarttrak.vo.AccountVO;

/****************************************************************************
 * <b>Title:</b> RSSBucketVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Used to manage Bucket User Data and store the articleCounts
 * for the user.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 2, 2019
 ****************************************************************************/
public class RSSBucketVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6901362866197122884L;
	private int articleCount;
	private AccountVO accountData;

	public RSSBucketVO() {
		super();
	}

	public RSSBucketVO(AccountVO accountData, int articleCount) {
		this.accountData = accountData;
		this.articleCount = articleCount;
	}

	public int getArticleCount() {
		return this.articleCount;
	}

	public void setArticleCount(int articleCount) {
		this.articleCount = articleCount;
	}

	public AccountVO getAccountData() {
		return this.accountData;
	}

	public void setAccountData(AccountVO accountData) {
		this.accountData = accountData;
	}
}
