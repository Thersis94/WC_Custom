package com.depuysynthes.scripts;

import java.io.IOException;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

/****************************************************************************
 * <b>Title</b>: ShowpadMediaBinDecorator.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 30, 2015
 ****************************************************************************/
public class ShowpadMediaBinDecorator extends DSMediaBinImporterV2 {

	/**
	 * @param args
	 */
	public ShowpadMediaBinDecorator(String[] args) {
		super(args);
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		ShowpadMediaBinDecorator smbd = new ShowpadMediaBinDecorator(args);
		//smbd.run();
		
		String code = "";
		//AuthorizationCodeFlow.newTokenRequest(code).
	}
	
	public static HttpResponse executeGet(
			HttpTransport transport, JsonFactory jsonFactory, String accessToken, GenericUrl url)
					throws IOException {
		Credential credential =
				new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
		HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
		return requestFactory.buildGetRequest(url).execute();
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		dataCounts.put("showpad", 1); //used as a boolean in the report email to print showpad stats
		super.run();
	}
	
	
	/**
	 * override the saveRecords method to push the records to Showpad after 
	 * super.saveRecords() saves them to the database.
	 */
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);
		
		//confirm we have something to add or update
		if (getDataCount((isInsert ? "inserted" : "updated")) == 0) return;
		
		//verify Showpad connection
		
		//push updates or inserts to Showpad
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if ((isInsert && vo.getRecordState() != State.Insert) || (!isInsert && vo.getRecordState() != State.Update)) 
				continue;
			
			
			
		}
		
	}
	
	
	
	/**
	 * override the deleteRecords methods to push deletions to Showpad after
	 * super.deleteRecords() saves them to the database.
	 */
	public void deleteRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		super.deleteRecords(masterRecords);
		
		//confirm we have something to delete
		if (getDataCount("deleted") == 0) return;
		
		//push deletions to Showpad
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() == State.Delete) {

			}
		}
	}

}
