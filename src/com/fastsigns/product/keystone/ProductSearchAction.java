package com.fastsigns.product.keystone;

import java.util.ArrayList;
import java.util.List;

import com.fastsigns.product.keystone.vo.CatalogVO;
import com.fastsigns.product.keystone.vo.CategoryVO;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductSearchAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 8, 2013
 ****************************************************************************/
public class ProductSearchAction extends AbstractBaseAction {

	public ProductSearchAction() {
	}

	public ProductSearchAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		SMTActionInterface ai = new CatalogAction(actionInit);
		ai.setAttributes(attributes);
		ai.retrieve(req);
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (mod.getActionData() == null) throw new ActionException("Unable to load product catalog");
		
		super.putModuleData(filterSearch(StringUtil.checkVal(req.getParameter("prodSearch")).toLowerCase(), mod.getActionData()));
	}
	
	
	/**
	 * Loop over the products in the catalog and remove all entries that do not
	 * match the search term.
	 * @param searchTerm
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<CatalogVO> filterSearch(String searchTerm, Object data) {
		List<CatalogVO> catalogs = (List<CatalogVO>) data;
		List<KeystoneProductVO> saveProds = null;
		
		//loop all catalogs and categories, and return only products containing matching results
		for (CatalogVO vo : catalogs) {
			for (CategoryVO cat : vo.getCategories()) {
				//ignore the 'general' category, since these product also appear in other categories
				if (":smt_general_catalog:".equals(cat.getCategoryNm())){
					cat.setProducts(new ArrayList<KeystoneProductVO>());
					continue;
				}
				saveProds = new ArrayList<KeystoneProductVO>();
				for (KeystoneProductVO prod : cat.getProducts()) {
					if (StringUtil.checkVal(prod.getWeb_description()).toLowerCase().contains(searchTerm) ||
							StringUtil.checkVal(prod.getDisplay_name()).toLowerCase().contains(searchTerm)) {
						saveProds.add(prod);
					}
				}
				cat.setProducts(saveProds);
			}
		}
		
		return catalogs;
	}

	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//no work done here, View is a static html form
		
		if (req.hasParameter("prodSearch")) {
			build(req);
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
