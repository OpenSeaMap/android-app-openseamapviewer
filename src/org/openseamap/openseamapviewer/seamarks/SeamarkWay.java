/*
 * Copyright 2012 V.Klein
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openseamap.openseamapviewer.seamarks;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;

/**
 * @author vkADM
 *
 */
public class SeamarkWay {
	  private String mId;
	  private ArrayList<SeamarkNode> mNodesList;
	  private ArrayList<Tag> mTagList;
	  private LinkedHashMap<String,String> mTagDictionary;
	  
	  public SeamarkWay (String id){
			 mId = id;
			 mNodesList = new ArrayList<SeamarkNode>();
			 mTagDictionary = new LinkedHashMap<String,String>();
			 mTagList = new ArrayList<Tag>();
		  }
	  
	  public String getId() {
		  return mId;
	  }
	  
	  public void addTag(Tag tag) {
		  String key = tag.key;
		  String value = tag.value;
		  mTagDictionary.put(key, value);
		  mTagList.add(tag);
		  
	  }
	  
	  public Tag getTag(int index) {
		  Tag result = null;
		  if (index > -1 && index < mTagList.size()) {
			  result = mTagList.get(index);
		  }
		  return result;
	  }
	  
	  public int getTagListSize(){
		  return mTagList.size();
	  }
	  
	  public String getValueToKey(String key){
		 String result = null;
		 if (mTagDictionary.containsKey(key)){
			 result = mTagDictionary.get(key);
		 }
		 return result; 
	  }
	  
	  public LinkedHashMap<String,String> getTagDictionary() {
		  return mTagDictionary;
	  }
	  
	  public void clear() {
		  mTagDictionary.clear();
		  mTagList.clear();
	  }
	  public ArrayList<SeamarkNode> getNodeList() {
		  return mNodesList;
	  }
	  
	  public void addNode(SeamarkNode aNode){
		  mNodesList.add(aNode);
	  }
	  
	  public boolean belongsToBoundingBox (BoundingBox boundingBox) {
		  boolean result = false;
		  int countNodes = mNodesList.size();
		  for (int index = 0; index < countNodes; index++){
			  SeamarkNode aNode = mNodesList.get(index);
			  GeoPoint geoPoint = new GeoPoint(aNode.getLatitudeE6(),aNode.getLongitudeE6());
			  if (boundingBox.contains(geoPoint)){
				  result = true;
				  break;
			  }
		  }
		  return result;
	  }
}
