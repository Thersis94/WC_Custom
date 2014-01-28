package com.fastsigns.product.keystone.parser;

/** JDK 1.7.x **/
import java.lang.reflect.Type;
import java.util.List;

/** Gson 2.2.4 **/
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/** SMT Base Libs **/
import com.siliconmtn.data.Node;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.SMTSerializer;

/** WC Libs **/
import com.fastsigns.product.keystone.vo.AccountVO;
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
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		String val = new String(byteData);
		log.info("^^^^^ " + val);
		try {
			Type type = new TypeToken<List<Node>>(){}.getType();
			List<Node> data = SMTSerializer.fromJson(new String(val), type);
			JsonParser jsonparser = new JsonParser();
			JsonArray values = (JsonArray)jsonparser.parse(val);

	        for(int i=0; i< values.size(); i++){
	        	JsonObject value = (JsonObject)values.get(i);
	        	JsonObject typeInfo = (JsonObject)value.get("userObject");
	        	Gson g = new Gson();
	        	AccountVO o = g.fromJson(typeInfo.toString(), AccountVO.class);
	        	data.get(i).setUserObject(o);
	        }
	        
			mod.setActionData(data);
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}

		return mod;
	}

}
