package com.sas.action.product;

// JDK 1.6
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// J2EE 1.5
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

// SMT Base Libs
import com.siliconmtn.http.SMTBaseServlet;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SASReportServlet.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Generates a list of url paths in a text delimited format 
 * for the SAS product catalog
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 24, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SASReportServlet extends SMTBaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SASReportServlet() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.http.SMTBaseServlet#processRequest(com.siliconmtn.http.SMTServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRequest(SMTServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		log.debug("Starting SAS Report Servlet");
		
		// Determine if the request is for
		int type = Convert.formatInteger(req.getParameter("type"), 1);
		
		Connection conn = this.getDBConnection();
		try {
			if (type == 1) this.retrieveProductReport(conn, res);
			else this.retrieveCategoryReport(conn, res);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Gets the product URL and Group Info
	 * @param dbConn
	 * @param res
	 * @throws IOException
	 */
	public void retrieveProductReport(Connection dbConn, HttpServletResponse res) 
	throws IOException {
		StringBuilder s = new StringBuilder();
		s.append("select category_url, '/cat/qs/' + replace(a.short_desc, '|', '/')  as full_url, ");
		s.append("c.product_id, cust_product_no, product_url, cust_category_id ");
		s.append("from product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("inner join product c on b.product_id = c.product_id ");
		s.append("where a.organization_id = 'SAS' and parent_id is null ");
		s.append("union ");
		s.append("select category_url, '/cat/qs/' + replace(a.short_desc, '|', '/') as full_url, ");
		s.append("c.product_id, cust_product_no, product_url, cust_category_id ");
		s.append("from product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("inner join product c on b.product_id = c.parent_id ");
		s.append("where a.organization_id = 'SAS' and parent_id is not null ");
		s.append("order by category_url ");
		
		log.debug("SQS Report SQL: " + s);
		
		PreparedStatement ps = null;
		res.setContentType("text/plain");
		
		// Write out the header
		OutputStream os = res.getOutputStream(); 
		os.write(this.getProdHeader());
		try {
			ps = dbConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				
				String url = "/cat/qs/detail/" + rs.getString(5) + "/" + rs.getString(4);
				if (StringUtil.checkVal(rs.getString(4)).length() == 0) {
					url = rs.getString(2) + "/" + rs.getString(5);
				}
				
				StringBuilder val = new StringBuilder();
				val.append(rs.getString(6)).append("\t");
				val.append(rs.getString(2)).append("\t");
				val.append(StringUtil.checkVal(rs.getString(4))).append("\t");
				val.append(rs.getString(3)).append("\t");
				val.append(url).append("\r\n"); 
				os.write(val.toString().getBytes());
			}
		} catch (Exception e) {
			log.error("Unable to retrieve SAS product report", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Gets the product URL and Group Info
	 * @param dbConn
	 * @param res
	 * @throws IOException
	 */
	public void retrieveCategoryReport(Connection dbConn, HttpServletResponse res) 
	throws IOException {
		StringBuilder s = new StringBuilder();
		s.append("select cust_category_id, category_url, replace(short_desc, '|', '/') as full_url ");
		s.append("from product_category ");
		s.append("where organization_id = 'SAS' ");
		s.append("order by category_url");
		
		log.debug("SAS Cat Report SQL: " + s);
		
		PreparedStatement ps = null;
		res.setContentType("text/plain");
		
		// Write out the header
		OutputStream os = res.getOutputStream(); 
		os.write(this.getCatHeader());
		try {
			ps = dbConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				StringBuilder val = new StringBuilder();
				val.append(rs.getString(1)).append("\t");
				val.append(rs.getString(2)).append("\t");
				val.append("/cat/qs/" + rs.getString(3)).append("\r\n"); 
				os.write(val.toString().getBytes());
			}
		} catch (Exception e) {
			log.error("Unable to retrieve SAS product report", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the desc header
	 * @return
	 */
	public byte[] getProdHeader() {
		StringBuilder s = new StringBuilder();
		s.append("Category ID\tCategory_URL\tProduct_SKU");
		s.append("\tProduct_ID\tProduct_URL\r\n");
		
		return s.toString().getBytes();
	}
	
	/**
	 * Returns the desc header
	 * @return
	 */
	public byte[] getCatHeader() {
		StringBuilder s = new StringBuilder();
		s.append("SAS_Category_ID\tCategory_ID\tCategory_URL\r\n");
				
		return s.toString().getBytes();
	}

}
