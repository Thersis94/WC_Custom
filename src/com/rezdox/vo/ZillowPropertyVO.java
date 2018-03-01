package com.rezdox.vo;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4J 1.2.17
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ZestimateVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object holding data for the zillow estimate
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/

public class ZillowPropertyVO extends BeanDataVO {
	
	/**
	 * Serial Version UID for serialization
	 */
	private static final long serialVersionUID = 631356729462912155L;
	private transient Logger log = Logger.getLogger(ZillowPropertyVO.class);
	
	// Member variables
	private String zillowId;
	private Integer valueEstimate;
	private Integer lowValueEstimate;
	private Integer highValueEstimate;
	private Date lastUpdated;
	private Integer valueChange;
	private double latitude;
	private double longitude;
	private Map<String, String> methodMapper = new HashMap<>(8);
	
	// Extended Data
	private Map<String, String> extendedData = new HashMap<>(16);
	
	/**
	 * 
	 */
	public ZillowPropertyVO() {
		super();
		
	}
	
	/**
	 * Takes the values from the XML and stores them into the 
	 * @param values
	 */
	public ZillowPropertyVO(Map<String, String> values) {
		this();
		assignMapper();
		List<String> delItems = new ArrayList<>();
		
		for (Map.Entry<String, String> item: values.entrySet()) {
			String methodName = methodMapper.get(item.getKey());
			if (methodName == null) continue;
			Method m = null;

			try {
				if ("last-updated".equalsIgnoreCase(item.getKey())) {
					m = this.getClass().getDeclaredMethod(methodName, Date.class);
					m.invoke(this, Convert.parseDateUnknownPattern(item.getValue()));
				} else {
					m = this.getClass().getDeclaredMethod(methodName, Integer.class);
					m.invoke(this, Convert.formatInteger(item.getValue()));
				}
				
				// Once the value has been assigned, add to the list for removal
				delItems.add(item.getKey());
			} catch (Exception e) {
				log.error("Unable to map values", e);
			}
		}
		
		// Remove assigned values
		for(String key : delItems) { values.remove(key); }
		
		// If there are any items left in the map, add them to the extended data set
		if (values.size() > 0) extendedData.putAll(values);
	}
	
	/**
	 * Assigns a setter to the Zillow key
	 */
	private void assignMapper() {
		methodMapper.put("amount", "setValueEstimate");
		methodMapper.put("last-updated", "setLastUpdated");
		methodMapper.put("valueChange", "setValueChange");
		methodMapper.put("low", "setLowValueEstimate");
		methodMapper.put("high", "setHighValueEstimate");
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * @return the zillowId
	 */
	public String getZillowId() {
		return zillowId;
	}

	/**
	 * @return the valueEstimate
	 */
	public Integer getValueEstimate() {
		return valueEstimate;
	}

	/**
	 * @return the lowValueEstimate
	 */
	public Integer getLowValueEstimate() {
		return lowValueEstimate;
	}

	/**
	 * @return the highValueEstimate
	 */
	public Integer getHighValueEstimate() {
		return highValueEstimate;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @return the valueChange
	 */
	public Integer getValueChange() {
		return valueChange;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	/**
	 * @param zillowId the zillowId to set
	 */
	public void setZillowId(String zillowId) {
		this.zillowId = zillowId;
	}

	/**
	 * @param valueEstimate the valueEstimate to set
	 */
	public void setValueEstimate(Integer valueEstimate) {
		this.valueEstimate = valueEstimate;
	}

	/**
	 * @param lowValueEstimate the lowValueEstimate to set
	 */
	public void setLowValueEstimate(Integer lowValueEstimate) {
		this.lowValueEstimate = lowValueEstimate;
	}

	/**
	 * @param highValueEstimate the highValueEstimate to set
	 */
	public void setHighValueEstimate(Integer highValueEstimate) {
		this.highValueEstimate = highValueEstimate;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @param valueChange the valueChange to set
	 */
	public void setValueChange(Integer valueChange) {
		this.valueChange = valueChange;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the extendedData
	 */
	public Map<String, String> getExtendedData() {
		return extendedData;
	}

	/**
	 * @param extendedData the extendedData to set
	 */
	public void setExtendedData(Map<String, String> extendedData) {
		this.extendedData = extendedData;
	}

}

