package com.mts.admin.action;


import static com.smt.sitebuilder.admin.action.SiteAuthManageAction.LOGIN_MODULE_XR_ID;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mts.common.MTSConstants;
import com.mts.security.SSOProviderVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.admin.action.SiteAuthManageAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.LoginConfigurationVO;
import com.smt.sitebuilder.util.WCConfigUtil;

/****************************************************************************
 * <p><b>Title:</b> SSOProviderAction.java</p>
 * <p><b>Description:</b> Manages the SSO customers who have access to the platform.
 * Proxies calls to login_module_xr to register login modules to the website.
 * Note: this action is only invoked over ajax calls - its not registered on a page directly.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 26, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SSOProviderAction extends SimpleActionAdapter {

	public SSOProviderAction() {
		super();
	}

	public SSOProviderAction(ActionInitVO init) {
		super(init);
	}


	/**
	 * @param smtdbConnection
	 * @param attributes
	 */
	public SSOProviderAction(SMTDBConnection dbConn, Map<String, Object> attrs) {
		this();
		setDBConnection(dbConn);
		setAttributes(attrs);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("isAuthForm")) {
			loadAuthDetails(req);
		} else {
			loadProviders(req);
		}
	}


	/**
	 * Load the authentication details from wc_config for the given loginModuleXrId
	 * @param req
	 */
	private void loadAuthDetails(ActionRequest req) {
		String lmXrId = req.getParameter("loginModuleXrId");
		String classNm = MTSConstants.SAML_LOGIN_MODULE_CLASSPATH;
		String prefix = LoginConfigurationVO.getContextKey(classNm, lmXrId);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		putModuleData(WCConfigUtil.getConfig(site, prefix));
	}


	/**
	 * Put the list of providers into moduleData
	 * @param req
	 */
	private void loadProviders(ActionRequest req) {
		String schema = getCustomSchema();
		String sql = getRetrieveSql(schema, null);

		BSTableControlVO bst = new BSTableControlVO(req, SSOProviderVO.class);
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		GridDataVO<SSOProviderVO> providers = db.executeSQLWithCount(sql, null, new SSOProviderVO(), bst);

		// merge the publication names
		if (providers.getTotal() > 0)
			mergePublicationNames(providers, db, schema);

		setModuleData(providers);
	}


	/**
	 * @param schema
	 * @param object
	 * @return
	 */
	private String getRetrieveSql(String schema, String loginModuleXrId) {
		StringBuilder  sql = new StringBuilder(500);
		sql.append("select s.*, r.role_nm, lm.active_flg, lm.qualifier_pattern_txt from ").append(schema);
		sql.append("mts_sso s ");
		sql.append("left join role r on s.user_role_id=r.role_id ");
		sql.append("left join login_module_xr lm on s.site_login_module_xr_id=lm.login_module_xr_id ");
		if (!StringUtil.isEmpty(loginModuleXrId)) sql.append("where s.site_login_module_xr_id=? ");
		sql.append("order by s.provider_nm");
		log.debug(sql + loginModuleXrId);
		return sql.toString();
	}

	/**
	 * retrieve a list of publication names, them merge them into the providers for cosmetic use
	 * @param providers
	 */
	private void mergePublicationNames(GridDataVO<SSOProviderVO> providers, DBProcessor db, String schema) {
		Map<String, String> publications = new HashMap<>();
		String sql = StringUtil.join("select publication_id as key, publication_nm as value from ", schema, "mts_publication");
		log.debug(sql);
		MapUtil.asMap(publications, db.executeSelect(sql, null, new GenericVO()));

		//let each bean take what it needs from the source map
		for (SSOProviderVO vo : providers.getRowData())
			vo.setPublicationNames(publications);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean saveLoginModule = req.hasParameter("saveLoginModule");
		SSOProviderVO vo = new SSOProviderVO(req);

		//call both on deletes, otherwise toggle
		if (req.hasParameter("isDelete")) {
			//don't delete the auth details if there's no pkId given (there's no data there anyways)
			if (req.hasParameter(LOGIN_MODULE_XR_ID))
				saveLoginModule(req, true);

			saveSSORecord(vo, true);

		} else if (saveLoginModule) {
			saveLoginModule(req, false);

			//save the created key as a foreign key in our mts_sso table
			if (StringUtil.isEmpty(vo.getLoginModuleXrId()))
				saveLMForeignKey(vo.getSsoId(), req.getParameter(LOGIN_MODULE_XR_ID));

		} else {
			saveSSORecord(vo, false);

			//if the role changed, we need to update the login module too
			if (req.hasParameter("oldRoleId") && !vo.getRoleId().equals(req.getParameter("oldRoleId")))
				cascadeRoleChange(vo);
		}
	}


	/**
	 * Save the login_module_xr to the WC core
	 * @param req
	 * @throws ActionException 
	 */
	private void saveLoginModule(ActionRequest req, boolean isDelete) throws ActionException {
		String[] fields = new String[] { "spEntityId","providerUrlAlias","endpointUri","publicKey","parserClass"};

		req.setParameter(SiteAction.SITE_ID, MTSConstants.SUBSCRIBER_SITE_ID); //parent site
		req.setParameter("orderNo", "5");
		req.setParameter("classNm", MTSConstants.SAML_LOGIN_MODULE_CLASSPATH);
		req.setParameter("loginModuleId", MTSConstants.SAML_LOGIN_MODULE_ID);
		req.setParameter("endpointUri", "/process");

		//the module isn't active until all required fields are complete
		String qualNm = StringUtil.checkVal(req.getParameter("qualifierPattern"));
		boolean isComplete = qualNm.indexOf('@') > -1;
		for (String nm : fields)
			if (isComplete) isComplete = req.hasParameter(nm);

		req.setParameter("activeFlg", (isComplete ? "1" : "0")); //its only active if we have a valid qualifier

		SiteAuthManageAction mgr = new SiteAuthManageAction();
		mgr.setDBConnection(getDBConnection());
		mgr.setAttributes(getAttributes(), true);
		LoginConfigurationVO moduleXr = new LoginConfigurationVO(req, false);
		if (isDelete) {
			mgr.delete(req);
			//also flush stored site config
			String prefix = LoginConfigurationVO.getContextKey(MTSConstants.SAML_LOGIN_MODULE_CLASSPATH, req.getParameter(LOGIN_MODULE_XR_ID));
			WCConfigUtil.flushPrefixedConfig(dbConn, prefix, MTSConstants.SUBSCRIBER_SITE_ID);
		} else {
			String lmXrId = mgr.save(moduleXr);
			req.setParameter(LOGIN_MODULE_XR_ID, lmXrId);

			//transpose the form fields off our form into the ones the downstream action is looking for, so the config gets saved
			String contextKey = LoginConfigurationVO.getContextKey(MTSConstants.SAML_LOGIN_MODULE_CLASSPATH, lmXrId);
			for (String nm : fields)
				req.setParameter(contextKey + nm, req.getParameter(nm));

			moduleXr = new LoginConfigurationVO(req, true); //reload this now that the config is in place
			mgr.saveConfigParams(moduleXr, lmXrId);

			clearSiteCache(MTSConstants.SUBSCRIBER_SITE_ID);
		}
		sbUtil.cancelRedirect(req);
	}


	/**
	 * Save the MTS_SSO record
	 * @param vo
	 */
	private void saveSSORecord(SSOProviderVO vo, boolean isDelete) {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		boolean isUpdate = !isDelete && !StringUtil.isEmpty(vo.getSsoId());

		try {
			if (isDelete) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (Exception e) {
			log.error("could not save SSO record", e);
		}

		//if this is an update, cascade updated settings through to the mts_user table
		if (isUpdate) {
			updateUsers(vo);
			updateSubscriptions(vo);
		}
	}


	/**
	 * update the login module saved with the SSO record.  The value is immutable once written.
	 * @param vo
	 */
	private void saveLMForeignKey(String ssoId, String loginModuleXrId) {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, getCustomSchema(), 
				"mts_sso set site_login_module_xr_id=? where sso_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, loginModuleXrId);
			ps.setString(2, ssoId);
			int cnt = ps.executeUpdate();
			log.debug(String.format("updated %d row - loginModuleId=%s, ssoId=%s", cnt, loginModuleXrId, ssoId));

		} catch (SQLException sqle) {
			log.error("could not save loginModuleXrId in mts_sso", sqle);
		}
	}


	/**
	 * update existing mts_users with the changed expiration date, role, and/or publication(s)
	 * @param vo
	 */
	private void updateUsers(SSOProviderVO ssoVo) {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, getCustomSchema(), 
				"mts_user set expiration_dt=?, role_id=? where sso_id=?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setDate(1, Convert.formatSQLDate(ssoVo.getExpirationDate()));
			ps.setString(2, ssoVo.getRoleId());
			ps.setString(3, ssoVo.getSsoId());
			int cnt = ps.executeUpdate();
			log.debug(String.format("updated SSO data for %d mts users", cnt));

		} catch (SQLException sqle) {
			log.error("could not update mts_user", sqle);
		}
	}


	/**
	 * update the login module side with the changed roleId, then clear site cache
	 * @param vo
	 */
	private void cascadeRoleChange(SSOProviderVO vo) {
		String sql = "update login_module_xr set default_role_id=?, update_dt=? where login_module_xr_id=?";
		log.debug("roleId changed: " + sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, vo.getRoleId());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, vo.getLoginModuleXrId());
			int cnt = ps.executeUpdate();
			log.debug(String.format("updated %d row - loginModuleId=%s, roleId=%s", cnt, vo.getLoginModuleXrId(), vo.getRoleId()));

			clearSiteCache(MTSConstants.SUBSCRIBER_SITE_ID);

		} catch (SQLException sqle) {
			log.error("could not update login module roleId", sqle);
		}
	}


	/**
	 * delete and re-create user subscriptions tied to this SSO config
	 * @param vo
	 */
	private void updateSubscriptions(SSOProviderVO vo) {
		String schema = getCustomSchema();
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		String sql = StringUtil.join("select user_id as key from ", schema, "mts_user where sso_id=?");
		List<GenericVO> users = db.executeSelect(sql, Arrays.asList(vo.getSsoId()), new GenericVO());

		//no existing subscriptions to update, fail fast
		if (users.isEmpty()) {
			log.debug("no users tied to SSO " + vo.getSsoId());
			return;
		}

		sql = StringUtil.join(DBUtil.DELETE, schema, "mts_subscription_publication_xr where user_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (GenericVO user : users) {
				ps.setString(1, (String)user.getKey());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug(String.format("deleted %d user subscriptions", cnt.length));
		} catch (Exception e) {
			log.error("could not delete mts subscriptions", e);
		}

		//if no subscriptions to create, fail fast
		if (StringUtil.isEmpty(vo.getPublicationId())) {
			log.debug("no publications tied to SSO " + vo.getSsoId());
			return;
		}

		//recreate the subscriptions - one for each user+publication
		UUIDGenerator uuid = new UUIDGenerator();
		sql = StringUtil.join(DBUtil.INSERT_CLAUSE, schema, "mts_subscription_publication_xr ",
				"(subscription_publication_id, publication_id, user_id, create_dt) values (?,?,?,?)");
		log.debug(sql);

		for (String publicationId : vo.getPublicationId().split(",")) {
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				for (GenericVO user : users) {
					ps.setString(1, uuid.getUUID());
					ps.setString(2, publicationId);
					ps.setString(3, (String)user.getKey());
					ps.setTimestamp(4, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
				int[] cnt = ps.executeBatch();
				log.debug(String.format("added %d user subscriptions to publication %s", cnt.length, publicationId));
			} catch (Exception e) {
				log.error("could not create mts subscriptions", e);
			}
		}
	}


	/**
	 * Return a specific provider - used by the login module when we need to create
	 * a new user account.
	 * @param loginModuleXrId
	 * @return
	 */
	public SSOProviderVO getProviderById(String loginModuleXrId) {
		String sql = getRetrieveSql(getCustomSchema(), loginModuleXrId);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SSOProviderVO> providers = db.executeSelect(sql, Arrays.asList(loginModuleXrId), new SSOProviderVO());
		return providers.isEmpty() ? null : providers.get(0);
	}
}
