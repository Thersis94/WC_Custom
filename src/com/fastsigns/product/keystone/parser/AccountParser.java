/**
 * 
 */
package com.fastsigns.product.keystone.parser;

import java.lang.reflect.Type;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.AccountVO;
import com.google.gson.reflect.TypeToken;
import com.siliconmtn.data.Node;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.siliconmtn.util.SMTSerializer;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: AccountParser.java
 * <p/>
 * <b>Description: </b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class AccountParser extends KeystoneDataParser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		log.info("^^^^^ " + new String(byteData));
		try {
			Type type = new TypeToken<List<Node>>(){}.getType();
			List<Node> data = SMTSerializer.fromJson(new String(byteData), type);
			
			System.out.println(data.get(0).getUserObject());
			mod.setActionData(data);
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}

		return mod;
	}

}
