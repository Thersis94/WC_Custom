package com.biomed.smarttrak.action;

// Java 8
import java.util.List;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tools.FavoriteVO;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;

/*****************************************************************************
 <p><b>Title</b>: FavoritesAction.java</p>
 <p><b>Description: </b>Manages a user's 'favorites'.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 16, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class FavoritesAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public FavoritesAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public FavoritesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ActionInterface ai = new MyFavoritesAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		
		// retrieve off mod and parse.
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession sess = (SMTSession)req.getSession();
		List<FavoriteVO> favs = (List<FavoriteVO>)sess.getAttribute(MyFavoritesAction.MY_FAVORITES);
		if (favs == null) return;

		FavoriteVO fav = new FavoriteVO();
		fav.setTypeCd(req.getParameter("id"));
		fav.setTitle(req.getParameter("name"));
		fav.setUriTxt(req.getRequestURI());

		// add to top of list
		favs.add(0,fav);

		// if list size > max, remove 1
		if (favs.size() > QuickLinksAction.MAX_LIST_SIZE) 
			favs.remove(favs.size() - 1);

		// set update collection on session
		sess.setAttribute(MyFavoritesAction.MY_FAVORITES, favs);
	}
}
