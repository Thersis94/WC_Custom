package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;


public class TreatCalVO extends StageVO {
	
	private static final long serialVersionUID = 3772869815882652130L;
	private String TreatCalId = null;
	private String hospitalInstId = null;
	private Map<String, TreatCalItemVO> items = new LinkedHashMap<String, TreatCalItemVO>();
	private String summaryText = null;
	private String headerText = null;
	
	public TreatCalVO() {
		super();
	}
	
	public TreatCalVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		setTreatCalId(db.getStringVal("treat_cal_id", rs));
		setHospitalInstId(db.getStringVal("hospital_inst_id", rs));
		setSummaryText(db.getStringVal("summary_txt", rs));
		setHeaderText(db.getStringVal("header_txt", rs));
	}
	
	public TreatCalVO(SMTServletRequest req) {
		super(req);
		if (req.hasParameter("treatCalId")) setTreatCalId(req.getParameter("treatCalId"));
		if (req.hasParameter("hospitalInstId")) setHospitalInstId(req.getParameter("hospitalInstId"));
		setSummaryText(req.getParameter("summaryText"));
		setHeaderText(req.getParameter("headerText"));
	}
	
	public void addItem(TreatCalItemVO item) {
		items.put(item.getTreatCalItemId(), item);
	}
	
	/**
	 * @return the stages
	 */
	public List<TreatCalItemVO> getItems() {
		return new ArrayList<TreatCalItemVO>(items.values());
	}
	
	public TreatCalItemVO getItem(String itemId) {
		return items.get(itemId);
	}
	
	public boolean containsStage(String itemId) {
		return items.containsKey(itemId);
	}

	public String getTreatCalId() {
		return TreatCalId;
	}

	public void setTreatCalId(String treatCalId) {
		TreatCalId = treatCalId;
	}

	public String getHospitalInstId() {
		return hospitalInstId;
	}

	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}

	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

}
