package com.biomed.smarttrak.action;

//Java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//WC_Custom
import com.biomed.smarttrak.action.NoteAction.NoteType;
import com.biomed.smarttrak.vo.NoteVO;
import com.bmg.admin.vo.NoteEntityInterface;

import com.bmg.admin.vo.NoteInterface;
//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: NoteLoder.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> takes a list of VOs, and gets notes related to that VOs type
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 27, 2017<p/>
 * @updates:
 ****************************************************************************/
public class NoteLoader extends SimpleActionAdapter {

	private String userId = null;
	private List<String> teamIds = null;

	//the loader requires an smart track user id and a list of team ids. 
	public NoteLoader(String userId, List<String> teamIds) {
		super();
		this.userId= userId;
		this.teamIds = teamIds;
	}

	/**
	 * takes the VOs provided and loads any notes on to those VOs, sets type to company
	 * @param companyVOs
	 */
	public void addCompanyNotes(List<NoteEntityInterface> companyVOs){
		log.debug("add company notes called");

		loadNotes((List<NoteEntityInterface>) companyVOs, NoteType.COMPANY);
	}

	/**
	 * takes the VOs provided and loads any notes on to those VOs, sets type to product
	 * @param ProductVOs
	 */
	public void addProductNotes(List<NoteEntityInterface> productVOs){
		log.debug("add product notes called");

		loadNotes((List<NoteEntityInterface>) productVOs, NoteType.PRODUCT);
	}
	/**
	 * takes the VOs provided and loads any notes on to those VOs, sets type to market
	 * @param MarketVOs
	 */
	public void addMarketNotes(List<NoteEntityInterface> marketVOs){
		log.debug("add market notes called");

		loadNotes((List<NoteEntityInterface>) marketVOs, NoteType.MARKET);
	}

	/**
	 * takes the vo and a note type and processes the right note action methods for
	 * each type and sorts the notes into the matching vo.  
	 * @param company 
	 * @param companyVOs
	 * @param company
	 */
	private void loadNotes(List<NoteEntityInterface> targetVOs, NoteType type) {
		List<String>targetIds = new ArrayList<>();
		List<String> attributeIds = new ArrayList<>();
		if (targetVOs == null) return;

		for ( NoteEntityInterface vo : targetVOs){
			targetIds.add(vo.getId());
			
			List<? extends NoteInterface> results =  vo.getAttributes();

			for (NoteInterface vo2 : results){
				attributeIds.add(vo2.getId());
			}
		}

		NoteAction na = new NoteAction();	
		na.setDBConnection(dbConn);
		na.setAttributes(attributes);

		if (this.userId != null){

			Map<String, List<NoteVO>> results = na.getNotes(this.userId, this.teamIds, attributeIds, targetIds, type);

			if(results != null){
				attachNotes(targetVOs, results);
			}
		}
	}


	/**
	 * handles looping through the target VOs and adding lists of notes on to the 
	 * correct VO
	 * @param results 
	 * @param targetVOs 
	 * 
	 */
	private void attachNotes(List<NoteEntityInterface> targetVOs, Map<String, List<NoteVO>> results) {
		for (NoteEntityInterface vo : targetVOs) {
			if (results.containsKey(vo.getId())){
				log.debug("size of note list added to " + vo.getId() + " is " + results.get(vo.getId()).size());
				vo.setNotes(results.get(vo.getId()));
			}
			List<? extends NoteInterface> attriTargetVos = vo.getAttributes();
			attachAttributeNotes(attriTargetVos, results);
		}
	}

	/**
	 * loops attribute VOs if they exists and adds any notes from the results assigned to that attribute
	 * @param results 
	 * @param attriTargetVos 
	 */
	private void attachAttributeNotes(List<? extends NoteInterface> attriTargetVos, Map<String, List<NoteVO>> results) {
		if(attriTargetVos != null && results != null){
			for(NoteInterface avo : attriTargetVos ){
				log.debug("size of note list added to " + avo.getId() + " is " + results.get(avo.getId()).size());
				avo.setNotes(results.get(avo.getId()));
			}
		}
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the teamIds
	 */
	public List<String> getTeamIds() {
		return teamIds;
	}

	/**
	 * @param teamIds the teamIds to set
	 */
	public void setTeamIds(List<String> teamIds) {
		this.teamIds = teamIds;
	}
}
