package com.fastsigns.product.keystone.parser;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.OrderVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: MyOrdersParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class MyOrdersParser extends KeystoneDataParser {
	
	
	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#parseData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		
		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(OrderVO.class);
		
		try {
			JSONObject data = JSONObject.fromObject(new String(byteData));
			JSONArray jsonArr = JSONArray.fromObject(data.get("data"));
			
			mod.setActionData(JSONArray.toCollection(jsonArr, cfg));

		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mod;
	}

}
