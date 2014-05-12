package org.openseamap.openseamapviewer.seamarks;

import java.util.ArrayList;

import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;

public class NavigationLine {
   private ArrayList<SeamarkNode> mNodesList;
   private String mId = "";
   
   
  public NavigationLine(String id) {
	  mId = id;
	  mNodesList = new ArrayList<SeamarkNode>();
  }
  
  public String getId (){
	  return mId;
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
