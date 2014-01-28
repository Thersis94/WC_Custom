package com.fastsigns.product.keystone.parser;

// JDK 1.7.x
import java.lang.reflect.Type;
import java.util.List;

// Gson 2.2.4
import com.google.gson.reflect.TypeToken;

// SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.SMTSerializer;

// WC Libs
import com.fastsigns.product.keystone.vo.InvoiceVO;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: InvoicesParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class InvoicesParser extends KeystoneDataParser {

	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#formatData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		
		try {
			Type type = new TypeToken<List<InvoiceVO>>(){}.getType();
			List<InvoiceVO> data = SMTSerializer.fromJson(new String(byteData), type);
			mod.setActionData(data);
			
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mod;
	}

}
