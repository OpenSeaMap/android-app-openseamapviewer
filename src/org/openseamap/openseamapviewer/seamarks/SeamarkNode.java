package org.openseamap.openseamapviewer.seamarks;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.mapsforge.core.Tag;
import org.openseamap.openseamapviewer.overlay.SeamarkOverlayItem;

public class SeamarkNode {
  private String mNodeNumber;
  private int  mLatitudeE6 ;
  private int mLongitudeE6;
  private ArrayList<Tag> mTagList;
  private LinkedHashMap<String,String> mTagDictionary;
 
  // use with OpenSeamapWithPoisViewer
  private SeamarkWithPoisOverlayItem mSeamarkWithPoisOverlayItem = null;
  // use with OpenSeamapViewer
  private SeamarkOverlayItem mSeamarkOverlayItem = null;
  private boolean mIsVisible = false;
  
  public SeamarkNode (String nodeNumber){
	 mNodeNumber = nodeNumber;
	 mTagDictionary = new LinkedHashMap<String,String>();
	 mTagList = new ArrayList<Tag>();
  }
  
  public String getId() {
	  return mNodeNumber;
  }
  
  public void setSeamarkWithPoisOverlayItem(SeamarkWithPoisOverlayItem seamarkOverlayItem){
	  mSeamarkWithPoisOverlayItem = seamarkOverlayItem; 
  }
  
  public SeamarkWithPoisOverlayItem getSeamarkWithPoisOverlayItem () {
	  return mSeamarkWithPoisOverlayItem;
  }
  
  
  public void setSeamarkOverlayItem(SeamarkOverlayItem seamarkOverlayItem){
	  mSeamarkOverlayItem = seamarkOverlayItem; 
  }
  
  public SeamarkOverlayItem getSeamarkOverlayItem () {
	  return mSeamarkOverlayItem;
  }
  
  public void setVisibility(boolean visible){
	  mIsVisible= visible;
  }
  public boolean getVisibility() {
	  return mIsVisible;
  }
  
  
  public void setLatitudeE6( int lat) {
	  mLatitudeE6 = lat;
  }
  
  public int getLatitudeE6 (){
	  return mLatitudeE6;
  }
  
  public void setLongitudeE6 (int lon) {
	  mLongitudeE6 = lon;
  }
  
  public int getLongitudeE6 (){
	  return mLongitudeE6;
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
}
