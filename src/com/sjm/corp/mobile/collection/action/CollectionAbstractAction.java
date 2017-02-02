package com.sjm.corp.mobile.collection.action;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.sjm.corp.mobile.collection.MobileCollectionVO;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CollectionAbstractAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> An abstract class for the Mobile Collection Actions
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public abstract class CollectionAbstractAction extends SBActionAdapter {
	
	
	CollectionAbstractAction(){
	}
	
	/**
	 * Blocks the user from clicking the next button
	 * @param req
	 */
	protected void blockNext(ActionRequest req){
		req.setParameter("nextBlock", "true");
	}
	/**
	 * Blocks the user from clicking the back button
	 * @param req
	 */
	protected void blockBack(ActionRequest req){
		req.setParameter("backBlock", "true");
	}
	/**
	 * Unblocks a previously blocked next button
	 * @param req
	 */
	protected void unblockNext(ActionRequest req){
		req.setParameter("nextBlock", "");
	}
	/**
	 * Unblocks a perviously blocked back button
	 * @param req
	 */
	protected void unblockBack(ActionRequest req){
		req.setParameter("backBlock", "");
	}
	
	/**
	 * Wraps Integer.parseInt() to handle null pointer exceptions
	 * @param str
	 * @return
	 */
	public int parseInt(String str){
		int res;
		if(str != null && !str.isEmpty()){
			res = Integer.parseInt(str);
		}
		else {
			res = 0;
		}
		return res;
	}
	
	/**
	 * Method to quickly check if passed parameters have distinct values or not. Performs in O(n) time, due to the use of a HashMap here
	 * @param values
	 * @return
	 */
	public boolean checkIfDistinct(Object... values){
		if(values.length < 2){
			return true; // a single parameter is unique, as well as if we receive no parameters
		}
		Map<Object,Object> map = new HashMap<Object,Object>();
		int i = 0;
		for(Object o: values){
			if(map.containsValue(o)){
				return false; //If the current value is in the map, it's a duplicate, so we return false immediately
			}
			map.put(i,o);
			i++;
		}
		return true;
	 }
	/**
	 * Abstract update method to call
	 * @param req
	 * @param vo
	 */
	public abstract void update(ActionRequest req, MobileCollectionVO vo);

}
