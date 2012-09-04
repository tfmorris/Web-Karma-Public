/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/
package edu.isi.karma.controller.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetListUpdate;
import edu.isi.karma.imp.json.JsonImport;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.util.JSONUtil;
import edu.isi.karma.view.VWorksheet;
import edu.isi.karma.view.VWorkspace;

public class ImportServiceCommand extends Command {
	private static final Logger logger = LoggerFactory
	.getLogger(ImportServiceCommand.class);

	private String ServiceUrl;
	/**
	 * GetSources or ImportSource; (in importFromService.js)
	 */
	private InteractionType interactionType;
	/**
	 * the source that was imported
	 */
	private String sourceName;
	/**
	 * sources returned by serviceURL/GetAvailableSources
	 */
	Object availableSources;
	
	public enum JsonKeys {
		updateType, SourceList
	}

	public enum PreferencesKeys {
		ServiceUrl
	}
	protected enum InteractionType {
		GetSources, ImportSource
	}

	protected ImportServiceCommand(String id, String interactionType, String ServiceUrl, String sourceName){
		super(id);
		this.ServiceUrl=ServiceUrl;
		this.sourceName=sourceName;
		if(interactionType.equals("GetSources")){
			this.interactionType=InteractionType.GetSources;
		}
		if(interactionType.equals("ImportSource")){
			this.interactionType=InteractionType.ImportSource;
		}
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Import from Service";
	}

	@Override
	public String getDescription() {
		if(sourceName==null){
			return "";
		}else{
			return sourceName;
		}
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notUndoable;
	}

	@Override
	public UpdateContainer doIt(VWorkspace vWorkspace) throws CommandException {
		
	
		//save the preferences 
		savePreferences(vWorkspace);

		try{

			if(interactionType==InteractionType.GetSources){
				//get avilable sources from the service
				String url = ServiceUrl + "/GetAvailableSources";
				logger.info("Get sources ..." + url);

				//get JSON from the URL
				String sources = getFromUrl(url);
				logger.debug("Source="+sources);
				availableSources = JSONUtil.createJson(sources);
				if(!(availableSources instanceof JSONArray)){
					return new UpdateContainer(new ErrorUpdate("Get Available Sources failed!"));					
				}
				return new UpdateContainer(new AbstractUpdate() {
					@Override
					public void generateJson(String prefix, PrintWriter pw,
							VWorkspace vWorkspace) {
						JSONObject outputObject = new JSONObject();
						try {
							outputObject.put(JsonKeys.updateType.name(),
							"GetServiceSourceList");

							JSONArray dataRows = new JSONArray();
//							dataRows.put("TestSource");
							JSONArray availableSourcesArray = (JSONArray)availableSources;
							for(int i=0; i<availableSourcesArray.length(); i++){
								dataRows.put(((JSONObject)availableSourcesArray.get(i)).get("SourceName"));
							}
							outputObject.put(JsonKeys.SourceList.name(),
									dataRows);
							pw.println(outputObject.toString(2));
						} catch (JSONException e) {
							logger.error("Error occured while generating JSON!");
						}
					}
				});
			}
			else if(interactionType==InteractionType.ImportSource){
				//import a source
				String url = ServiceUrl + "/GetSampleData?sourceName="+sourceName;
				logger.info("Import source " + url);
				
				UpdateContainer c = new UpdateContainer();

				//get data from the URL
				String wsOutput = getFromUrl(url);
				
				//add the source name to the data
				//it is always an array
				JSONArray json = (JSONArray)JSONUtil.createJson(wsOutput);
				for(int i=0; i<json.length(); i++){
					JSONObject oneRow = (JSONObject)json.get(i);
					oneRow.put("SourceName", sourceName);
					oneRow.put("score","0");
				}
				
				//import data in Karma
				//JsonImport imp = new JsonImport(wsOutput, sourceName, vWorkspace.getWorkspace());
				JsonImport imp = new JsonImport(json, sourceName, vWorkspace.getWorkspace());
				Worksheet wsht = imp.generateWorksheet();
				vWorkspace.addAllWorksheets();
				
				c.add(new WorksheetListUpdate(vWorkspace.getVWorksheetList()));
				VWorksheet vw = vWorkspace.getVWorksheet(wsht.getId());
				vw.update(c);

				return c;
				
			}
			return new UpdateContainer(new ErrorUpdate("Unknown interaction type " + interactionType));
			
		} catch (Exception e) {
			e.printStackTrace();
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}
	}

	/**
	 * Returns the result data from the given URL.
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private String getFromUrl(String url) throws UnsupportedEncodingException, IOException{
		
		URL urlStr = new URL(url);
		URLConnection connection = urlStr.openConnection();

		StringBuffer outString = new StringBuffer();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(
						connection.getInputStream(), "UTF-8"));

		String outLine;
		while ((outLine = in.readLine()) != null) {
			outString.append(outLine + "\n");
		}
		in.close();
		return outString.toString();
	}

	private void savePreferences(VWorkspace vWorkspace){
		try{
			JSONObject prefObject = new JSONObject();
			prefObject.put(PreferencesKeys.ServiceUrl.name(), ServiceUrl);
			vWorkspace.getPreferences().setCommandPreferences(
					"ImportServiceCommandPreferences", prefObject);
			
			/*
			System.out.println("I Saved .....");
			ViewPreferences prefs = vWorkspace.getPreferences();
			JSONObject prefObject1 = prefs.getCommandPreferencesJSONObject("PublishDatabaseCommandPreferences");
			System.out.println("I Saved ....."+prefObject1);
			 */
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public UpdateContainer undoIt(VWorkspace vWorkspace) {
		return null;
	}

}
