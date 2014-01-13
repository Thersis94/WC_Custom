package com.fastsigns.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;
import com.smt.sitebuilder.db.ProfileExport;

public class ProductCategoryConversion {
	protected static String DESTINATION_DB_URL = "jdbc:sqlserver://192.168.3.120:2007";
	protected static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
	protected static String[] DESTINATION_AUTH = new String[] {"sitebuilder_sb_user", "sqll0gin"};
	protected static String exportFile = "C:/Users/smt_user/Desktop/conversionQueries.sql";
	protected static String importFile = "C:/Users/smt_user/Desktop/changing_urls.txt";
    protected static final Logger log = Logger.getLogger(ProfileExport.class);
    protected Map<String, String> prods;
    protected Map<String, String> cats;
    protected static Map<String, String> errors;
    public ProductCategoryConversion() {
    	PropertyConfigurator.configure("C:/Software/log4j.properties");
    	prods = new TreeMap<String, String>();
    	cats = new TreeMap<String, String>();
    	errors = new TreeMap<String, String>();
    }
    
    public static void main(String [] args){
    	
    	ProductCategoryConversion pCC = new ProductCategoryConversion();
    	pCC.execute(args);
    	if(errors.size() == 0)
    	System.out.println("Operation Completed Successfully to: " + exportFile);
    	else{
        	System.out.println("Operation Completed with Errors to: " + exportFile);
        	for(String k : errors.keySet())
        		System.out.println("Key: " + k + " was previously assigned, new value: " + errors.get(k) + " ignored");
    	}
    }
    
