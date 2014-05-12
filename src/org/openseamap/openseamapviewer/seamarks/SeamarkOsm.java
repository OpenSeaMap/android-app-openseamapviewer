package org.openseamap.openseamapviewer.seamarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.namespace.QName;

import org.mapsforge.core.Tag;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;



import android.content.Context;
import android.util.Log;

public class SeamarkOsm {
	private static final String TAG = "SeamarkOSM";
	private static final boolean test = false;
	  private Context mContext;
	  private File mSeamarkFile = null;
	  private ArrayList<String> mStringList = null; // not in use only for loadDataOld 12_11_30
	  private boolean mFileReadHasFinished = false;
	  private ArrayList<SeamarkNode> mSeamarksNodesList;
	  private LinkedHashMap<String,SeamarkNode> mSeamarksDictionary;
	  private LinkedHashMap<String,SeamarkNode> mWayNodesDictionary;
	  //private ArrayList<NavigationLine> mNavigationLinesList;
	  private  ArrayList<SeamarkWay> mSeamarkWaysList;
      public SeamarkOsm ( Context context){
    	 mContext = context; 
    	 mStringList = new ArrayList<String>(); // not in use only for  loadDataOld 12_11_30
    	 mFileReadHasFinished = false;
    	 mSeamarksNodesList = new ArrayList<SeamarkNode>();
    	 mSeamarksDictionary = new LinkedHashMap<String,SeamarkNode>();
    	 mWayNodesDictionary = new LinkedHashMap<String,SeamarkNode>();
    	 //mNavigationLinesList = new ArrayList<NavigationLine>();
    	 mSeamarkWaysList = new ArrayList<SeamarkWay>();
      }
      
      
      public void readSeamarkFile (String aPath){
    	  File aFile = new File (aPath);
    	  loadDataWithThread(aFile);
    	   
      }
      
      public boolean getSeamarkFileReadComplete() {
    	  return mFileReadHasFinished;
      }
      
     /* public ArrayList<String> getSeamarksAsStringList(){  / 12_11_30
    	 if (mFileReadHasFinished) return mStringList;
    	 return null;
      }*/
      
      public ArrayList<SeamarkNode> getSeamarksAsArrayList(){
          if (mFileReadHasFinished) return  mSeamarksNodesList ;
        return null;
     }
      
      public LinkedHashMap<String,SeamarkNode> getSeamarksAsDictionary(){
           if (mFileReadHasFinished) return  mSeamarksDictionary ;
         return null;
      }
      
     /* public ArrayList<NavigationLine> getNavigationLinesAsArrayList(){
          if (mFileReadHasFinished) return mNavigationLinesList ;
        return null;
     }*/
      
