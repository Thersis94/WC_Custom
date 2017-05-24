package com.biomed.smarttrak.action.rss;

import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title:</b> RSSDataAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom action manages for RSS article Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.0
 * @since May 9, 2017
 ****************************************************************************/
public class RSSDataAction extends SBActionAdapter {

	public enum ArticleStatus{N("New"), R("Rejected"), O("Other"), K("Kept");
		private String statusName;
		ArticleStatus(String statusName) {
			this.statusName = statusName;
		}

		public String getStatusName() {
			return statusName;
		}
	}

	/**
	 * 
	 */
	public RSSDataAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RSSDataAction(ActionInitVO actionInit) {
		super(actionInit);
	}
}