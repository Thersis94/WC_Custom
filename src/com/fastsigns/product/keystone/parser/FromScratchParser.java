/**
 * 
 */
package com.fastsigns.product.keystone.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: FromScratchParser.java
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
public class FromScratchParser extends KeystoneDataParser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		List<KeystoneProductVO> mats = new ArrayList<KeystoneProductVO>();

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
				for (int j = 0; j < objs.size(); j++) {
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

		mod.setActionData(mats);
		return mod;
	}

}