      public ArrayList<SeamarkWay> getSeamarkWaysAsArrayList() {
    	  if (mFileReadHasFinished) return mSeamarkWaysList ;
    	  return null;
      }
      
      
      public void loadDataWithThread(File aFile) {
            
			final String aFilename = aFile.getAbsolutePath();
			Log.d(TAG,"Reading seamarksfile: " + aFilename);
			new Thread(new Runnable() {

				public void run() {

					try {
						
						XmlPullParserFactory parserCreator;

						parserCreator = XmlPullParserFactory.newInstance();

						XmlPullParser parser = parserCreator.newPullParser();
						FileReader myReader = new FileReader(aFilename);
						parser.setInput(myReader);
						// parser.setInput(text.openStream(), null);

						int parserEvent = parser.getEventType();
						long nodeCount = 0;
						long seamarkNodeCount = 0;
						long wayCount = 0;
						long relationCount = 0;
						long seamarkWayCount = 0;
						String aId = "";
						String aWayId ="";
						String aNodeStr="";
						
                        SeamarkNode currentSeamarkNode = null;
                        boolean seamarkNodeFound = false;
                        //NavigationLine currentNavLine = null;
                        //boolean navLineFound= false;
                        SeamarkWay currentSeamarkWay = null;
                        boolean seamarkWayFound = false;
                        
						// Parse the XML returned from the file
						while (parserEvent != XmlPullParser.END_DOCUMENT) {
							switch (parserEvent) {
								case XmlPullParser.START_TAG:
									String tag = parser.getName();

									if (tag.compareTo("node") == 0) {
										// make a new node
										String aNodeNumberStr = parser.getAttributeValue(null,"id");
										aId = aNodeNumberStr;
										
										String aLatStr = parser.getAttributeValue(null,"lat");
										Double aLat = Double.parseDouble(aLatStr);
										int aLatE6 = (int)(aLat * 1E6);
										
										String aLonStr = parser.getAttributeValue(null,"lon");
										Double aLon = Double.parseDouble(aLonStr);
										int aLonE6 = (int)(aLon * 1E6);
										
										currentSeamarkNode = new SeamarkNode(aNodeNumberStr);
										currentSeamarkNode.setLatitudeE6(aLatE6);
										currentSeamarkNode.setLongitudeE6(aLonE6);
										mWayNodesDictionary.put(aId, currentSeamarkNode);
										
									} 
									if (tag.compareTo("tag") == 0) {
										int countAttr = parser.getAttributeCount();
										// should be <tag k="seamark:...." v="xyz" >
										for (int indexAttr = 0;2*indexAttr < countAttr; indexAttr++) {
											String aKeyAttributeName = parser.getAttributeName(indexAttr);
											String aKeyAttributeValue  = parser.getAttributeValue(indexAttr);
											
											String aValueAttributeName = parser.getAttributeName(indexAttr +1);
											String aValueAttributeValue  = parser.getAttributeValue(indexAttr +1);
											
											if (aKeyAttributeValue.contains("seamark")){  // maybe we have a node or a way
												Tag aTag = new Tag(aKeyAttributeValue,aValueAttributeValue);
												if (currentSeamarkNode != null ) { // we have a valid node
													currentSeamarkNode.addTag(aTag); 
													seamarkNodeFound = true;
												}
												if (currentSeamarkWay != null) { // we have a valid seamark way
													currentSeamarkWay.addTag(aTag);
												}
											}
											if (aKeyAttributeValue.equals("light:description")){
												Tag aTag = new Tag(aKeyAttributeValue,aValueAttributeValue);
												if (currentSeamarkNode != null ) { // we must have a valid node
													currentSeamarkNode.addTag(aTag); 
												}
											}
										}
										
									} 
									if(tag.compareTo("way")== 0){
							        	  aWayId = parser.getAttributeValue(null,"id");
							        	  // navLineFound = true;
							        	  //currentNavLine = new NavigationLine(aWayId);
							        	  seamarkWayFound = true;
							        	  currentSeamarkWay = new SeamarkWay(aWayId);
							        	  if (test) Log.d(TAG,"new way found " + aWayId); 
									}
									
									if(tag.compareTo("nd")== 0){
							        	  String refToNode = parser.getAttributeValue(null,"ref");
							        	  //System.out.println("ref to node " + refToNode);
							        	  //if (currentNavLine != null){
							        	  if (currentSeamarkWay != null){
							        		 SeamarkNode aNode =  mWayNodesDictionary.get(refToNode);
							        		 if (aNode != null){
							        			 //currentNavLine.addNode(aNode);
							        			 currentSeamarkWay.addNode(aNode);
							        			 // if(test) Log.d(TAG,"node " +  refToNode + " added to line " + currentNavLine.getId());
							        			 if(test) Log.d(TAG,"node " +  refToNode + " added to way " + currentSeamarkWay.getId());
							        		 } else {
							        			if(test) Log.d(TAG,"node to ref " + refToNode + " not found");
							        			
							        		 }
							        	  }
							        	 
							          }
									
									
									break;
								case XmlPullParser.END_TAG: {
									tag = parser.getName();

									if (tag.compareTo("node") == 0) {
										if (seamarkNodeFound) {
											seamarkNodeCount++;
											
											if ((seamarkNodeCount % 10) == 0 ){
										    	if(test)Log.d(TAG,"SeamarkNodes " + seamarkNodeCount);
										    }
											if (!mSeamarksDictionary.containsKey(aId)){
											   //Log.d(TAG,"add Node " + aId);
											   mSeamarksNodesList.add(currentSeamarkNode);
											   mSeamarksDictionary.put(aId, currentSeamarkNode);
											}
											//seamarkNodeFound = false;
											//currentSeamarkNode = null;  // for the next step we begin with aSeamarkNode = null
										}
										//Log.d(TAG,"processed " + aId);
										seamarkNodeFound = false;
										currentSeamarkNode = null;  // for the next step we begin with aSeamarkNode = null
									   
									    nodeCount++;
									    if ((nodeCount % 100) == 0 ){
									    	if(test) Log.d(TAG,"Nodes " + nodeCount);
									        
									    }
									}
									if (tag.compareTo("way")==0) {
										seamarkWayCount++;
							        	  if ((seamarkWayCount % 1) == 0 ){
										    	if(test) Log.d(TAG,"seamrk ways " + seamarkWayCount);
							        	  }
							        	  //if (currentNavLine != null){
							        	//	  mNavigationLinesList.add(currentNavLine);
							        	 // }
							        	  if (currentSeamarkWay != null){
							        		  mSeamarkWaysList.add(currentSeamarkWay);
							        	  }
							        	  //navLineFound = false;
							        	 // currentNavLine = null;
							        	  seamarkWayFound= false;
							        	  currentSeamarkWay = null;
							          }
							          
							          if (tag.compareTo("nd")==0) {
							          }
									
								}
								break;
							}

							parserEvent = parser.next();
						}
						
						mFileReadHasFinished = true;
						Log.d(TAG,"finished reading seamarksfile: " + aFilename);
						//Log.d(TAG,"found " + wayCount + " navigation_lines ");
						Log.d(TAG,"found " + seamarkWayCount + " seamark ways ");
						
					} catch (FileNotFoundException e) {
						Log.d(TAG, "File not found");
					} catch (Exception e) {
						Log.i(TAG, "Failed in parsing XML", e);
						//showToastOnUiThread(aUIMsg);

					}
					

				}
			}).start();

		}
      
      
      public synchronized void clear() {
    	 mFileReadHasFinished = false;
    	 Log.d(TAG,"Clear Semarks");
    	 mStringList.clear();
     	 mSeamarksDictionary.clear();
     	 int count = mSeamarksNodesList.size();
     	 for (int index =0;index < count;index++) {
     		 SeamarkNode aSeamarkNode = mSeamarksNodesList.get(index);
     		 aSeamarkNode.clear();
     	 }
     	 mSeamarksNodesList.clear();
      }
      
