package edu.isi.karma.controller.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.Command.CommandTag;
import edu.isi.karma.controller.history.HistoryJsonUtil.ClientJsonKeys;
import edu.isi.karma.controller.history.HistoryJsonUtil.ParameterType;
import edu.isi.karma.rep.HNode;
import edu.isi.karma.util.JSONUtil;
import edu.isi.karma.view.VWorkspace;

public class CommandHistoryWriter {
	private final ArrayList<Command> history;
	private VWorkspace vWorkspace;
	
	public enum HistoryArguments {
		vWorksheetId, commandName, inputParameters, hNodeId, tags
	}

	public CommandHistoryWriter(ArrayList<Command> history, VWorkspace vWorkspace) {
		this.history = history;
		this.vWorkspace = vWorkspace;
	}

	public void writeHistoryPerWorksheet() throws JSONException {
		HashMap<String, List<Command>> comMap = new HashMap<String, List<Command>>();
		for(Command command : history) {
			if(command.hasTag(CommandTag.Modeling) || command.hasTag(CommandTag.Transformation)) {
				JSONArray json = new JSONArray(command.getInputParameterJson());
				String vWorksheetId = HistoryJsonUtil.getStringValue(HistoryArguments.vWorksheetId.name(), json);
				String worksheetName = vWorkspace.getViewFactory().getVWorksheet(vWorksheetId).getWorksheet().getTitle(); 
				if(comMap.get(worksheetName) == null){
					comMap.put(worksheetName, new ArrayList<Command>());
				}
				comMap.get(worksheetName).add(command);
			}
		}
		
		for(String wkName : comMap.keySet()) {
			List<Command> comms = comMap.get(wkName);
			JSONArray commArr = new JSONArray();
			for(Command comm : comms) {
				JSONObject commObj = new JSONObject();
				commObj.put(HistoryArguments.commandName.name(), comm.getCommandName());
				
				// Populate the tags
				JSONArray tagsArr = new JSONArray();
				for (CommandTag tag : comm.getTags()){
					tagsArr.put(tag.name());
				}
				commObj.put(HistoryArguments.tags.name(), tagsArr);
				
				JSONArray inputArr = new JSONArray(comm.getInputParameterJson());
				for (int i = 0; i < inputArr.length(); i++) {
					JSONObject inpP = inputArr.getJSONObject(i);
					
					/*** Check the input parameter type and accordingly make changes ***/
					if(HistoryJsonUtil.getParameterType(inpP) == ParameterType.hNodeId) {
						String hNodeId = inpP.getString(ClientJsonKeys.value.name());
						HNode node = vWorkspace.getRepFactory().getHNode(hNodeId);
						JSONArray hNodeRepresentation = node.getJSONArrayRepresentation(vWorkspace.getRepFactory());
						inpP.put(ClientJsonKeys.value.name(), hNodeRepresentation);
					
					} else if (HistoryJsonUtil.getParameterType(inpP) == ParameterType.vWorksheetId) {
						inpP.put(ClientJsonKeys.value.name(), "VW");
					} else {
						// do nothing
					}
				}
				commObj.put(HistoryArguments.inputParameters.name(), inputArr);
				 if(!commandAlreadyexists(commArr, commObj)){
					commArr.put(commObj);
				}
			}
//			System.out.println(commArr.toString(4));
			JSONUtil.writeJsonFile(commArr, HistoryJsonUtil.constructWorksheetHistoryJsonFilePath(wkName, 
					vWorkspace.getPreferencesId()));
		}
	}
	
	private boolean commandAlreadyexists(JSONArray commArr, JSONObject commObj1) throws JSONException {
		for (int i = 0; i< commArr.length(); i++) {
			JSONObject commObj2 = (JSONObject) commArr.get(i);
			if (JSONUtil.compareJSONObjects(commObj1, commObj2)) {
				return true;
			}
		}
		return false;
	}
}
