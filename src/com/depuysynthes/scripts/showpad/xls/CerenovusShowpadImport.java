package com.depuysynthes.scripts.showpad.xls;

import java.io.IOException;

/****************************************************************************
 * <p><b>Title:</b> CerenovusShowpadImport.java</p>
 * <p><b>Description:</b> This class extends the abstract's reusable components in a 
 * way that's specific to this implementation.</p>
 * <p></p>
 * <p>Copyright: Copyright (c) 2020, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2020
 * <b>Changes:</b>
 ****************************************************************************/
public class CerenovusShowpadImport extends AbstractShowpadIngest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public CerenovusShowpadImport(String[] args) throws IOException {
		super(args);
		emailSubjectSuffix = "Cerenovus";
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new CerenovusShowpadImport(args).run();
	}
}
