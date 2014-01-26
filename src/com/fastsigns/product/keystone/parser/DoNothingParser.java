package com.fastsigns.product.keystone.parser;

import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: DoNothingParser.java<p/>
 * <b>Description: Does nothing to the byte[] but return it in a ModuleVO. 
 * This class is provided for completeness of code, not with practical intent.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class DoNothingParser extends KeystoneDataParser {
	
	
	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#parseData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		mod.setActionData(byteData);
		return mod;
	}

}
