package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;

public class ActionPlanVO extends StageVO implements Serializable {

	private static final long serialVersionUID = 19873412363135315L;
	private String actionPlanId = null;
	private String hospitalInstId = null;
	private String headerText = null;
	private Map<String, ListItemVO> items = new LinkedHashMap<String, ListItemVO>();
	private ListItemVO caregiverItem = null;
	private ListItemVO featuredItem = null;
	
	public ActionPlanVO() {
		super();
	}
	
	public ActionPlanVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		setActionPlanId(db.getStringVal("action_plan_id", rs));
		setHospitalInstId(db.getStringVal("hospital_inst_id", rs));
		setHeaderText(db.getStringVal("header_txt", rs));
		db = null;
	}
	
	public ActionPlanVO(ActionRequest req) {
		super(req);
		if (req.hasParameter("actionPlanId")) setActionPlanId(req.getParameter("actionPlanId"));
		if (req.hasParameter("hospitalInstId")) setHospitalInstId(req.getParameter("hospitalInstId"));
		setHeaderText(req.getParameter("headerText"));
	}
	
	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public String getHospitalInstId() {
		return hospitalInstId;
	}

	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}

	public String getActionPlanId() {
		return actionPlanId;
	}

	public void setActionPlanId(String actionPlanId) {
		this.actionPlanId = actionPlanId;
	}

	public List<ListItemVO> getItems() {
		return new ArrayList<ListItemVO>(items.values());
	}
	
	public ListItemVO getItem(String itemId) {
		return items.get(itemId);
	}

	public void addItem(ListItemVO item) {
		//put the two special Items, in their special places (improves View efficiencies to do it now and cache it!)
		if (1 == item.getFeaturedFlg()) {
			setFeaturedItem(item);
		} else if (1 == item.getCaregiverFlg()) {
			setCaregiverItem(item);
		} else {
			items.put(item.getListItemId(), item);
		}
	}
	
	public ListItemVO getCaregiverItem() {
		return caregiverItem;
	}

	public void setCaregiverItem(ListItemVO caregiverItem) {
		this.caregiverItem = caregiverItem;
	}

	public ListItemVO getFeaturedItem() {
		return featuredItem;
	}

	public void setFeaturedItem(ListItemVO featuredItem) {
		this.featuredItem = featuredItem;
	}
}
