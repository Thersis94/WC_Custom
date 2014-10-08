package com.fastsigns.product.keystone;

import java.util.List;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.product.keystone.vo.CatalogVO;
import com.fastsigns.product.keystone.vo.CategoryVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;
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
		String webId = CenterPageAction.getFranchiseId(req);
		
		//Use Cached action and set necessary pieces for cache groups to be used. 
		attributes.put(Constants.SITE_DATA, req.getAttribute(Constants.SITE_DATA));
		attributes.put("wcFranchiseId", CenterPageAction.getFranchiseId(req));
		KeystoneProxy proxy = KeystoneProxy.newInstance(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("products");
		proxy.setAction("getCatalogProducts");
		proxy.setFranchiseId(sessVo.getFranchise(webId).getFranchiseId());
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		proxy.addPostData("webId", webId);
		proxy.setParserType(KeystoneDataParser.DataParserType.Catalog);
		
		try {
			//tell the proxy to go get our data
			ModuleVO mod2 = proxy.getData();
			
			//copy the data out of the potentially-cached object into our own.
			mod.setActionData(mod2.getActionData());
			
		} catch (Exception e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Product Catalogs");
		}
		
		//need to supply this for image calls, which go to Keystone directly from the browser
		req.setAttribute("apiKey", proxy.buildApiKey());
		
		//If we're calling for a dsol template only return that product.
		if(req.hasParameter("altId")) {
			retrieveProduct(mod, req);
		}
		
		//Use Cached action and set necessary pieces for cache groups to be used. 
		proxy = KeystoneProxy.newInstance(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("products");
		proxy.setAction("getDsolMaterials");
		proxy.setParserType(KeystoneDataParser.DataParserType.FromScratch);
		
		//tell the proxy to go get our data
		try {
			//this was moved down here because of the potential NPE on getFranchiseId():
			proxy.setFranchiseId(sessVo.getFranchise(webId).getFranchiseId());
			proxy.addPostData("franchiseId", sessVo.getFranchise(webId).getFranchiseId());
			
			mod.setAttribute("fromScratch", proxy.getData().getActionData());
			
		} catch (Exception e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Materials list");
		}
		
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Return just the product when we're calling for a dsol template.
	 * @param mod
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void retrieveProduct(ModuleVO mod, SMTServletRequest req) {
		List<CatalogVO> cats = (List<CatalogVO>)mod.getActionData();
		for(CatalogVO cat : cats) {
			if(cat.getCatalogNm().equals(req.getParameter("catalog"))) {
			List<CategoryVO> cg = (List<CategoryVO>) cat.getCategories();
			for(CategoryVO c : cg) {
					for(ProductVO p : c.getProducts()) {
						if(p.getProductId().equals(req.getParameter("product"))) {
							mod.setActionData(p);
							return;
						}
					}
				}
			}
		}		
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
