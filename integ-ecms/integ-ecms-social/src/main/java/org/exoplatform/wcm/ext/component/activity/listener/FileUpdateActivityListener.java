/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity.listener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.impl.CmsServiceImpl;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Value;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;


/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileUpdateActivityListener extends Listener<Node, String> {

  private String[]  editedField     = {"exo:title", "exo:summary", "exo:language", "dc:title", "dc:description", "dc:creator", "dc:source", "jcr:data"};
  private String[]  bundleMessage   = {"SocialIntegration.messages.editFileTitle",
                                       "SocialIntegration.messages.editSummary",
                                       "SocialIntegration.messages.editLanguage",
                                       "SocialIntegration.messages.editTitle",
                                       "SocialIntegration.messages.editDescription",
                                       "SocialIntegration.messages.singleCreator",
                                       "SocialIntegration.messages.singleSource",
                                       "SocialIntegration.messages.editFile",
                                       "SocialIntegration.messages.editContent"};
  private String[]  bundleRemoveMessage = {"SocialIntegration.messages.removeName",
      																 	   "SocialIntegration.messages.removeSummary",
      																 	  "SocialIntegration.messages.removeLanguage",
                                           "SocialIntegration.messages.removeTitle",
                                           "SocialIntegration.messages.removeDescription",
                                           "SocialIntegration.messages.removeCreator",
                                           "SocialIntegration.messages.removeAllSource",
                                           "SocialIntegration.messages.editFile",
                                           "SocialIntegration.messages.removeContent"};
  
  private boolean[] needUpdate      = {true, true, false, true, true, false, false, false};
  private int consideredFieldCount = editedField.length;
  /**
   * Instantiates a new post edit content event listener.
   */
  public FileUpdateActivityListener() {
	  
  }

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
  	CmsService cmsService = WCMCoreUtils.getService(CmsService.class);
  	Map<String, Object> properties = cmsService.getPreProperties(); 
  	Map<String, Object> updatedProperties = cmsService.getUpdatedProperties();
    Node currentNode = event.getSource();
    
    String propertyName = event.getData();
    String oldValue = "";
    String newValue = "";
    try {      
    	if(currentNode.getProperty(propertyName).getDefinition().isMultiple()){
    		Value[] values = currentNode.getProperty(propertyName).getValues();
    		if(values != null && values.length > 0) {
    			for (Value value : values) {
						newValue += value.getString() + ", ";
					}
    			if(newValue.length() >= 2) newValue = newValue.substring(0, newValue.length()-2);
    		}
    		values = (Value[]) properties.get(currentNode.getUUID() + "_" + propertyName);
    		if(values != null && values.length > 0) {
    			for (Value value : values) {
    				oldValue += value.getString() + ", ";
					}
    			if(oldValue.length() >= 2) oldValue = oldValue.substring(0, oldValue.length()-2);
    		}
    	} else {
    		oldValue = properties.get(propertyName).toString();
    		newValue= currentNode.getProperty(propertyName).getString();      
    	}
    }catch (Exception e) {
    }
    newValue = newValue.trim();
    oldValue = oldValue.trim();
    
    if(currentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) currentNode = currentNode.getParent();
    String resourceBundle = "";
    boolean hit = false;
    for (int i=0; i< consideredFieldCount; i++) {
      if (propertyName.equals(editedField[i])) {
      	hit = true;
      	if(newValue.length() > 0) {
      		
      		resourceBundle = bundleMessage[i];
      		if(propertyName.equals(NodetypeConstant.DC_CREATOR))
      		{
      			if(newValue.split(",").length > 1) {
      				if(oldValue != null && oldValue.length() > 0)
        	  		resourceBundle = "SocialIntegration.messages.multiCreator";
        	  	else
        	  		resourceBundle = "SocialIntegration.messages.addMultiCreator";
      			} else {
      				resourceBundle = "SocialIntegration.messages.addCreator";
      			}
      			Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
            break;
      		}
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE)) {      			
      				List<String> lstOld = Arrays.asList(oldValue.split(","));
      				List<String> lstNew = Arrays.asList(newValue.split(","));
      				String itemsRemoved = "";
      				StringBuffer sb = new StringBuffer();
      				for (String item : lstOld) {
								if(!lstNew.contains(item)) sb.append(item).append(",");
							}
      				if(sb.length() > 0) {
      				  itemsRemoved = sb.toString();
      				  itemsRemoved = itemsRemoved.substring(0, itemsRemoved.length()-1);
      				}
      				sb.delete(0, sb.length());
      				String itemsAdded = "";
      				for (String item : lstNew) {
								if(!lstOld.contains(item)) sb.append(item).append(",");
							}
      				if(sb.length() > 0) {
      					itemsAdded = sb.toString();
      					itemsAdded = itemsAdded.substring(0, itemsAdded.length()-1);
      				}
      				if(itemsRemoved.length() > 0 && itemsAdded.length() > 0){  					
      					Utils.postFileActivity(currentNode, "SocialIntegration.messages.removeSource", needUpdate[i], true, itemsRemoved);
      					Utils.postFileActivity(currentNode, "SocialIntegration.messages.addSource", needUpdate[i], true, itemsAdded);
      	        break;
      				}      				  
      				else if(itemsRemoved.length() > 0) {
      					resourceBundle = "SocialIntegration.messages.removeSource";
      					newValue = itemsRemoved;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
      	        break;
      				}
      				else if(itemsAdded.length() > 0) {
      					resourceBundle = "SocialIntegration.messages.addSource";
      					newValue = itemsAdded;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
      	        break;
      				}      			
      		}      		
      	} else if(!propertyName.equals(NodetypeConstant.EXO_LANGUAGE)){ //Remove the property
      		resourceBundle = bundleRemoveMessage[i];
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE)) {
      			newValue = oldValue;
      		}
      		Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
          break;
      	} else break;
      	        
      }
    }
    if(!hit && propertyName.startsWith("dc:") && !propertyName.equals("dc:date")) {
    	if(newValue.length() > 0) {
    		resourceBundle = "SocialIntegration.messages.updateMetadata";
    		newValue = propertyName + ActivityCommonService.METADATA_VALUE_SEPERATOR + newValue;
    	}	else {
    		resourceBundle = "SocialIntegration.messages.removeMetadata";
    		newValue = propertyName;
    	}
    	Utils.postFileActivity(currentNode, resourceBundle, false, true, newValue);
    }
  }
}
