/**
 * 
 */
package com.fastsigns.product.keystone.parser;

// JDK 1.7.x
import java.util.ArrayList;

// SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.SMTSerializer;

// WC Libs
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: MyAssetsParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class MyAssetsParser extends KeystoneDataParser {

	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		try {
			ArrayList<?> assets = (ArrayList<?>)SMTSerializer.fromJson(new String(byteData), ArrayList.class);
			mod.setActionData(assets);
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mod;
	}

}
