package com.depuysynthes.emea.leihsets;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
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
	public static final String ORG_PATH = "/org/DPY_SYN_EMEA/leihset_documents/";
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
		leihset, asset; 
	}

	public void list(SMTServletRequest req) throws ActionException {
		//LeihsetAction handles both Leihset and LeihsetAsset lists
		getAction(ActionType.leihset).list(req);
	}

	public void delete(SMTServletRequest req) throws ActionException {
		getAction(req.getParameter(AdminConstants.FACADE_TYPE)).delete(req);
	}

	public void update(SMTServletRequest req) throws ActionException {
		getAction(req.getParameter(AdminConstants.FACADE_TYPE)).update(req);
	}

	public void copy(SMTServletRequest req) throws ActionException {
		//LeihsetAction handles both Leihset and LeihsetAsset copies
		getAction(ActionType.leihset).copy(req);
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
	private SMTActionInterface getAction(String actionType) throws ActionException {
		ActionType at = null;
		try {
			at = ActionType.valueOf(actionType);
			return getAction(at);
		} catch (Exception e) {}

		throw new ActionException("Not a valid action type");
	}


	/**
	 * Determine which action should handle the request based on an enum token
	 * @param actionType
	 * @return
	 */
	private SMTActionInterface getAction(ActionType type) {
		SMTActionInterface ai = null;
		log.debug("Loading action " + type);
		switch(type) {
			case leihset:
				ai = new LeihsetAction(actionInit);
				break;
			case asset:
				ai =  new LeihsetAssetAction(actionInit);
				break;
		}

		if (ai != null) {
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
		}
		return ai;
	}
}