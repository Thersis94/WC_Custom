/**
 * 
 */
package com.fastsigns.product.keystone.parser;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.AccountVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.json.PropertyStrategyWrapper;
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
		List<Node> data = null;

		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(Node.class);

		try {
			JSONObject baseObj = JSONObject.fromObject(new String(byteData));
			JSONArray nodesArr = JSONArray.fromObject(baseObj.get("tree"));
			data = (List<Node>) JSONArray.toCollection(nodesArr, cfg);

			for (Node n : data) {
				JSONObject obj = JSONObject.fromObject(n.getUserObject());
				n.setUserObject(JSONObject.toBean(obj, AccountVO.class));
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		mod.setActionData(data);
		return mod;
	}

}