      /**
       * old Version til 112_11_30 
       * @param aFile
       */
      
      public void loadDataWithThreadOld(File aFile) {
  
			final String aFilename = aFile.getAbsolutePath();
			
			new Thread(new Runnable() {

				public void run() {

					try {
						
						XmlPullParserFactory parserCreator;

						parserCreator = XmlPullParserFactory.newInstance();

						XmlPullParser parser = parserCreator.newPullParser();
						FileReader myReader = new FileReader(aFilename);
						parser.setInput(myReader);
						// parser.setInput(text.openStream(), null);

						int parserEvent = parser.getEventType();
						long nodeCount = 0;
						long seamarkNodeCount = 0;
						long wayCount = 0;
						long relationCount = 0;
						boolean seamarkNodeFound = false;
						String aId = "";
						String aNodeStr="";
						String qMark ="\"";
						String qMarkAndSpace =qMark+" ";
                        String aFileHeaderStr1 = "<?xml version='1.0' encoding='UTF-8'?>";
                        String aFileHeaderStr2 = "<osm version= " + qMark + "0.6" + qMark + "generator=" + qMark + "osmfilter 1.2P" + qMark + ">";
                        mStringList.add(aFileHeaderStr1);
                        mStringList.add(aFileHeaderStr2);
                        SeamarkNode aSeamarkNode;
                        aSeamarkNode = new SeamarkNode("0000000");
						// Parse the XML returned from the file
						while (parserEvent != XmlPullParser.END_DOCUMENT) {
							switch (parserEvent) {
								case XmlPullParser.START_TAG:
									String tag = parser.getName();

									if (tag.compareTo("node") == 0) {
										String aNodeNumberStr = parser.getAttributeValue(null,"id");
										aId = aNodeNumberStr;
										String aLatStr = parser.getAttributeValue(null,"lat");
										Double aLat = Double.parseDouble(aLatStr);
										int aLatE6 = (int)(aLat * 1E6);
										aSeamarkNode.setLatitudeE6(aLatE6);
										String aLonStr = parser.getAttributeValue(null,"lon");
										Double aLon = Double.parseDouble(aLonStr);
										int aLonE6 = (int)(aLon * 1E6);
										aSeamarkNode.setLongitudeE6(aLonE6);
										
										int countAttr = parser.getAttributeCount();
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<node ");
										for (int indexAttr = 0;indexAttr < countAttr; indexAttr++) {
											
											aStringBuffer.append(parser.getAttributeName(indexAttr));
											aStringBuffer.append("=");
											aStringBuffer.append(qMark);
											aStringBuffer.append(parser.getAttributeValue(indexAttr));
											aStringBuffer.append(qMarkAndSpace);											
										}
										aStringBuffer.append(">");
							
										aNodeStr = aStringBuffer.toString();
										mStringList.add(aNodeStr);
										aSeamarkNode = new SeamarkNode(aNodeNumberStr);
										aSeamarkNode.setLatitudeE6(aLatE6);
										aSeamarkNode.setLatitudeE6(aLonE6);
										
									} 
									if (tag.compareTo("tag") == 0) {
										int countAttr = parser.getAttributeCount();
										
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<tag ");
										for (int indexAttr = 0;2*indexAttr < countAttr; indexAttr++) {
											String aAttributeName = parser.getAttributeName(indexAttr);
											aStringBuffer.append(aAttributeName);
											aStringBuffer.append("=");
											aStringBuffer.append(qMark);
											String aAttributeValue  = parser.getAttributeValue(indexAttr);
											aStringBuffer.append(aAttributeValue);
											aStringBuffer.append(qMarkAndSpace);
											String aSecondAttributeName = parser.getAttributeName(indexAttr +1);
											aStringBuffer.append(aSecondAttributeName);
											aStringBuffer.append("=");
											aStringBuffer.append(qMark);
											String aSecondAttributeValue  = parser.getAttributeValue(indexAttr +1);
											aStringBuffer.append(aSecondAttributeValue);
											aStringBuffer.append(qMarkAndSpace);
											if (aAttributeValue.contains("seamark")){
												Tag aTag = new Tag(aAttributeValue,aSecondAttributeValue);
												aSeamarkNode.addTag(aTag);
												seamarkNodeFound = true;
											}
											
											
										}
										aStringBuffer.append("/>");
										String aTagStr = aStringBuffer.toString();
										
										mStringList.add(aTagStr);
									} 
									if (tag.compareTo("way") == 0) {
										int countAttr = parser.getAttributeCount();
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<way ");
										for (int indexAttr = 0;indexAttr < countAttr; indexAttr++) {
											
											aStringBuffer.append(parser.getAttributeName(indexAttr));
											aStringBuffer.append("=");
											aStringBuffer.append(qMark);
											aStringBuffer.append(parser.getAttributeValue(indexAttr));
											aStringBuffer.append(qMarkAndSpace);											
										}
										aStringBuffer.append(">");
										String aWayString= aStringBuffer.toString();
										mStringList.add(aWayString);
									}	
									if (tag.compareTo("nd")==0){
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<nd ");
										String aAttributeValue  = parser.getAttributeValue(null,"ref");
										aStringBuffer.append("ref=");
										aStringBuffer.append(qMark);
										aStringBuffer.append(aAttributeValue);
										aStringBuffer.append(qMarkAndSpace);
										aStringBuffer.append("/>");
										String ndString = aStringBuffer.toString();
										mStringList.add(ndString);
									}
									if (tag.compareTo("relation")==0){
										int countAttr = parser.getAttributeCount();
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<relation ");
										for (int indexAttr = 0;indexAttr < countAttr; indexAttr++) {
											
											aStringBuffer.append(parser.getAttributeName(indexAttr));
											aStringBuffer.append("=");
											aStringBuffer.append(qMark);
											aStringBuffer.append(parser.getAttributeValue(indexAttr));
											aStringBuffer.append(qMarkAndSpace);											
										}
										aStringBuffer.append(">");
										String aWayString= aStringBuffer.toString();
										mStringList.add(aWayString);
									}
									if (tag.compareTo("member")==0){
										StringBuffer aStringBuffer = new StringBuffer();
										aStringBuffer.append("<member ");
										String aTypeAttributeValue  = parser.getAttributeValue(null,"type");
										aStringBuffer.append("type=");
										aStringBuffer.append(qMark);
										aStringBuffer.append(aTypeAttributeValue);
										aStringBuffer.append(qMarkAndSpace);
										String aRefAttributeValue  = parser.getAttributeValue(null,"ref");
										aStringBuffer.append("ref=");
										aStringBuffer.append(qMark);
										aStringBuffer.append(aRefAttributeValue);
										aStringBuffer.append(qMarkAndSpace);
										String aRoleAttributeValue  = parser.getAttributeValue(null,"role");
										aStringBuffer.append("role=");
										aStringBuffer.append(qMark);
										aStringBuffer.append(aRoleAttributeValue);
										aStringBuffer.append(qMarkAndSpace);
										aStringBuffer.append("/>");
										String ndString = aStringBuffer.toString();
										mStringList.add(ndString);
									}
									
									break;
								case XmlPullParser.END_TAG: {
									tag = parser.getName();

									if (tag.compareTo("node") == 0) {
										if (seamarkNodeFound) {
											StringBuffer buf = new StringBuffer();
											buf.append("<tag ");
											buf.append("k=");
											buf.append(qMark);
											buf.append("addr:housenumber");
											buf.append(qMarkAndSpace); 
											buf.append("v=");
											buf.append(qMark);
											buf.append(aId);
											buf.append(qMarkAndSpace);
											buf.append("/>");
											String tagStr = buf.toString();
											mStringList.add(tagStr);
											seamarkNodeCount++;
											if ((seamarkNodeCount % 10) == 0 ){
										    	Log.d(TAG,"SeamarkNodes " + seamarkNodeCount);
										        
										    }
											mSeamarksNodesList.add(aSeamarkNode);
											mSeamarksDictionary.put(aId, aSeamarkNode);
											seamarkNodeFound = false;
										}
									    mStringList.add("</node> ");
									   
									    nodeCount++;
									    if ((nodeCount % 100) == 0 ){
									    	Log.d(TAG,"Nodes " + nodeCount);
									        
									    }
									} 
									if (tag.compareTo("way") == 0) {
										mStringList.add("</way> ");
										wayCount++;
										if ((wayCount % 100) == 0 ){
									    	Log.d(TAG,"Ways " + wayCount);
									        
									    }
									}
									if (tag.compareTo("relation") == 0) {
										mStringList.add("</relation> ");
										relationCount ++;
										if ((relationCount % 100) == 0 ){
									    	Log.d(TAG,"Relations " + relationCount);
									        
									    }
									}
								}
								break;
							}

							parserEvent = parser.next();
						}
						mStringList.add("</osm>");
						mFileReadHasFinished = true;
						
					} catch (FileNotFoundException e) {
						Log.d(TAG, "File not found");
					} catch (Exception e) {
						Log.i(TAG, "Failed in parsing XML", e);
						//showToastOnUiThread(aUIMsg);

					}
					

				}
			}).start();

		}
}
