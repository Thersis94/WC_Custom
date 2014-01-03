package com.fastsigns.action.franchise.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smt.sitebuilder.action.gis.MapVO;


/****************************************************************************
 * <b>Title</b>: FranchiseContainer.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 19, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseContainer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer storeNumber = null;
	private List<ButtonVO> buttons	= new ArrayList<ButtonVO>();
	private FranchiseVO franchiseLocation = new FranchiseVO();
	private Map<String, CenterModuleVO> moduleData = new LinkedHashMap<String, CenterModuleVO>();
	private MapVO mapData = new MapVO();
	private Map<String, String> customVals = null;
	private FranchiseTimeVO times = null;
	private Object actionData = null;
	
	/**
	 * 
	 */
	public FranchiseContainer() {
		
	}
	
	/**
	 * 
	 * @param button
	 */
	public void addButton(ButtonVO button) {
		buttons.add(button);
	}

	/**
	 * @return the storeNumber
	 */
	public Integer getStoreNumber() {
		return storeNumber;
	}

	/**
	 * @param storeNumber the storeNumber to set
	 */
	public void setStoreNumber(Integer storeNumber) {
		this.storeNumber = storeNumber;
	}

	/**
	 * @return the buttons
	 */
	public List<ButtonVO> getButtons() {
		return buttons;
	}

	/**
	 * @param buttons the buttons to set
	 */
	public void setButtons(List<ButtonVO> buttons) {
		this.buttons = buttons;
	}

	/**
	 * @return the franchiseLocation
	 */
	public FranchiseVO getFranchiseLocation() {
		return franchiseLocation;
	}

	/**
	 * @param franchiseLocation the franchiseLocation to set
	 */
	public void setFranchiseLocation(FranchiseVO franchiseLocation) {
		this.franchiseLocation = franchiseLocation;
	}

	/**
	 * @return the moduleData
	 */
	public Map<String, CenterModuleVO> getModuleData() {
		return moduleData;
	}

	/**
	 * @param moduleData the moduleData to set
	 */
	public void setModuleData(Map<String, CenterModuleVO> moduleData) {
		this.moduleData = moduleData;
	}
	
	/**
	 * 
	 * @return
	 */
	public String[] getLocations() {
		String[] locations = new String[6];
		Set<String> s = moduleData.keySet();
		for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			String key = iter.next();
			CenterModuleVO vo = moduleData.get(key);
			locations[vo.getModuleLocationId() - 1] = key;
		}
		
		return locations;
	}

	/**
	 * @return the mapData
	 */
	public MapVO getMapData() {
		return mapData;
	}

	/**
	 * @param mapData the mapData to set
	 */
	public void setMapData(MapVO mapData) {
		this.mapData = mapData;
	}
	
	public Map<String, String> getCustomVals(){
		return customVals;
	}
	public void setCustomVals(Map<String, String> customVals){
		this.customVals = customVals;
	}

	/**
	 * @param times the times to set
	 */
	public void setTimes(FranchiseTimeVO times) {
		this.times = times;
	}

	/**
	 * @return the times
	 */
	public FranchiseTimeVO getTimes() {
		return times;
	}

	public void setActionData(Object actionData) {
		this.actionData = actionData;
	}

	public Object getActionData() {
		return actionData;
	}
}
