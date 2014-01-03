package com.sas.action.product;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SASProductReport.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 24, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SASProductReport extends SBActionAdapter {

	/**
	 * 
	 */
	public SASProductReport() {
		
	}

	/**
	 * @param actionInit
	 */
	public SASProductReport(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		StringBuilder s = new StringBuilder();
		s.append("select category_url, '/cat/qs/' + replace(a.short_desc, '|', '/')  as full_url, ");
		s.append("c.product_id, cust_product_no, product_url, cust_category_id ");
		s.append("from product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("inner join product c on b.product_id = c.product_id ");
		s.append("where a.organization_id = 'SAS' ");
		s.append("union ");
		s.append("select category_url, '/cat/qs/' + replace(a.short_desc, '|', '/') as full_url, ");
		s.append("c.product_id, cust_product_no, product_url, cust_category_id ");
		s.append("from product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("inner join product c on b.product_id = c.parent_id ");
		s.append("where a.organization_id = 'SAS' ");
		s.append("order by category_url ");
		
		log.debug("SQS Report SQL: " + s);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				
			}
		} catch (Exception e) {
			log.error("Unable to retrieve SAS product report", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}

}
