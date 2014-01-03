package com.fastsigns.action.wizard;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.admin.action.data.PageModuleVO;

public interface FSSiteWizardIntfc {

	public abstract void insertOrReturn(SMTServletRequest req) throws Exception;

	/**
	 * This method checks to see if a franchise, organization, or site exists for the given id.
	 * @param id
	 * @throws SQLException
	 * @throws InvalidDataException
	 */
	public abstract void checkForExistingEntry(String id) throws SQLException,
			InvalidDataException, NumberFormatException;

	/**
	 * This method adds the redirects to the corporate site. Implemented in 
	 * localized site wizard. 
	 * @param fId
	 * @param alias
	 * @param req
	 * @throws ActionException
	 */
	public abstract void addRedirect(String fId, String alias,
			SMTServletRequest req) throws ActionException;

	/**
	 * This method adds the home page to the center site. Implemented in 
	 * localized site wizard.
	 * @param layoutId
	 * @param page
	 */
	public abstract void addHomePage(String layoutId, String fId,
			SMTServletRequest req) throws Exception;

	/**
	 * This method assigns the themes to the center site. Implemented in 
	 * localized site wizard.
	 * @param siteId
	 */
	public abstract void assignTheme(FranchiseVO vo) throws Exception;

	/**
	 * This method assists in adding page modules to the centers home page. 
	 * @param page
	 * @param centerActionId
	 * @throws SQLException
	 */
	public abstract void associateCenterPage(String layoutId, String fId,
			String centerActionId, int type) throws Exception;

	/**
	 * This method adds the secondary layout used by the center secondary
	 * pages.
	 * @param fId
	 * @return
	 */
	public abstract String addSecondaryLayout(SMTServletRequest req)
			throws Exception;

	/**
	 * This method returns the secondary layout for the center.
	 * @param siteId
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public abstract String getSecondaryLayoutId(String siteId, String name)
			throws Exception;

	/**
	 * Modifies the number of columns and the default column for the layout.
	 * @param fId
	 * @return GUID for the layout
	 */
	public abstract String updateLayout(String fId, String actionId)
			throws SQLException;

	/**
	 * Adds the franchise web site for the provided franchise.
	 * @param vo
	 * @throws IOException
	 */
	public abstract void addWebsite(FranchiseVO vo, SMTServletRequest req)
			throws Exception;
	
	/**
	 * This method assigns the page modules to the center pages. Implemented in 
	 * localized site wizard.
	 */
	public abstract void assignTypes();

	/**
	 * This method assists in creating the page modules.
	 * @param actionId
	 * @param displayPgId
	 * @param paramNm
	 * @param col
	 * @param order
	 * @return
	 */
	public abstract PageModuleVO makePageModule(boolean isPublic,
			boolean isRegistered, boolean isAdmin, String actionId,
			String displayPgId, String paramNm, int col, int order);

	/**
	 * This method associates the proper roles to the site. Implemented in 
	 * localized site wizard.
	 * @param isPublic
	 * @param isReg
	 * @param isAdmin
	 * @return
	 */
	public abstract Map<String, Integer> makeRoles(boolean isPublic,
			boolean isReg, boolean isAdmin);
	
	/**
	 * This method adds the center page action to the database and associates it.
	 * @param franchiseId
	 * @throws SQLException
	 */
	public abstract String addCenterPage(String franchiseId) throws SQLException;

}