package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTCookie;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;

/****************************************************************************
 * <b>Title</b>: FavoritesAction.java<p/>
 * <b>Description: Wraps core MyFavorites with support for sortOrder and perhaps other things.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 6, 2016
 ****************************************************************************/
public class FavoritesAction extends MyFavoritesAction {

	public FavoritesAction() {
		super();
	}

	public FavoritesAction(ActionInitVO arg0) {
		super(arg0);
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTCookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
		if (sort != null) req.setParameter("sort", sort.getValue());
		super.retrieve(req);
	}
}