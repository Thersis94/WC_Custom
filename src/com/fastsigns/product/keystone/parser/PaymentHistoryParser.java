package com.fastsigns.product.keystone.parser;

import java.lang.reflect.Type;
import java.util.List;

import com.fastsigns.product.keystone.vo.PaymentHistoryVO;
import com.google.gson.reflect.TypeToken;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.SMTSerializer;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: PaymentHistoryParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class PaymentHistoryParser extends KeystoneDataParser {

	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		
		try {
			Type type = new TypeToken<List<PaymentHistoryVO>>(){}.getType();
			List<PaymentHistoryVO> data = SMTSerializer.fromJson(new String(byteData), type);
			mod.setActionData(data);
			
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mod;
	}

}
