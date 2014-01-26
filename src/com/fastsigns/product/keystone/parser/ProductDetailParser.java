package com.fastsigns.product.keystone.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.vo.ImageVO;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ProductDetailVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO.OptionVO;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: ProductDetailParser.java
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
public class ProductDetailParser extends KeystoneDataParser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(
	 * byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();

		// define a Map of object types for each of the different Object
		// variables in the bean
		Map<String, Class<?>> dMap = new HashMap<String, Class<?>>();
		dMap.put("sizes", SizeVO.class);
		dMap.put("images", ImageVO.class);
		ProductDetailVO vo = null;

		try {
			// pass the definition Map and base bean Class to the static toBean generator
			JSONObject jsonObj = JSONObject.fromObject(new String(byteData));
			vo = (ProductDetailVO) JSONObject.toBean(jsonObj,ProductDetailVO.class, dMap);

			// now we need to iterate the modifiers and sublevels
			JSONObject modsObj = jsonObj.getJSONObject("modifiers");
			Set<?> modifiers = modsObj.keySet();
			for (Object modifier : modifiers) {
				JSONObject modObj = JSONObject.fromObject(modsObj.get(modifier));
				ModifierVO modVo = new ModifierVO();
				modVo.setDescription(modObj.getString("description"));
				modVo.setModifier_id(modObj.getString("modifier_id"));
				modVo.setModifier_name(modObj.getString("modifier_name"));

				JSONObject attrsObj = modObj.getJSONObject("attributes");
				Set<?> attributes = attrsObj.keySet();
				for (Object attribute : attributes) {
					JSONObject attrObj = JSONObject.fromObject(attrsObj.get(attribute));
					AttributeVO attrVo = modVo.new AttributeVO();
					attrVo.setAttribute_name(attrObj.getString("attribute_name"));
					attrVo.setAttribute_type(attrObj.getString("attribute_type"));
					attrVo.setModifiers_attribute_id(attrObj.getString("modifiers_attributes_id"));
					attrVo.setAttribute_required(attrObj.getInt("attribute_required"));

					JSONObject optionsObj = attrObj.getJSONObject("options");
					Set<?> options = optionsObj.keySet();
					for (Object option : options) {
						JSONObject optObj = JSONObject.fromObject(optionsObj.get(option));
						OptionVO optVo = attrVo.new OptionVO();
						optVo.setModifiers_attributes_options_id(optObj.getString("modifiers_attributes_options_id"));
						optVo.setOption_name(optObj.getString("option_name"));
						optVo.setOption_value(optObj.getString("option_value"));
						attrVo.addOption(optVo);
					}
					modVo.addAttribute(attrVo);
				}
				vo.addModifier(modVo);
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}

		mod.setActionData(vo);
		return mod;
	}

}