    public void execute(String [] args){
        if (args.length > 0){
        	exportFile = args[0];
        	importFile = args[1];
        }

		try {
			System.out.println("exportFile=" + exportFile);
			System.out.println("importFile=" + importFile);
			//List<UserDataVO> profiles = retrievePaths(data);
			
			//write the profiles to the export file in the desired format
			File f = new File(exportFile);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY_XR] drop constraint [PRODCAT_PROD_XR_FKEY]\n"));
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY_XR] add constraint [PRODCAT_PROD_XR_FKEY] foreign key([PRODUCT_CATEGORY_CD]) references [PRODUCT_CATEGORY] ([PRODUCT_CATEGORY_CD]) on update cascade on delete cascade\n"));
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY] drop constraint [PRODUCTCAT_PARENT_SKEY]\n"));
			/**
			 * Do stuff for iteration.
			 */
			String oldL = null;
			String newL = null;
			Scanner s = null;
			try{
				s = new Scanner(new FileReader(importFile));
				while(s.hasNext()){
					oldL = s.next();
					newL = s.next();
					StringTokenizer oldT = new StringTokenizer(oldL, "//");
					StringTokenizer newT = new StringTokenizer(newL, "//");
					buildQuery(bos, oldT, newT, oldT.countTokens());
					oldL = null;
					newL = null;
					
				}
			} catch (FileNotFoundException e){
				System.err.println("Error, File: \"" + importFile + "\" not found.");
				log.debug(e);
			} catch (IOException e) {
				System.err.println("Error, IOException thrown for: " + oldL + " " + newL);
				e.printStackTrace();
				log.debug(e);
			} finally {
				s.close();
			}
			
			/*Iterator<UserDataVO> iter = profiles.iterator();
			while (iter.hasNext()) {
				UserDataVO vo = iter.next();
				StringBuffer b = new StringBuffer("<tr><td>");
				b.append(vo.getProfileId()).append(DELIMITER).append(vo.getFirstName()).append(DELIMITER).append(vo.getLastName()).append(DELIMITER);
				b.append(vo.getEmailAddress()).append(DELIMITER).append(vo.getAddress()).append(DELIMITER);
				b.append(vo.getAddress2()).append(DELIMITER).append(vo.getCity()).append(DELIMITER);
				b.append(vo.getState()).append(DELIMITER).append("&nbsp;").append(vo.getZipCode());
				for (Iterator<PhoneVO> i = vo.getPhoneNumbers().iterator(); i.hasNext();) {
					PhoneVO ph = i.next();
					b.append(DELIMITER).append(ph.getPhoneType() + ": " + ph.getFormattedNumber());
				}
				b.append("</td></tr>");
				//log.debug(vo.getLastName() + " " + vo.getProfileId());
				bos.write(b.toString().getBytes());
				b = null;
				vo = null;
			}
			bos.write(new String("</table>").getBytes());*/
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY_XR] drop constraint [PRODCAT_PROD_XR_FKEY]\n"));
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY_XR] add constraint [PRODCAT_PROD_XR_FKEY] foreign key([PRODUCT_CATEGORY_CD]) references [PRODUCT_CATEGORY] ([PRODUCT_CATEGORY_CD]) on update no action on delete cascade\n"));
			printOutput(bos, new String("alter table [PRODUCT_CATEGORY] add constraint [PRODUCTCAT_PARENT_SKEY] foreign key([PARENT_CD]) references [Product_Category] ([PRODUCT_CATEGORY_CD]) on update no action on delete no action\n"));
			bos.close();
			} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		}
	}
    protected void buildQuery(BufferedOutputStream bos, StringTokenizer oldT, StringTokenizer newT, int level) throws IOException{
    	String [] one = new String [level];
		String [] two = new String [level];
		for(int i = 0; i < level; i++){
			one[i] = oldT.nextToken();
			two[i] = newT.nextToken();
		}
		
    	switch(level){
    	case 3:{
    		if(isProduct(one[0])){
    			if(!prods.containsKey(one[2])){
    			printOutput(bos, new String("update PRODUCT set PRODUCT_ID=\'" + two[2] + "\' where PRODUCT_ID=\'" + one[2] + "\'\n"));
    			printOutput(bos, new String("update PRODUCT set PARENT_ID=\'" + two[2] + "\' where PARENT_ID=\'" + one[2] + "\'\n"));
    			prods.put(one[2], two[2]);
    			}
    			else if(prods.get(one[2]) != null && prods.get(one[2]).compareTo(two[2]) != 0){
    					errors.put(one[2], two[2]);
    				}
    		}
    		else{    
    			if(!cats.containsKey(one[2])){
    			printOutput(bos, new String("update PRODUCT_CATEGORY set PRODUCT_CATEGORY_CD=\'" + two[2] + "\' where PRODUCT_CATEGORY_CD=\'" + one[2] + "\'\n"));
    			cats.put(one[2], two[2]);
    			}
    			else if(cats.get(one[2]).compareTo(two[2]) != 0){
    				errors.put(one[2], two[2]);
    			}
    		}
    		break;
    	}
    	case 4:
    	case 5:{
    		if(isProduct(one[0])){
    			for(int i = 2; i < level; i++){
    				if(two[i].compareTo(one[i]) != 0){
    					if(!prods.containsKey(one[i])){
    					printOutput(bos, new String("update PRODUCT set PRODUCT_ID=\'" + two[i] + "\' where PRODUCT_ID=\'" + one[i] + "\'\n"));
    					printOutput(bos, new String("update PRODUCT set PARENT_ID=\'" + two[i] + "\' where PARENT_ID=\'" + one[i] + "\'\n"));
    					prods.put(one[i], two[i]);
    					} else if(prods.get(one[i]).compareTo(two[i]) != 0){
    	    				errors.put(one[i], two[i]);
    					}
    				}
    			}
    		}
    		else{
    			if(!cats.containsKey(one[2])){
    			printOutput(bos, new String("update PRODUCT_CATEGORY set PRODUCT_CATEGORY_CD=\'" + two[2] + "\' where PRODUCT_CATEGORY_CD=\'" + one[2] + "\'\n"));
    			cats.put(one[2], two[2]);
    			} else if(cats.get(one[2]).compareTo(two[2]) != 0){
	    				errors.put(one[2], two[2]);
					}
    			for(int i = 3; i < level; i++){
    				if(two[i].compareTo(one[i]) != 0){    					
    					if(!prods.containsKey(one[i])){
    					printOutput(bos, new String("update PRODUCT set PRODUCT_ID=\'" + two[i] + "\' where PRODUCT_ID=\'" + one[i] + "\'\n"));
    					printOutput(bos, new String("update PRODUCT set PARENT_ID=\'" + two[i] + "\' where PARENT_ID=\'" + one[i] + "\'\n"));
    					prods.put(one[i], two[i]);
    					} else if(prods.get(one[i]).compareTo(two[i]) != 0){
    	    				errors.put(one[i], two[i]);
    					}
    				}
    			}
    		}
    		break;
    	}
    	default: System.err.println("This is not a valid item: " + oldT.toString() + " " + newT.toString());
    	break;
    	}
    }
    public void printOutput(BufferedOutputStream bos, String q) throws IOException{
    	bos.write(q.getBytes());
    	//System.out.println(q);
    }
	protected boolean isProduct(String s){
		if(s.toLowerCase().equals("event") || s.toLowerCase().equals("industry")){
			return false;
		}
		return true;
	}
	/**
	 * 
	 * @param userName Login Account
	 * @param pwd Login password info
	 * @param driver Class to load
	 * @param url JDBC URL to call
	 * @return Database Conneciton object
	 * @throws DatabaseException
	 */
	protected Connection getDBConnection(String userName, String pwd, String driver, String url) 
	throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver", cnfe);
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, userName, pwd);
		} catch (SQLException sqle) {
			sqle.printStackTrace(System.out);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}
		
		return conn;
	}

	protected void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch(Exception e) {}
	}
	
	public List<String> loadPaths() {
		List<String> data = new ArrayList<String>();
		data.add("");
		return data;
	}

}
