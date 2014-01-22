package com.fastsigns.product.keystone;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class StartFromScratchAction extends SBActionAdapter {
	
	public StartFromScratchAction(){
		
	}
	
	public StartFromScratchAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		//Clear out old template data that may exist.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		//Use Cached action and set necessary pieces for cache groups to be used. 
		attributes.put("siteData", req.getAttribute(Constants.SITE_DATA));
		attributes.put("wcFranchiseId", CenterPageAction.getFranchiseId(req));
		KeystoneProxy proxy = new CachingKeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("products");
		proxy.setAction("getDsolMaterials");
		if (sessVo == null) {
			ProductFacadeAction.loadDefaultSession(req, true, attributes);
			sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		} else if(((FastsignsSessVO)req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO)).getFranchise(CenterPageAction.getFranchiseId(req)) == null) {
			ProductFacadeAction.loadDefaultSession(req, false, attributes);
			sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		} else if (req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID) == null) {
			//ensure we have a webId; almost all transactions revolve around this value
			req.getSession().setAttribute(FastsignsSessVO.FRANCHISE_ID, CenterPageAction.getFranchiseId(req));
		}
		log.debug(sessVo.getFranchise(webId).getFranchiseId());
		String franId = sessVo.getFranchise(webId).getFranchiseId();
		if(franId == null || franId.length() == 0){
			franId = CenterPageAction.getFranchiseId(req);
			proxy.addPostData("webId", franId);
		} else {
			proxy.addPostData("franchiseId", franId);
		}

		//tell the proxy to go get our data
		try {
			byte[] byteData = proxy.getData();
			mod.setActionData(formatMaterials(byteData));
		} catch (InvalidDataException e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Materials list");
		}
		
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	private List<KeystoneProductVO> formatMaterials(byte [] byteData) throws InvalidDataException {
		List<KeystoneProductVO> mats = new ArrayList<KeystoneProductVO>();
		log.debug(new String(byteData));
		
		try {
			JSONObject object = JSONObject.fromObject(new String(byteData));
			JSONArray jsobjs = JSONArray.fromObject(object.get("materials"));
			KeystoneProductVO vo = null;
			for (int i = 0; i < jsobjs.size(); i++) {
				vo = new KeystoneProductVO();
				JSONObject jsobj = jsobjs.getJSONObject(i);
				vo.setProductName(jsobj.getString("name"));
				vo.setProduct_id(jsobj.getString("product_id"));
				JSONArray objs = jsobj.getJSONArray("sizes");
				List<SizeVO> sizes = new ArrayList<SizeVO>();
				for(int j = 0; j < objs.size(); j++){
					JSONObject obj = objs.getJSONObject(j);
					SizeVO s = new SizeVO();
					s.setEcommerce_size_id(obj.getString("ecommerce_size_id"));
					s.setDimensions(obj.getString("dimensions"));
					
					s.setWidth_pixels(obj.getInt("width") * 72);
					s.setHeight_pixels(obj.getInt("height") * 72);
					sizes.add(s);
				}
				vo.setSizes(sizes);
				mats.add(vo);
				}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mats;
	}
}
