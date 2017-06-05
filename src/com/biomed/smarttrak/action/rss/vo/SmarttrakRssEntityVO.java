package com.biomed.smarttrak.action.rss.vo;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.action.rss.RSSEntityVO;

/****************************************************************************
 * <b>Title:</b> SmarttrakRssEntityVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO for managing Smarttrak Custom Data related to an RSS
 * Source.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.0
 * @since May 18, 2017
 ****************************************************************************/
@Table(name="biomedgps_rss_entity")
public class SmarttrakRssEntityVO extends RSSEntityVO {

	private static final long serialVersionUID = -3100622634952315115L;
	private List<RSSFeedGroupVO> groups;
	private String configUrlTxt;
	private String feedTypeId;

	public SmarttrakRssEntityVO() {
		super();
		groups = new ArrayList<>();
	}

	public SmarttrakRssEntityVO(ActionRequest req) {
		super(req);
		groups = new ArrayList<>();
	}

	@Override
	public void setData(ActionRequest req) {
		super.setData(req);
		configUrlTxt = req.getParameter("configUrlTxt");
		feedTypeId = req.getParameter("feedTypeId");
	}

	@Column(name="config_url_txt")
	public String getConfigUrlTxt() {
		return configUrlTxt;
	}

	@Column(name="feed_type_id")
	public String getFeedTypeId() {
		return feedTypeId;
	}

	public List<RSSFeedGroupVO> getGroups() {
		return groups;
	}

	public void setConfigUrlTxt(String configUrlTxt) {
		this.configUrlTxt = configUrlTxt;
	}

	public void setFeedTypeId(String feedTypeId) {
		this.feedTypeId = feedTypeId;
	}
	@BeanSubElement
	public void addGroup(RSSFeedGroupVO g) {
		if(g != null) {
			groups.add(g);
		}
	}
	public void setGroups(List<RSSFeedGroupVO> groups) {
		this.groups = groups;
	}
}