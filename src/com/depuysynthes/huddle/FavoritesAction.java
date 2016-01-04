package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;

/****************************************************************************
 * <b>Title</b>: FavoritesAction.java<p/>
 * <b>Description: Extends core behavior to retrieve data needed for Views that isn't kept in Solr.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 30, 2015
 ****************************************************************************/
public class FavoritesAction extends MyFavoritesAction {

	public FavoritesAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public FavoritesAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	//TODO we may not need this action...it depends on whether Solrs gives us back what we 
	//need to render the Favorites page.

}
