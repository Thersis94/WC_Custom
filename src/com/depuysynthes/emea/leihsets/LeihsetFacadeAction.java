package com.depuysynthes.emea.leihsets;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LeihsetsFacadeAction.java<p/>
 * <b>Description: Data Tools controller for EMEA Leihsets.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 1, 2015
 ****************************************************************************/
public class LeihsetFacadeAction extends FacadeActionAdapter {

	public static final String MEDIABIN_PATH = "/json?amid=MEDIA_BIN_AJAX&mbid=";
	public static final String ORG_PATH = "/org/DPY_SYN_EMEA_DE/leihset_documents/";
	public static final String BINARY_PATH = "/binary" + ORG_PATH;


	public LeihsetFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public LeihsetFacadeAction() {
		super();
	}

	/**
	 * ActionType - supported behaviors of this facade
	 **/
	private enum ActionType {
		LEIHSET, ASSET, CATEGORY; 
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		//add a hook to save categories via ajax
		ActionType type = req.hasParameter("addCategory") ? ActionType.CATEGORY : ActionType.LEIHSET;	

		//LeihsetAction handles both Leihset and LeihsetAsset lists
		getAction(type).list(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		getAction(req.getParameter(AdminConstants.FACADE_TYPE)).delete(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		getAction(req.getParameter(AdminConstants.FACADE_TYPE)).update(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException {
		//LeihsetAction handles both Leihset and LeihsetAsset copies
		getAction(ActionType.LEIHSET).copy(req);
	}


	/**
	 * Write the new file for this Leihset to disk - used in both LeihsetAction and LeihsetAssetAction
	 * @param req
	 * @throws ActionException 
	 */
	protected String writeFile(FilePartDataBean file) throws ActionException {
		try {
			String path = (String)getAttribute(Constants.BINARY_PATH) + LeihsetFacadeAction.ORG_PATH;
			FileLoader fm = new FileLoader(getAttributes());
			fm.setPath(path);
			fm.writeFiles(file.getFileData(), path, file.getFileName(), true, false);
			log.debug("Wrote file to " + path + fm.getFileName());

			return fm.getFileName();
		} catch (Exception e) {
			log.error("Unable to upload file for leihset", e);
			throw new ActionException(e);
		}
	}


	/**
	 * Determine which action should handle the request based on a String value
	 * @param actionType
	 * @return
	 */
	private ActionInterface getAction(String actionType) throws ActionException {
		try {
			return getAction(ActionType.valueOf(actionType.toUpperCase()));
		} catch (Exception e) {
			throw new ActionException("Not a valid action type: " + actionType);
		}		
	}


	/**
	 * Determine which action should handle the request based on an enum token
	 * @param actionType
	 * @return
	 * @throws ActionException 
	 */
	private ActionInterface getAction(ActionType type) throws ActionException {
		log.debug("Loading action " + type);
		ActionInterface ai;
		switch(type) {
			case LEIHSET:
				ai = new LeihsetAction(actionInit);
				break;
			case ASSET:
				ai =  new LeihsetAssetAction(actionInit);
				break;
			case CATEGORY:
				ai = new LeihsetCategoryAction(actionInit);
				break;
			default:
				throw new ActionException("Not a valid action type");
		}

		ai.setAttributes(getAttributes());
		ai.setDBConnection(getDBConnection());
		return ai;
	}
}