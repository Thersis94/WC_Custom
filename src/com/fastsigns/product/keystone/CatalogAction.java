package com.fastsigns.product.keystone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.CatalogVO;
import com.fastsigns.product.keystone.vo.CategoryVO;
import com.fastsigns.product.keystone.vo.ImageVO;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CatalogAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2012
 ****************************************************************************/
public class CatalogAction extends AbstractBaseAction {

	public CatalogAction() {
	}

	public CatalogAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		//TODO reactivate caching proxy
		//KeystoneProxy proxy = new CachingKeystoneProxy(attributes);
		KeystoneProxy proxy = new CachingKeystoneProxy(attributes, 1440);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("products");
		proxy.setAction("getCatalogProducts");
		proxy.setFranchiseId(sessVo.getFranchise(webId).getFranchiseId());
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		proxy.addPostData("webId", webId);
		
		try {
			//tell the proxy to go get our data
			byte[] byteData = proxy.getData();
			
			//transform the response into something meaningful to WC
			mod.setActionData(formatData(byteData));
			
		} catch (InvalidDataException e) {
			mod.setError(e);
			mod.setErrorMessage("Unable to load Product Catalogs");
		}
		
		//need to supply this for image calls, which go to Keystone directly from the browser
		req.setAttribute("apiKey", proxy.buildApiKey());
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	@SuppressWarnings("unchecked")
	private List<CatalogVO> formatData(byte[] byteData) throws InvalidDataException {
		List<CatalogVO> myCatalogs = new ArrayList<CatalogVO>();
		Map<String, Class<?>> dMap = new HashMap<String, Class<?>>();
		dMap.put("sizes", SizeVO.class);
		dMap.put("images", ImageVO.class);
		
		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(KeystoneProductVO.class);
		cfg.setClassMap(dMap);
		
		try {
			JSONObject jsonObj = JSONObject.fromObject(new String(byteData));
			Set<?> catalogNms = jsonObj.keySet();
			
			//iterate the catalogs found in the JSON response
			for (Object catalogNm : catalogNms) {
				//log.debug("catalog=" + catalogNm);
				JSONObject category = JSONObject.fromObject(jsonObj.get(catalogNm));
				if (category == null || category.size() == 0) continue;
				
				Set<?> categoryNms = category.keySet();
				List<CategoryVO> myCategories = new ArrayList<CategoryVO>();
				
				//iterate the categories within this catalog
				for (Object categoryNm : categoryNms) {
					//log.debug("category=" + categoryNm);
					JSONArray jsonArr = JSONArray.fromObject(category.get(categoryNm));
					if (jsonArr == null || jsonArr.size() == 0) continue;
					
					//iterate the products within this category into a Collection<ProductVO>
					myCategories.add(new CategoryVO(categoryNm, JSONArray.toCollection(jsonArr, cfg)));
				}
				myCatalogs.add(new CatalogVO(catalogNm, myCategories));
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		log.debug("catalogCnt=" + myCatalogs.size());
		return myCatalogs;
	}
	
	

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {		
	}
}
