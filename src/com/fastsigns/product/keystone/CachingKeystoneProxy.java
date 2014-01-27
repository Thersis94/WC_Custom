package com.fastsigns.product.keystone;

import java.util.Map;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import com.opensymphony.oscache.web.filter.ExpiresRefreshPolicy;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleController;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CachingKeystoneProxy.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 7, 2013
 ****************************************************************************/
public class CachingKeystoneProxy extends KeystoneProxy {

	/**
	 * default timeout actions that cache personal data will cache for.  This overrides
	 * the value set in sb_config, which is cache timeout for the Franchise's data. (many hours or days long)
	 */
	public static final int ACCOUNT_DATA_TIMEOUT = 10; //mins
	
	public static final String TIMEOUT = "keystoneProxyTimeout";
	private GeneralCacheAdministrator cache = null;
	private String organizationId = null; 
	private String siteId = null;
	private String wcFranchiseId = null;
	protected int osCacheTimeout = 0;  //in seconds
	protected String[] cacheGroups = null;
	
	/*
	 * overloaded constructor allows for customizing the cacheTimeout
	 */
	public CachingKeystoneProxy(Map<String, Object> attribs, int cacheTimeoutMins) {
		super(attribs);
		this.cache =  (GeneralCacheAdministrator) attribs.get(GlobalConfig.OSCACHE);
		this.osCacheTimeout = cacheTimeoutMins * 60; //convert to seconds, OSCache wants seconds
				
		ModuleVO mod = (ModuleVO) attribs.get(Constants.MODULE_DATA);
		this.cacheGroups = mod.getCacheGroups();
		mod = null;

		SiteVO site = ((SiteVO)attribs.get(Constants.SITE_DATA));
		this.organizationId = site.getOrganizationId();
		this.siteId = site.getSiteId();
		this.wcFranchiseId = (String) attribs.get("wcFranchiseId");
	}
	
	
	/**
	 * intercept the http call, check for this object in cache first.
	 * return a new ModuleVO, NOT the one we retrieved from cache. 
	 * The potencial for cache poisoning exists if we don't do this.
	 */
	public ModuleVO getData() throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		ModuleVO cachedMod = null;
		String cacheKey = this.buildCacheKey();
		
		// Check the cache to see if our data already exists
		try {
	    		cachedMod = (ModuleVO) cache.getFromCache(cacheKey);
	    		log.debug("Retrieved data from cache");
			//log.debug("Retrieved from cache: " + new String(data));
			
		} catch (NeedsRefreshException e) {
			cache.cancelUpdate(cacheKey);
			//log.error(e);
			//retrieve the data from the superclass (http call to Keystone)
			cachedMod = super.getData();
			
			//store the retrieved data back into the cache for next time.
			cache.putInCache(cacheKey, cachedMod, this.buildCacheGroups(), new ExpiresRefreshPolicy(buildCacheTimeout()));
			
		} catch (Exception e) {
			log.error("error loading keystone data", e);
			cachedMod = super.getData();
		}
    	
		mod.setActionData(cachedMod.getActionData());
    		return mod;
	}
	
	
	/**
	 * buildCacheKey: 
	 * considers what is being sent to Keystone in order to create a unique key
	 * representing this transaction, so we can cache it without overlapping other
	 * entries.
	 * @return String the cache key
	 */
	protected String buildCacheKey() {
		StringBuilder key = new StringBuilder();
		key.append("FTS-KYST_").append(getAction()).append(getModule());
		if (getAccountId() != null) key.append(getAccountId());
		if (getFranchiseId() != null) key.append(getFranchiseId());
		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		for (String p : getPostData().keySet()) {
			key.append("-").append(p).append("-").append(getPostData().get(p));
		}
		
		log.debug("cacheKey=" + key.toString());
		//log.debug("hash=" + key.hashCode() + " strHash=" + key.toString().hashCode());
		return key.toString();
	}
	
	
	/**
	 * buildCacheGroups:
	 * determine the cache groups.  These need to hook into WC so cache can be
	 * flushed from the admintool: orgId, moduleTypeId, pageModuleId
	 * @return String[] groups
	 */
	protected String[] buildCacheGroups() {
		cacheGroups = new String [] {getWcFranchiseId(), getWcFranchiseId() + "_KEYSTONE", getOrganizationId(), getSiteId()};
		
		//we can't have nulls here, must strip them
		for (int x=0; x < cacheGroups.length; x++) {
			if (cacheGroups[x] == null || cacheGroups[x].length() == 0) {
				cacheGroups[x] = "SMT_ADMIN";
				//until we trace this down...
				log.error("NULL CACHE GROUP! " + StringUtil.getToString(cacheGroups, true, 0, ",") + " | "  + this.getModule() + " | " + this.getAction());
			}
		}
		return cacheGroups;
	}
	
	
	/**
	 * buildCacheTimeout:
	 * dynamically determine what the cache timeout will be; either: 
	 * 1) passed by the action
	 * 2) set very low because it's "for one person only"
	 * 3) WC's default
	 * @return int cache timeout in seconds
	 */
	protected int buildCacheTimeout() {
		if (osCacheTimeout > 0) {
			return osCacheTimeout;
		} else if (getUserId() != null) {
			//if this is specific to a single user, set the timeout really low (<sessionTimeout)
			return 600; //10mins
		} else {
			return ModuleController.CACHE_REFRESH_TIMEOUT;
		}
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	public String getWcFranchiseId() {
		return wcFranchiseId;
	}

	public void setWcFranchiseId(String wcFranchiseId) {
		this.wcFranchiseId = wcFranchiseId;
	}


	public int getOsCacheTimeout() {
		return osCacheTimeout;
	}


	public void setOsCacheTimeout(int osCacheTimeout) {
		this.osCacheTimeout = osCacheTimeout;
	}

}
