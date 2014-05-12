package org.openseamap.openseamapviewer.seamarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.environment.Environment2;
import org.openseamap.openseamapviewer.filefilter.FilterByFileExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.graphics.Matrix;
import android.graphics.Path;
import android.util.Log;




public class SeamarkSymbol {
	public static final String TAG = "SeamarkSymbol";
	private Path mPath;
	private String mSymbolName;
	private String mHeight;
	private String mWidth;
	private String mStyle;
	private String mFill;
	private String mFill_Rule;
	private String mStroke;
	private String mStroke_Width;
	
	
	
	//private static Path cSymbolPath = null;
	//private static String cSymbolName = null;
	
	private static LinkedHashMap<String,SeamarkSymbol> mSymbolsDictionary ;
	
	
	
	 public static long preloadFromDefsFile (String defsFileName) {
		 long usedTime=0; 
		   long aTime= System.currentTimeMillis();
			mSymbolsDictionary = new LinkedHashMap<String,SeamarkSymbol>();
			String externalPathName = Environment2.getCardDirectory().getAbsolutePath();
			
			File aDefsFile = new File(externalPathName + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/"+ defsFileName);
			loadDefs(aDefsFile);
			long diffTime = System.currentTimeMillis() - aTime;
	        Log.d(TAG,"preload defs in " + diffTime + " ms");
	        usedTime = diffTime;
		   return usedTime;
	}
	 
	 public static String[] getKeys() {
			Set<String> aKeySet = mSymbolsDictionary.keySet();
			String[] keys = {};
			keys = aKeySet.toArray(keys);
			return keys;
		}
	 
	public static SeamarkSymbol getSeamarkSymbol(String symbolName) {
		SeamarkSymbol aSeamarkSymbol = null;
		if (mSymbolsDictionary.containsKey(symbolName)){
			aSeamarkSymbol = mSymbolsDictionary.get(symbolName);
		}
		return aSeamarkSymbol;
	}
	 
	private static boolean loadDefs(File aFile){
		boolean fileExists = aFile.exists();
		final String aFilename = aFile.getAbsolutePath();
		Log.d(TAG,"Scanning defs file " + aFilename);
		if (fileExists) {
		 /* new Thread(new Runnable() {

			public void run() {
         */
				try {
					
					XmlPullParserFactory parserCreator;

					parserCreator = XmlPullParserFactory.newInstance();

					XmlPullParser parser = parserCreator.newPullParser();
					FileReader myReader = new FileReader(aFilename);
					parser.setInput(myReader);
					// parser.setInput(text.openStream(), null);

					int parserEvent = parser.getEventType();
					String id ="";
					String width="";
					String height="";
                    Path path=null;
                    String symbolName="";;
					// Parse the XML returned from the file
					while (parserEvent != XmlPullParser.END_DOCUMENT) {
						switch (parserEvent) {
							case XmlPullParser.START_TAG:
								String tag = parser.getName();
								if (tag.compareTo("defs") == 0) {
									
								}
								
								if (tag.compareTo("symbol") == 0) {
									// read symbol definition
									id = parser.getAttributeValue(null,"id");
									width = parser.getAttributeValue(null,"width");
									height = parser.getAttributeValue(null,"height");
									//Log.d(TAG,"id="+ id + " w="+ width + " h="+ height);
									symbolName = id;
									if (symbolName.contains("notice_a10a")) {
										currentSymbol = id;
									}
									//cSymbolName = symbolName;
								} 
								if (tag.compareTo("path") == 0) {
									// read the svg path definition
									String svgPath = parser.getAttributeValue(null,"d");
									//Log.d(TAG,"Create path from svg to: " + symbolName);
									path = convertSVGtoAndroidPath (svgPath);
									//cSymbolPath = path;
								} 
								
								break;
							case XmlPullParser.END_TAG: {
								tag = parser.getName();
								if (tag.compareTo("symbol") == 0) {
									if (!mSymbolsDictionary.containsKey(symbolName)){
										SeamarkSymbol aSeamarkSymbol = new SeamarkSymbol(symbolName,path,width,height);
										mSymbolsDictionary.put(symbolName, aSeamarkSymbol); 
									}
							    }
								
								if (tag.compareTo("path") == 0){
									
									//cSymbolPath.rewind();
								}
										
								
							    break;
							}
						  }

						parserEvent = parser.next();
					}
					
					
					
				} catch (FileNotFoundException e) {
					Log.d(TAG, "File not found " + aFilename);
				} catch (Exception e) {
					Log.i(TAG, "Failed in parsing defs File " + aFilename, e);

				}
			
        /* belongs to the Thread
			}
		}).start();
		*/
				
	  } // if fileExists
		return fileExists;	
	}
	
	
	public static long preloadSomeSymbolsFromDirectory(String[] names) {
		long usedTime = 0;
		mSymbolsDictionary = new LinkedHashMap<String,SeamarkSymbol>();
		String externalPathName = Environment2.getCardDirectory().getAbsolutePath();
		for (int namesIndex = 0;namesIndex < names.length;namesIndex++){
			String symbolName = names[namesIndex];
			if (!mSymbolsDictionary.containsKey(symbolName)) {
				long aTime= System.currentTimeMillis();
				File aFile = new File(externalPathName + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME 
				          + "/svg_symbols/" + symbolName + ".svg");
				if (loadData(aFile)) {	
						
				}
		        long diffTime = System.currentTimeMillis() - aTime;
		        Log.d(TAG,"preload can in " + diffTime + " ms");
		        usedTime += diffTime;
			}
		}
		return usedTime;
	}
	   
	public static long  preloadSymbolsFromDirectory(){
		long usedTime = 0;
		mSymbolsDictionary = new LinkedHashMap<String,SeamarkSymbol>();
		String externalPathName = Environment2.getCardDirectory().getAbsolutePath();
		File aSymbolsDirectory = new File(externalPathName + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/svg_symbols");
		if (aSymbolsDirectory.exists()){
			FilterByFileExtension aFilter = new FilterByFileExtension("svg");	
			File[] files = aSymbolsDirectory.listFiles(aFilter);
			int count = files.length;
			ArrayList<String> namesList = new ArrayList<String>();
			for (int i= 0;i<files.length;i++){
			  String aName = files[i].getName();
			  if (aName.endsWith(".svg")) {
				  aName= aName.substring(0,aName.length()-4);
				  namesList.add(aName);
			  }
			  
			}
			String[] names = {};
			names = namesList.toArray(names);
			for (int namesIndex = 0;namesIndex < names.length;namesIndex++){
				String symbolName = names[namesIndex];
				if (!mSymbolsDictionary.containsKey(symbolName)) {
					long aTime= System.currentTimeMillis();
					File aFile = new File(externalPathName + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME 
					          + "/svg_symbols/" + symbolName + ".svg");
					if (loadData(aFile)) {	
							
					}
			        long diffTime = System.currentTimeMillis() - aTime;
			        Log.d(TAG,"preload can in " + diffTime + " ms");
			        usedTime += diffTime;
				}
			}
			
			
		}
		/*String[] names = {//"Light_Major"
	             "Barrel","Can","Cone","Float","LandTower","Light","Light_House","Light_Major",
	             "Light_Minor","Pillar","Spar","Sphere","Stake",	
        };*/
		
		/*if (!mSymbolsDictionary.containsKey("can")) {
			    long aTime= System.currentTimeMillis();
				loadData(new File(externalPathName + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + "can.svg"));	
		        mSymbolsDictionary.put("can", cSymbolPath);
		        long diffTime = System.currentTimeMillis() - aTime;
		        Log.d(TAG,"preload can in " + diffTime + " ms");
		        usedTime += diffTime;
		}*/
		
 	  return usedTime;
 	   
	}
	
   private static boolean loadData(File aFile) {

		boolean fileExists = aFile.exists();
		final String aFilename = aFile.getAbsolutePath();
		Log.d(TAG,"Scanning svg file " + aFilename);
		if (fileExists) {
		 /* new Thread(new Runnable() {

			public void run() {
         */
				try {
					
					XmlPullParserFactory parserCreator;

					parserCreator = XmlPullParserFactory.newInstance();

					XmlPullParser parser = parserCreator.newPullParser();
					FileReader myReader = new FileReader(aFilename);
					parser.setInput(myReader);
					// parser.setInput(text.openStream(), null);

					int parserEvent = parser.getEventType();
					String xmlnsStr= "";
					String xmlns_xlink="";
					String version="";
					String width="";
					String height="";
					String symbolName="";
					Path path= null;
					// Parse the XML returned from the file
					while (parserEvent != XmlPullParser.END_DOCUMENT) {
						switch (parserEvent) {
							case XmlPullParser.START_TAG:
								String tag = parser.getName();

								if (tag.compareTo("svg") == 0) {
									// read header
									xmlnsStr = parser.getAttributeValue(null,"xmlns");
									xmlns_xlink = parser.getAttributeValue(null,"xmlns:xlink");
									version = parser.getAttributeValue(null,"version");
									width = parser.getAttributeValue(null,"width");
									height = parser.getAttributeValue(null,"height");
									//Log.d(TAG, tag + " " + xmlnsStr + " w=" + width + " h=" + height);
								} 
								if (tag.compareTo("symbol") == 0) {
									// read symbol definition
									String id = parser.getAttributeValue(null,"id");
									width = parser.getAttributeValue(null,"width");
									height = parser.getAttributeValue(null,"height");
									Log.d(TAG,"id="+ id + " w="+ width + " h="+ height);
									symbolName = id;
									
									if (symbolName.contains("notice_a")) {
										currentSymbol = id;
									}
									//cSymbolName = symbolName;
								} 
								if (tag.compareTo("path") == 0) {
									// read the svg path definition
									String svgPath = parser.getAttributeValue(null,"d");
									Log.d(TAG,"Create path from svg to: " + symbolName);
								    path = convertSVGtoAndroidPath (svgPath);
									//cSymbolPath = path;
									
								} 
								if (tag.compareTo("use")==0){
									String xlink_href= parser.getAttributeValue(null,"xlink:href");
									//og.d(TAG,"xlink:href="+ xlink_href);
								}
								
								
								break;
							case XmlPullParser.END_TAG: {
								tag = parser.getName();

								if (tag.compareTo("svg") == 0) {
				 
								    }
								} 
								if (tag.compareTo("symbol") == 0) {
									if (!mSymbolsDictionary.containsKey(symbolName)){
										SeamarkSymbol aSeamarkSymbol = new SeamarkSymbol(symbolName,path,width,height);
										mSymbolsDictionary.put(symbolName, aSeamarkSymbol); 
									}
							    }
								
								if (tag.compareTo("path") == 0){
									
								}
										
								
							    break;
						  }

						parserEvent = parser.next();
					}
					
					
					
				} catch (FileNotFoundException e) {
					Log.d(TAG, "File not found " + aFilename);
				} catch (Exception e) {
					Log.d(TAG, "Failed in parsing SVG File " + aFilename, e);

				}
			
        /* belongs to the Thread
			}
		}).start();
		*/
				
	  } // if fileExists
		return fileExists;	
	}
   
   private static String s;
   private static int index;
   private static float current_x;
   private static float current_y;
   private static boolean inFirstMove = false;
   private static Path path = null;
   
   private static String currentSymbol ;
   
   
   private static float readFloat() {
	  char c = s.charAt(index);
	  while ((c == ' ' || c == ',') && index < s.length()) {
		  index++;
		  c=s.charAt(index);
	  }
	  float result = 0f; 
	  boolean isNegative = false;
	  if (c == '-')  {
		  isNegative = true;
		  index++;
		  c= s.charAt(index);
	  }
	  StringBuffer buf = new StringBuffer();
	  while ((index < s.length())&& (c == '0' || c == '1' || c == '2' || c=='3' || c == '4' || c == '5' || c=='6' || c == '7' || c == '8' || c=='9' || c == 'e' || c =='E'|| c == '.' )) {
		 buf.append(c);
		 if (c=='e'|| c=='E'){ // if we have an e notation we must read the next char, it maybe a - sign
			 index++;
			 c=s.charAt(index); 
			 buf.append(c);
		 }
		 index++;
		 c= s.charAt(index);
	  }
	  try {
		 String aResultStr = buf.toString();
		 if (isNegative){
			 result= -Float.parseFloat(aResultStr);
		 } else {
			 result= Float.parseFloat(aResultStr); 
		 }
	  }catch (Exception e) {
		  
	  }
	  return result;
   }
   
   private static void handleNextFloat(char lastCmd) {
	   if (lastCmd == 'm') { // the moves behind the first m are lines
		   float dx = readFloat();
		   float dy = readFloat();
		   current_x += dx; 
		   current_y += dy;
    	   path.rLineTo(dx,dy);
    	  
	   }
	   if (lastCmd == 'l'){
		   float dx = readFloat();
		   float dy = readFloat();
		   current_x += dx; 
		   current_y += dy;
    	   path.rLineTo(dx,dy); 
    	   
	   }
	   if (lastCmd == 'h'){
		   float dx = readFloat();
		   current_x += dx; 
    	   path.rLineTo(dx,0f); 
    	   
	   }
	   if (lastCmd == 'v'){
		   float dy = readFloat();
		   current_y += dy; 
    	   path.rLineTo(0f,dy); 
    	  
	   }
	   if (lastCmd =='c') {
			float  dx1 = readFloat();
		    float  dy1 = readFloat();
		     
		    float dx2 = readFloat();
		    
		    float dy2 = readFloat();
		     
		    float dx = readFloat();
		    
		    float dy = readFloat();
		     current_x+=dx;
			 current_y+=dy;
		     path.rCubicTo(dx1, dy1, dx2,dy2,dx, dy);
		      
	   }
	    if (lastCmd=='a'){
			 float rx = readFloat();
		   
		     float ry1 = readFloat();
		     
		     float xRot = readFloat();
		     
		     float largeArcFlag = readFloat();
		     
		     float sweepArcFlag = readFloat();
		     
		     float dx = readFloat();
		     float dy = readFloat();
		     current_x+=dx;
			 current_y+=dy;
		     path.rLineTo(dx, dy);
		     
	    }
	   if (lastCmd == 's') {
		   float dx2 = readFloat();
		     
		   float dy2 = readFloat();
		     
		  float   dx1 = -dx2;
		  float   dy1 = -dy2;
		     
		  float   dx = readFloat();
		     
		  float   dy = readFloat();
		     current_x+=dx;
			 current_y+=dy;
		     path.rCubicTo(dx1, dy1, dx2,dy2,dx, dy);
		      
	   }
	   if (lastCmd == 'M') { // the moves behind the first M are lines
		   float dx = readFloat();
		   
		   float dy = readFloat();
		   current_x = dx; 
		   current_y = dy;
    	   path.lineTo(dx,dy);
    	
	   }
	   if (lastCmd == 'L'){
		   float dx = readFloat();
		   
		   float dy = readFloat();
		   current_x = dx; 
		   current_y = dy;
    	   path.lineTo(dx,dy); 
    	  
	   }
	   if (lastCmd == 'H'){
		   float dx = readFloat();
		   current_x = dx; 
    	   path.lineTo(dx,current_y); 
    	  
	   }
	   if (lastCmd == 'V'){
		   float dy = readFloat();
		   current_y = dy; 
    	   path.lineTo(current_x,dy); 
    	
	   }
	   if (lastCmd =='C') {
		   
			float  dx1 = readFloat();
		    
		    float dy1 = readFloat();
		     
		    float dx2 = readFloat();
		     
		    float dy2 = readFloat();
		     
		    float dx = readFloat();
		     
		    float dy = readFloat();
		     current_x=dx;
			 current_y=dy;
		     path.cubicTo(dx1, dy1, dx2,dy2,dx, dy);
		     
	   }
	   
	   if (lastCmd=='A'){
			 float rx = readFloat();
		   
		     float ry1 = readFloat();
		     
		     float xRot = readFloat();
		     
		     float largeArcFlag = readFloat();
		     
		     float sweepArcFlag = readFloat();
		     
		     float x = readFloat();
		     float y = readFloat();
		     current_x=x;
			 current_y=y;
		     
		     
	    }
	   if (lastCmd == 'S') {
		   float dx2 = readFloat();
		     
		   float dy2 = readFloat();
		     
		  float   dx1 = -dx2;
		  float   dy1 = -dy2;
		     
		  float   dx = readFloat();
		    
		  float   dy = readFloat();
		     current_x=dx;
			 current_y=dy;
		     path.cubicTo(dx1, dy1, dx2,dy2,dx, dy);
		    
	   }
   }

   private static Path convertSVGtoAndroidPath(String svgPath) {
	   //String outerLineTest = "m 159.48,160.26 -28.062,-8.8438 -15.219,45.75 h-7.9688 v 5.6562 h 29.688 c1.2873,5.533 6.2333,9.6562 12.156,9.6562 5.925,0 10.871,-4.1205 12.156,-9.6562 h 29.844 ";
	   char aChar;
	   boolean parseError = false;
	   s= svgPath + " ";
	   path = new Path();
	   inFirstMove = true;
	   try {
		  char cmd;
		  char lastCmd = ' ';
		  float dx = 0;
		  float dy = 0;
		  float dx1 = 0;
		  float dy1 = 0;
		  float dx2 = 0;
		  float dy2 = 0;
		  index = 0;
		  while (index < s.length()&& !parseError)  {
			aChar = s.charAt(index);
			
			switch (aChar)
			{
				case ' ': index++;break;
				case 'm': cmd = aChar; lastCmd = cmd; index++; 
				      dx = readFloat();
				      
				      dy = readFloat();
				      if (inFirstMove) {
				    	  current_x = dx; // in svg the first move is absolute
						  current_y = dy;
				    	  path.moveTo(current_x,current_y);
				    	  inFirstMove = false;
				      } else {
				    	  current_x += dx; 
						  current_y += dy;
				    	  path.rMoveTo(dx,dy);
				      }
				      
				   break;
				case 'l':
					
					 cmd = aChar; lastCmd = cmd;; index++;
					 dx = readFloat();
				    
				     dy = readFloat();
				     current_x+=dx;
					 current_y+=dy;
	               //Log.d(TAG,"rLineTo (" + rx + "," + ry +")");
	               //Log.d(TAG,"current x,y (" + current_x + "," + current_y +")");
	                 path.rLineTo(dx,dy); 
	                 
	             break;
				case 'c' :
					 cmd = aChar; lastCmd = cmd;; index++;
					 dx1 = readFloat();
				   
				     dy1 = readFloat();
				     
				     dx2 = readFloat();
				     
				     dy2 = readFloat();
				     
				     dx = readFloat();
				     
				     dy = readFloat();
				     current_x+=dx;
					 current_y+=dy;
				     path.rCubicTo(dx1, dy1, dx2,dy2,dx, dy);
				     
				  break;
				case 'a' :
					 cmd = aChar; lastCmd = cmd;; index++;
					 float rx = readFloat();
				   
				     float ry1 = readFloat();
				     
				     float xRot = readFloat();
				     
				     float largeArcFlag = readFloat();
				     
				     float sweepArcFlag = readFloat();
				     
				     dx = readFloat();
				     dy = readFloat();
				     current_x+=dx;
					 current_y+=dy;
				     path.rLineTo(dx, dy);
				     
				  break;
				case 's' :
					 cmd = aChar; lastCmd = cmd;; index++;
					 dx2 = readFloat();
				     
				     dy2 = readFloat();
				     
				     dx1 = -dx2;
				     dy2 = -dy2;
				     
				     dx = readFloat();
				     
				     dy = readFloat();
				     current_x+=dx;
					 current_y+=dy;
				     path.rCubicTo(dx1, dy1, dx2,dy2,dx, dy);
				     
				  break;
				case 'h':
					cmd = aChar; lastCmd = cmd;; index++;
					dx = readFloat();
					current_x+=dx;
					path.rLineTo(dx, 0f);
					
			      break;
				case 'v':
					cmd = aChar; lastCmd = cmd;; index++;
					dy = readFloat();
					current_y+=dy;
					path.rLineTo(0f, dy);
					
			      break;
				case 'z':      

				    cmd = aChar; lastCmd = cmd; index++;
					path.close(); 
					 
				 break; 
				case 'M': cmd = aChar; lastCmd = cmd; index++; 
			      dx = readFloat();
			      
			      dy = readFloat();
		    	  current_x = dx; // in svg the first move is absolute
				  current_y = dy;
		    	  path.moveTo(current_x,current_y);
			      
			   break;
			case 'L':
				 cmd = aChar; lastCmd = cmd;; index++;
				 dx = readFloat();
			     
			     dy = readFloat();
			     current_x=dx;
				 current_y=dy;
             //Log.d(TAG,"rLineTo (" + rx + "," + ry +")");
             //Log.d(TAG,"current x,y (" + current_x + "," + current_y +")");
               path.lineTo(dx,dy); 
               
           break;
			case 'C' :
				 cmd = aChar; lastCmd = cmd;; index++;
				 dx1 = readFloat();
			     
			     dy1 = readFloat();
			     
			     dx2 = readFloat();
			     
			     dy2 = readFloat();
			     
			     dx = readFloat();
			     
			     dy = readFloat();
			     current_x=dx;
				 current_y=dy;
			     path.cubicTo(dx1, dy1, dx2,dy2,dx, dy);
			     
			  break;
			case 'S' :
				 cmd = aChar; lastCmd = cmd;; index++;
				 dx2 = readFloat();
			     
			     dy2 = readFloat();
			     
			     dx1 = -dx2;
			     dy2 = -dy2;
			     
			     dx = readFloat();
			     
			     dy = readFloat();
			     current_x=dx;
				 current_y=dy;
			     path.cubicTo(dx1, dy1, dx2,dy2,dx, dy);
			     
			  break;
			case 'A' :
				 cmd = aChar; lastCmd = cmd;; index++;
				 rx = readFloat();
			   
			     ry1 = readFloat();
			     
			     xRot = readFloat();
			     
			     largeArcFlag = readFloat();
			     
			     sweepArcFlag = readFloat();
			     
			     dx = readFloat();
			     dy = readFloat();
			     current_x+=dx;
				 current_y+=dy;
			     path.lineTo(dx, dy);
			     
			  break;
			case 'H':
				cmd = aChar; lastCmd = cmd;; index++;
				dx = readFloat();
				current_x=dx;
				path.lineTo(dx, current_y);
				
		      break;
			case 'V':
				cmd = aChar; lastCmd = cmd;; index++;
				dy = readFloat();
				current_y=dy;
				path.lineTo(current_x, dy);
				
		      break;
			
			case 'Z':      
				cmd = aChar; lastCmd = cmd; index++;
				path.close(); 
				
			 break;
			 
					
				case '0' : handleNextFloat(lastCmd);  break;
				case '1' : handleNextFloat(lastCmd);  break;
				case '2' : handleNextFloat(lastCmd);  break;
				case '3' : handleNextFloat(lastCmd);  break;
				case '4' : handleNextFloat(lastCmd);  break;
				case '5' : handleNextFloat(lastCmd);  break;
				case '6' : handleNextFloat(lastCmd);  break;
				case '7' : handleNextFloat(lastCmd);  break;
				case '8' : handleNextFloat(lastCmd);  break;
				case '9' : handleNextFloat(lastCmd);  break;
				case '-' : handleNextFloat(lastCmd);  break;
			default:
				parseError = true;
				path.reset();
				String info = "unhandled command " + aChar + " " + currentSymbol;
				Log.d(TAG,info);
				String hStr = s.substring(0,index); 
				Log.d(TAG," handled commands " + hStr);
			}	//switch
		  } // while
	   } catch (Exception e) {
		   parseError = true;
		   path.reset();
		   Log.d(TAG,e.toString());
	   }
	   return path;
   }

private static Path convertSVGtoAndroidPathwithPattern(String svgPath) {
	 // does not work with all files
	  
	   Pattern patDelimiter = Pattern.compile("[\\s,]");
	   //Pattern patDelimiter = Pattern.compile("(\\s|\\p{Alpha}|[,])");
	  
	   float current_x = 0;
	   float current_y = 0;
	   float factor_x = 1.0f;
       float factor_y = 1.0f;
       boolean firstMtoParse= true;
	 /*  // Teststring for a can, outerline is drawn countclockwise, inner clockwise, third is the inner ring
	   String outerLineTest = "m 159.48,160.26 -28.062,-8.8438 -15.219,45.75 h-7.9688 v 5.6562 h 29.688 c1.2873,5.533 6.2333,9.6562 12.156,9.6562 5.925,0 10.871,-4.1205 12.156,-9.6562 h 29.844";
	   //outerLineTest = outerLineTest + " " +"v -0.0625 -5.5938 h -7.6875 l 9,-26.188 -29.938,-9.4373 -3.9688,-1.2812 z";
	   String innerLineTest ="m -23.031,-0.21875 47.156,15.969 -7.2188,21.156 h -14.156 c -1.2853,-5.5357 -6.2313,-9.6562 -12.156,-9.6562 -5.923,10e-6 -10.869,4.1233 -12.156,9.6562";
	   innerLineTest = innerLineTest + " " + "h -13.719 l 12.25,-37.125 z";
	   String thirdContour ="m 13.625,32.438 c 4.14,0 7.5,3.36 7.5,7.5 0,4.14 -3.36,7.5 -7.5,7.5 -4.14,0 -7.5,-3.36 -7.5,-7.5 0,-4.14 3.36,-7. 7.5,-7.5 z";
	   //Scanner scanner = new Scanner(outerLineTest + " " +innerLineTest + " " + thirdContour);
*/	   
       // the file may not be well structured, so add a white space after and each in front of the delimiter
       String testStr = svgPath;
	   String testStr1 = "";
	   StringBuffer buf = new StringBuffer();
	   for (int index= 0;index < testStr.length()-1; index++) {
		   String aTestCharString = testStr.substring(index,index+1);
		   if  ("MmCcHhVvLlsSzZ-".contains(aTestCharString)) {
			   if ((testStr.charAt(index + 1)!= ' ') && (testStr.charAt(index - 1) != ',') && (testStr.charAt(index -1) != ' ')) {
				   testStr1 = testStr1+ " ";
				   buf.append(" ");
				   testStr1 = testStr1+ aTestCharString;
				   buf.append(aTestCharString);
				   if (!aTestCharString.equals("-" )) {
					   buf.append(" ");
					   testStr1 = testStr1+ " ";
					   };
				   
			   } else {
				   buf.append(aTestCharString);
				   testStr1 = testStr1+ aTestCharString;
			   }
		   }
	       else {
		      buf.append(aTestCharString);
		      testStr1 = testStr1+ aTestCharString;
	       }
	   }
	   String testStr2 = buf.toString();
	   boolean error = false;
	   Path path = new Path();
	   String lastCmd = "";
	   try {
		   Scanner scanner = new Scanner(testStr2);
		   scanner.useLocale(Locale.US);
		   scanner.useDelimiter(patDelimiter);
		  
		   while (scanner.hasNext()&& !error){
			   String cmd = scanner.next();
			   lastCmd = cmd;
			   if (cmd.equals("")){
				  
			   } else
			   if (cmd.equals("c")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
					  float rx1 = scanner.nextFloat();
					  
					  float ry1 = scanner.nextFloat();
					  float rx2 = scanner.nextFloat();
					 
					  float ry2 = scanner.nextFloat();
					  float rx3 = scanner.nextFloat();
					 
					  float ry3 = scanner.nextFloat();
					  current_x+=rx3;
					  current_y+=ry3;
	              // Log.d(TAG,"rCubicTo (" + rx1 + "," + ry1 +")" + "( " + rx2 + "," + ry2 +")"  + " (" + rx3 + "," + ry3 +")");
	               path.rCubicTo(factor_x*rx1, factor_y*ry1, factor_x*rx2, factor_y*ry2,factor_x*rx3, factor_y*ry3);
				   }
			   } else 
			   
			   if (cmd.equals("h")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
						  float rx = scanner.nextFloat();
						  current_x+=rx;
		                  //Log.d(TAG,"rHorizontalto (" + rx + ")" + " is rLineTo ( " + + rx + ",0.0)" ); 
		                  path.rLineTo(factor_x*rx, 0f);
					   } 
			   } else 
			   if (cmd.equals("l")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
					  float rx = scanner.nextFloat();
					 
					  float ry = scanner.nextFloat();
					  current_x+=rx;
					  current_y+=ry;
	               //Log.d(TAG,"rLineTo (" + rx + "," + ry +")");
	               //Log.d(TAG,"current x,y (" + current_x + "," + current_y +")");
	               path.rLineTo(factor_x*rx,factor_y*ry);
				   }
			   } else 
			   if (cmd.equals("m")){
				   scanner.skip(" ");
				   if (scanner.hasNextFloat()){
					  float dx= scanner.nextFloat();
					  
					  float dy= scanner.nextFloat();
					  
		               //Log.d(TAG,"rMoveto (" + x + "," + y +")");
		               if (firstMtoParse) {
		            	   //Log.d(TAG,"Start x,y (" + dx + "," + dy +")");
		            	   current_x = dx; // in svg the first move is absolute
		 				   current_y = dy;
		            	   path.moveTo(current_x,current_y);
		            	   firstMtoParse=false;
		               } else {
		            	   //Log.d(TAG,"rmove x,y (" + dx + "," + dy +")");
		            	   current_x+=dx;
		 				   current_y+=dy;
		            	   path.rMoveTo(factor_x*dx,factor_y*dy); 
		               }
		               while (scanner.hasNextFloat()){
		 				  float rx = scanner.nextFloat();
		 				  
		 				  float ry = scanner.nextFloat();
		 				  current_x+=rx;
		 				  current_y+=ry;
		                   //Log.d(TAG,"rLineTo (" + rx + "," + ry +")");
		                   path.rLineTo(factor_x*rx,factor_y*ry);
		               }
				   } 
			   } else 
			   if (cmd.equals("s")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
					  // we have to calculate the first control point as the reflection of the second point relative to the current point
					  // first read the data for point 2 
					  float rx2 = scanner.nextFloat();
					 
					  float ry2 = scanner.nextFloat();;
					  float rx1 = -rx2;
					  float ry1 = ry2;
					  float rx3 = scanner.nextFloat();
					  
					  float ry3 = scanner.nextFloat();
					  current_x+=rx3;
					  current_y+=ry3;
	                  // Log.d(TAG,"rCubicTo (" + rx1 + "," + ry1 +")" + "( " + rx2 + "," + ry2 +")"  + " (" + rx3 + "," + ry3 +")");
	                  path.rCubicTo(factor_x*rx1, factor_y*ry1, factor_x*rx2, factor_y*ry2,factor_x*rx3, factor_y*ry3);
				   }
			   } else 
			   if (cmd.equals("v")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
						  float ry = scanner.nextFloat();
						  current_y+=ry;
		                  //Log.d(TAG,"rVerticalto (" + ry + ")" + " is rLineTo (0.0 "  + ry + ")");
		                  path.rLineTo(0f, factor_y*ry);
					   } 
			   } else 
			   if (cmd.equals("z")|| cmd.equals("Z")){
			       //Log.d(TAG,"close contour");
			       //Log.d(TAG,"curent x,y (" + current_x + "," + current_y +")");
			       path.close();
			   } else
				   if (cmd.equals("H")){
					   scanner.skip(" ");
					   while (scanner.hasNextFloat()){
							  float x = scanner.nextFloat();
							  current_x=x;
			                  //Log.d(TAG,"rHorizontalto (" + rx + ")" + " is rLineTo ( " + + rx + ",0.0)" ); 
			                  path.rLineTo(factor_x*x, 0f);
						   } 
				   } else 
				   if (cmd.equals("L")){
					   scanner.skip(" ");
					   while (scanner.hasNextFloat()){
						  float x = scanner.nextFloat();
						  
						  float y = scanner.nextFloat();
						  current_x=x;
						  current_y=y;
		               //Log.d(TAG,"rLineTo (" + rx + "," + ry +")");
		               //Log.d(TAG,"current x,y (" + current_x + "," + current_y +")");
		               path.rLineTo(factor_x*x,factor_y*y);
					   }
				   } else 
			   if (cmd.equals("M")){
				   scanner.skip(" ");
				   if (scanner.hasNextFloat()){
					  float x= scanner.nextFloat();
					  
					  float y= scanner.nextFloat();
					  current_x = x; 
					  current_y = y;
		        	  path.moveTo(current_x,current_y);
		        	  firstMtoParse=false;
				   }
			   } else
			   if (cmd.equals("C")){  
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
					  float x1 = scanner.nextFloat();
					  
					  float y1 = scanner.nextFloat();
					  float x2 = scanner.nextFloat();
					  
					  float y2 = scanner.nextFloat();
					  float x = scanner.nextFloat();
					  
					  float y = scanner.nextFloat();
					  current_x=x;
					  current_y=y;
	                  Log.d(TAG,"CubicTo (" + x1 + "," + y1 +")" + "( " + x2 + "," + y2 +")"  + " (" + x + "," + y +")");
	                  path.cubicTo(x1,y1, x2, y2,x, y);
				   }
			   } else 
			   if (cmd.equals("S")){
				   scanner.skip(" ");
				   while (scanner.hasNextFloat()){
					  // we have to calculate the first control point as the reflection of the second point relative to the current point
					  // first read the data for point 2 
					  float x2 = scanner.nextFloat();
					  
					  float y2 = scanner.nextFloat();
					  float dx = current_x - x2;
					  float x1 = - dx;
					  float dy = current_y - y2;
					  float y1 = -dy;
					  float x3 = scanner.nextFloat();
					  
					  float y3 = scanner.nextFloat();
					  current_x=x3;
					  current_y=y3;
	              // Log.d(TAG,"rCubicTo (" + rx1 + "," + ry1 +")" + "( " + rx2 + "," + ry2 +")"  + " (" + rx3 + "," + ry3 +")");
	               path.cubicTo(x1, y1, x2, y2,x3, y3);
				   }
			    } else
			    if (cmd.equals("V")){
					   scanner.skip(" ");
					   while (scanner.hasNextFloat()){
							  float y = scanner.nextFloat();
							  current_y=y;
			                  //Log.d(TAG,"rVerticalto (" + ry + ")" + " is rLineTo (0.0 "  + ry + ")");
			                  path.rLineTo(0f, factor_y*y);
						   } 
			    } else
			      {
				    Log.d(TAG,"unhandeld command "+ cmd );
				    path.reset();
				    error = true;
			    }
			  
		   } // while
		   
	   }
	   catch (InputMismatchException e) {
	        Log.d(TAG, lastCmd + " " + e.toString());
	   } catch (NoSuchElementException e) {
		   Log.d(TAG, lastCmd + " " + e.toString());
	   } 
	   catch (Exception e) {
		   Log.d(TAG, lastCmd + " " + e.toString());
	   } finally {
		   
	   }
	   
	   if (!error) {
		   Log.d(TAG,"svg scan finished");
	   } else {
		   Log.d(TAG,"svg scan broke with error");
	   }
	   return path;

}
   
  
   
   private SeamarkSymbol(String symbolName,Path aPath, String width, String height) {
	   mSymbolName = symbolName;
	   mWidth = width;
	   mHeight = height;
	   if (symbolName.equals("light")){
		 float centerX = getWidth() / 2;
		 float centerY = getHeight() / 2;
		 Matrix matrix = new Matrix();
  		 matrix.setRotate(135,centerX,centerY);
  	     path.transform(matrix);
		}
	   mPath = aPath;
   }
   
   public Path getPath() {
	   return mPath;
   }
   
  
   public String getName() {
	   return mSymbolName;
   }
   
   public float getWidth() {
	  float result = 0;
	   try {
		   result =Float.parseFloat(mWidth);
	   } catch (Exception e){}
	   return result;
   }
   
   public float getHeight() {
	   float result = 0;
	   try {
		   result =Float.parseFloat(mHeight);
	   } catch (Exception e){}
	   return result;
	  
   }
   
  

   
}
