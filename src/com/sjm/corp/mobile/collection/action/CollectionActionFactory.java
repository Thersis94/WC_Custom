package com.sjm.corp.mobile.collection.action;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: CollectionActionFactory.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Factory for a given page in the sjm mobile data collection portlet
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CollectionActionFactory {
	public Map<Integer, CollectionAbstractAction> pageAssoc; 
	/**
	 * Initialises the action's map/page associations
	 */
	public CollectionActionFactory(){
		pageAssoc = new HashMap<Integer, CollectionAbstractAction>();
		try{
			pageAssoc.put(1, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.FetchData").newInstance());
			pageAssoc.put(4, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdateGoals").newInstance());
			pageAssoc.put(5, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdatePatients").newInstance());
			pageAssoc.put(6, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdateMaketingUse").newInstance());
			pageAssoc.put(8, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdateThemes").newInstance());
			pageAssoc.put(10, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdateTemplates").newInstance());
			pageAssoc.put(11, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.UpdateMaketingWants").newInstance());
			pageAssoc.put(12, (CollectionAbstractAction)Class.forName("com.sjm.corp.mobile.collection.action.EndActions").newInstance());
		} catch(Exception e){
		}
	}

	public CollectionActionFactory(ActionRequest req){
	
	}
	/**
	 * returns whatever class/object is refered to by pageNumber
	 * @param pageNumber
	 * @return
	 */
	public CollectionAbstractAction getAction(int pageNumber){
		
		return pageAssoc.get(pageNumber);
	}
}
