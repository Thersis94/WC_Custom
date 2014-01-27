package com.fastsigns.product.keystone.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: CatalogParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class CatalogParser extends KeystoneDataParser {
	
	
	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#parseData(byte[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		
		List<CatalogVO> myCatalogs = new ArrayList<CatalogVO>();
		Map<String, Class<?>> dMap = new HashMap<String, Class<?>>();
		dMap.put("sizes", SizeVO.class);
		dMap.put("images", ImageVO.class);
		
		JsonConfig cfg = new JsonConfig();
		PropertySetStrategy pss = new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT);
		cfg.setPropertySetStrategy(pss);
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
					//and sort them.
					
					List<KeystoneProductVO> prods = new ArrayList<KeystoneProductVO>(JSONArray.toCollection(jsonArr, cfg));
					Collections.sort(prods, new ProductComparator());
					
					myCategories.add(new CategoryVO(categoryNm, prods));
				}
				Collections.sort(myCategories, new CategoryComparator());

				myCatalogs.add(new CatalogVO(catalogNm, myCategories));
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		log.debug("catalogCnt=" + myCatalogs.size());
		mod.setActionData(myCatalogs);
		mod.setDataSize(myCatalogs.size());
		
		return mod;
	}

	private class ProductComparator implements Comparator<KeystoneProductVO> {

		@Override
		public int compare(KeystoneProductVO o1, KeystoneProductVO o2) {
			return o1.getDisplay_name().trim().compareTo(o2.getDisplay_name().trim());
		}
		
	}
	
	private class CategoryComparator implements Comparator<CategoryVO> {

		@Override
		public int compare(CategoryVO o1, CategoryVO o2) {
			return o1.getCategoryNm().trim().compareTo(o2.getCategoryNm().trim());
		}
		
	}
}
