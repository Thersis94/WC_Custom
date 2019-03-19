package com.wsla.util;

//JDK 1.8.x
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.wsla.data.product.ProductCategoryAssociationVO;

// WSLA Libs
import com.wsla.data.product.ProductSetVO;
import com.wsla.data.product.ProductVO;

/****************************************************************************
 * <b>Title</b>: ProductLoader.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Product Loader for WSLA.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 18, 2019
 * @updates:
 ****************************************************************************/

public class ProductLoader {
	
	private static final Logger log = Logger.getLogger(ProductLoader.class);
	
	/**
	 * Location of the file to import
	 */
	public static final String PRODUCT_FILE_PATH = "/Users/james/Downloads/products.txt";
	
	/**
	 * Maps SKUs to Product ID
	 */
	private static Map<String, String> productMapper = new HashMap<>(512);

	/**
	 * 
	 */
	public ProductLoader() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("javax.sql.DataSource");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_sb");
		dc.setPassword("");
		Connection conn = dc.getConnection();
		
		log.info("Started");
		
		// Get the products and set info parsed out
		List<ProductVO> products = saveProducts(conn);
		
		// Save the set information
		assignParts(conn, products);
		
		log.info("Completed");

	}
	
	/**
	 * 
	 * @param conn
	 * @param products
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public static void assignParts(Connection conn, List<ProductVO> products) 
	throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(conn, "custom.");
		
		for (ProductVO p : products) {
			for (ProductSetVO part : p.getParts()) {
				part.setSetId(p.getProductId());
				part.setProductId(productMapper.get(part.getProductId()));
				db.insert(part);
			}
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public static List<ProductVO> saveProducts(Connection conn) 
	throws IOException, InvalidDataException, DatabaseException {
		List<ProductVO> products = new ArrayList<>();
		DBProcessor db = new DBProcessor(conn, "custom.");
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(PRODUCT_FILE_PATH)))) {
			String temp = null;
			while ((temp = br.readLine()) != null) {
				String[] prod = temp.split("\\t");
				
				ProductVO p = new ProductVO();
				p.setProductName(prod[0]);
				p.setCustomerProductId(prod[1]);
				p.setSetFlag(Convert.formatInteger(prod[2]));
				p.setDescription(prod[3]);
				p.setProviderId(prod[4]);
				p.setActiveFlag(1);
				
				if (prod.length > 6 && prod[6] != null) {
					// Loop the set parts  Since the product id is not assigned,
					// it will have to be assigned later
					String[] parts = prod[6].split("\\:");
					for (String part : parts) {
						ProductSetVO ps = new ProductSetVO();
						ps.setProductId(part);
						ps.setQuantity(1);
						p.addPart(ps);
					}
				}
				
				// Save the product
				db.insert(p);
				products.add(p);
				productMapper.put(p.getCustomerProductId(), p.getProductId());
				
				// Save the category
				ProductCategoryAssociationVO cat = new ProductCategoryAssociationVO();
				cat.setProductCategoryId(prod[5]);
				cat.setProductId(p.getProductId());
				db.insert(cat);
			}
		}
		
		return products;
	}

}

