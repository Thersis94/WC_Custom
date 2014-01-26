package com.fastsigns.product.keystone.parser;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: DSOLMaterialsParser.java<p/>
 * <b>Description: parse the Keystone-returned Materials JSON into a usable WC object</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class DSOLMaterialsParser extends KeystoneDataParser {

	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		
		Map<String, String> mats = new HashMap<String, String>();
		try {
			JSONArray jsobj = JSONArray.fromObject(new String(byteData));
			for (int i = 0; i < jsobj.size(); i++) {
				JSONObject obj = jsobj.getJSONObject(i);
				mats.put((String)obj.get("product_id"), (String)obj.get("display_name"));
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		mod.setActionData(mats);
		return mod;
	}

}
