package org.openseamap.openseamapviewer.seamarks;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.PathShape;

public class SeamarkDrawable {
   private static final String TAG = "SeamarkDrawable";
   private SeamarkNode mSeamarkNode= null;
   private Drawable mDrawable= null;
   private Paint mPaint; 
   private float mSymbolWidth = 0.0f;
   private float mSymbolHeight = 0.0f;
   private float mTopBasisX = 0.0f;
   private float mTopBasisY = 0.0f;
   private float mTopRotate = 0.0f;
   private int mBitmapWidth = 0;
   private int mBitmapHeight = 0;
   private Canvas mCanvas = null;
   private float mFactor = 0.5f;
   private Bitmap mSeamarkBitmap = null;
   private int mBase = 20;
   private byte mZoomLevel = 0;
   //private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
   private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
   private float  mDisplayFactor = 1.0f;
   
   public SeamarkDrawable (SeamarkNode aSeamarkNode, byte zoomLevel , float pDisplayFactor){
	   mSeamarkNode = aSeamarkNode;
	   mZoomLevel = zoomLevel;
	   mDisplayFactor =pDisplayFactor;
	   String id = aSeamarkNode.getId();
	   float base = 20.0f;
	   if (zoomLevel < 14 ) {
		   base = 16.0f;
	   }
	   if (zoomLevel < 12 ) {
		   base = 12.0f;
	   }
	   if (zoomLevel < 10 ) {
		   base = 8.0f;
	   }
	   base = base * pDisplayFactor; 
	   mBase = (int) base;
	   mBitmapWidth = mBase*3;
	   mBitmapHeight = mBase*4;
	   mSeamarkBitmap = Bitmap.createBitmap(mBitmapWidth,mBitmapHeight,mBitmapConfig);
	   mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	   mPaint.setStyle(Paint.Style.STROKE);
	   mPaint.setStrokeWidth(2);
	   mPaint.setColor(Color.BLACK);
	   mCanvas = new Canvas(mSeamarkBitmap);  
	   //drawCenterAndFrame(mCanvas); // only for help
	   String seamarkType = mSeamarkNode.getValueToKey("seamark:type");
	   if (seamarkType != null) {
		   if (seamarkType.contains("buoy")){
			      chooseBuoyType(seamarkType);
			      mDrawable = new BitmapDrawable(mSeamarkBitmap);
		   }
		   if (seamarkType.contains("beacon")){
			      chooseBeaconType(seamarkType);
			      mDrawable = new BitmapDrawable(mSeamarkBitmap);
		   }
		   
		   
		   if (seamarkType.equals("landmark")) {
			   handleLandMark(); 
			   mDrawable = new BitmapDrawable(mSeamarkBitmap);
		   }
           if (seamarkType.equals("light_minor")) {
        	   handleLightMinor ( ) ;
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
		   }
           
           if (seamarkType.equals("light_major")) {
        	   handleLightMajor () ;
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
		   }
           
           if (seamarkType.endsWith("harbour")) {
        	   handleHabour ( ) ; 
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.endsWith("small_craft_facility")) {
        	   handleSmallCraftFacility () ; 
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.endsWith("mooring")) {
        	   handleMooring () ; 
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("wreck")) {
        	   handleWreck () ; 
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           if (seamarkType.equals("bridge")){
        	   if (zoomLevel <= 12 ) {
        		   handleBridgeWithSymbol();
        	   }else {
        		   handleBridgeWithInfo();
        	   }
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("cable_overhead")){
        	   handleCableOverhead();
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           if (seamarkType.equals("distance_mark")){
        	   handleDistance_mark();
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("anchorage")){
        	   handleAnchorage();
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("notice")){
        	   handleNotice();
        	   //mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("sounding")){
                handleDefaultDepthSounding();
           }
           
           if (seamarkType.equals("platform")) {
        	   handlePlatform();
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
           if (seamarkType.equals("rock")) {
        	   handleRock(mCanvas);
        	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
           }
           
	   }
	   		
   }
   
   private void handleRock(Canvas canvas) {
	   String waterLevelInfo = mSeamarkNode.getValueToKey("seamark:rock:water_level");
	   int bitmapWidth = canvas.getWidth();
	    int bitmapHeight = canvas.getHeight();
	// a visible Rectangle with a cross to show the venter of the bitmap
	    RectF rectF = new RectF(1,1,bitmapWidth - 1, bitmapHeight-1);
	    Paint paint = new Paint();
	    paint.setStrokeWidth(2);
        paint.setStyle(Style.STROKE);
	    paint.setColor(Color.BLUE);
	    /*canvas.drawRect(rectF, paint);
	    canvas.drawLine(bitmapWidth/2,0,bitmapWidth/2,bitmapHeight,paint);
	    canvas.drawLine(0,bitmapHeight/2,bitmapWidth,bitmapHeight/2, paint); */
	   if (waterLevelInfo != null && mZoomLevel > 12){ 
		   if (waterLevelInfo.equals("awash")) {
			   paint.setColor(Color.WHITE);
			   paint.setStyle(Style.FILL);
			   canvas.drawCircle(bitmapWidth / 2, bitmapHeight / 2, 9, paint);
			   paint.setStyle(Style.FILL);
			   paint.setColor(Color.BLACK);
			   int delta = 7;
			   int pDelta = 5;
			   int pWidth = 5;
			   canvas.drawLine(bitmapWidth/2 -delta ,bitmapHeight / 2,bitmapWidth/2 + delta ,bitmapHeight /2 ,paint); 
			   canvas.drawLine(bitmapWidth/2  ,bitmapHeight / 2 -delta ,bitmapWidth/2  ,bitmapHeight /2 +delta ,paint);
			   canvas.drawLine(bitmapWidth/2 -pDelta ,bitmapHeight / 2 -pWidth ,bitmapWidth/2 -pDelta ,bitmapHeight /2 -(pWidth-1) ,paint); 
			   canvas.drawLine(bitmapWidth/2 -pDelta ,bitmapHeight / 2 +pWidth ,bitmapWidth/2 -pDelta ,bitmapHeight /2 +(pWidth -1) ,paint);
			   canvas.drawLine(bitmapWidth/2 +pDelta,bitmapHeight / 2 -pWidth  ,bitmapWidth/2 +pDelta ,bitmapHeight /2 -(pWidth -1) ,paint); 
			   canvas.drawLine(bitmapWidth/2 +pDelta,bitmapHeight / 2 +pWidth  ,bitmapWidth/2 +pDelta ,bitmapHeight /2 +(pWidth -1) ,paint);
		   }
	   }
   }
   
   private void handleDefaultDepthSounding (){
	      mPaint.setStyle(Style.FILL);  
		  mPaint.setColor(Color.WHITE);
		  float centerX = mBitmapWidth *0.5f;
		  float centerY = mBitmapHeight *0.5f; 
		  float base = mBase * 0.7f;
		  float base08 = base * 0.8f;
		  float base03 = base * 0.3f;
		  // draw a black frame 
		  /*rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
		  mPaint.setColor(Color.RED); 
		  mCanvas.drawOval(rectF,mPaint); // draw  a inner red frame
		  mPaint.setStrokeWidth(1);*/
		  mPaint.setTextSize(14);
		  Typeface savedTypeFace = mPaint.getTypeface();
		  mPaint.setTypeface(Typeface.MONOSPACE);
		  mPaint.setColor(Color.BLACK);
		  String aDepthSoundingStr = mSeamarkNode.getValueToKey("seamark:sounding");
          if (aDepthSoundingStr != null ) {
        	  float halfTextWidth = mPaint.getTextSize() / 2.0f;
        	  if (base < halfTextWidth / 2.0f ) {
        		  base = halfTextWidth + 2.0f;
        	  }
        	  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
    		  //mCanvas.drawOval(rectF,mPaint);
        	  mPaint.setStrokeWidth(1.0f);
    		  mPaint.setStyle(Style.FILL);
    		  mPaint.setColor(Color.BLACK);
    		  //mCanvas.drawOval(rectF,mPaint);  
        	  mCanvas.drawText(aDepthSoundingStr, centerX-base *0.7f, centerY +base *0.5f, mPaint);
          }
          mPaint.setTypeface(savedTypeFace);
   
}
   
   private void chooseBuoyType(String seamarkType) {
	   if (seamarkType.equals("buoy_lateral")) {
		   String type = "buoy_lateral";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "can";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		   //handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		  //handleType_buoy_lateral(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
	   } 
	   if (seamarkType.equals("buoy_cardinal")) {
		      //handleType_buoy_cardinal(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		   String type = "buoy_cardinal";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "pillar";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		   //handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
	    }
	   if (seamarkType.equals("buoy_safe_water")) {
		      //handleType_buoy_safe_water(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		   String type = "buoy_safe_water";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "pillar";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		   //handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
	   }
	   
	   if (seamarkType.equals("buoy_isolated_danger")) {
		   String type = "buoy_isolated_danger";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "pillar";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		  // handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		}
	   if (seamarkType.equals("buoy_special_purpose")) {
			  //handleType_buoy_special_purpose(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		   String type = "buoy_special_purpose";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "pillar";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		   //handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			  //handleType_special_purpose("buoy_special_purpose",aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			 
	   }
   }
   
   private void chooseBeaconType(String seamarkType) {
	   if (seamarkType.equals("beacon_lateral")) {
		   String type = "beacon_lateral";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   /*if ((shapeName != null) && (shapeName.equals("stake"))){
			  Log.d(TAG,"stake found"); 
		   }*/
		  /* if ((shapeName != null) && (shapeName.equals("pile"))){
			  Log.d(TAG,"pile found"); 
		   }*/
		   if ((shapeName != null) && (shapeName.equals("perch"))){
			   String category = mSeamarkNode.getValueToKey("seamark:"+type+":category" );
			   if (category != null && category.equals("starboard")) shapeName="perchS";
			   if (category != null && category.equals("port")) shapeName ="perchP";
		   }
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "beacon";
		   }
		   
		   handleType_buoy_or_beacon(type,shapeName);
		   //handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			  //handleType_beacon_lateral(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			
		   }
	   
	  
	   if (seamarkType.equals("beacon_cardinal")) {
		      //handleType_buoy_cardinal(aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
		   String type = "beacon_cardinal";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "beacon";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		  // handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			 
		   }
	   
	   if (seamarkType.equals("beacon_isolated_danger")) {
		   String type = "beacon_isolated_danger";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "beacon";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		  // handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			 
		   }
	   
	   
	   if (seamarkType.equals("beacon_special_purpose")) {  // many of them have no shape defined
		   String type = "beacon_special_purpose";
		   String shapeName = mSeamarkNode.getValueToKey("seamark:"+type+":shape");
		   if ((shapeName == null) || (SeamarkSymbol.getSeamarkSymbol(shapeName)== null)){
			   shapeName = "beacon";
		   }
		   if ((shapeName != null)&& shapeName.equals("post")) {
			   shapeName = "beacon";
		   }
		   handleType_buoy_or_beacon(type,shapeName);
		  // handleType_buoy_or_beacon(type,shapeName,aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			  //handleType_special_purpose("beacon_special_purpose",aSeamarkNode,canvas,bitmapWidth,bitmapHeight);
			 
	   } 
   }
   
   private void drawCenterAndFrame(Canvas canvas){
	    int bitmapWidth = canvas.getWidth();
	    int bitmapHeight = canvas.getHeight();
	// a visible Rectangle with a cross to show the venter of the bitmap
	    RectF rectF = new RectF(1,1,bitmapWidth - 1, bitmapHeight-1);
	    Paint paint = new Paint();
	    paint.setStrokeWidth(2);
        paint.setStyle(Style.STROKE);
	    paint.setColor(Color.BLUE);
	    canvas.drawRect(rectF, paint);
	    canvas.drawLine(bitmapWidth/2,0,bitmapWidth/2,bitmapHeight,paint);
	    canvas.drawLine(0,bitmapHeight/2,bitmapWidth,bitmapHeight/2, paint); 
}
   
   private void setLightColour(String aColour){
	   mPaint.setColor(Color.BLACK);
	   if (aColour == null) return;
	   if (aColour.equals("green")) {
		   mPaint.setColor(Color.GREEN);
	   } else 
	   if (aColour.equals("red")) {
		   mPaint.setColor(Color.RED); 
	   }
	   else 
	   if (aColour.equals("yellow")) {
		   mPaint.setColor(Color.YELLOW);
	   } else 
	   if (aColour.equals("white")) {
		   mPaint.setColor(Color.YELLOW);
	   }   
   }
   
   private void setColour(String aColour){
	   mPaint.setColor(Color.BLACK);
	   if (aColour == null) return;
	   if (aColour.equals("green")) {
		   mPaint.setColor(Color.GREEN);
	   } else 
	   if (aColour.equals("red")) {
		   mPaint.setColor(Color.RED); 
	   }
	   else 
	   if (aColour.equals("yellow")) {
		   mPaint.setColor(Color.YELLOW);
	   } else 
	   if (aColour.equals("white")) {
		   mPaint.setColor(Color.WHITE);
	   }   
   }
   
   private void setTopBasisFromShape(String shapeName){
	   mTopBasisX = 132;
	   mTopBasisY = 45;
	   mTopRotate = 0;
	   if (shapeName.equals("pillar")) {
		   mTopBasisX = 175;
		   mTopBasisY = 20;
		   mTopRotate = 17;
	   }
	   if (shapeName.equals("beacon")) {
		   mTopBasisX = 130;
		   mTopBasisY = 45;
		   mTopRotate = 0;
	   }
	   
	   if (shapeName.equals("stake")) {
		   mTopBasisX = 130;
		   mTopBasisY = 45;
		   mTopRotate = 0;
	   }
	   if (shapeName.equals("spar")) {
		   mTopBasisX = 175;
		   mTopBasisY = 20;
		   mTopRotate = 17;
	   }
	   
	   
	   if (shapeName.equals("conical")) {
		   mTopBasisX = 145;
		   mTopBasisY = 80;
		   mTopRotate = 17;
	   }
	   if (shapeName.equals("barrel")) {
		   mTopBasisX = 145;
		   mTopBasisY = 80;
		   mTopRotate = 17;
	   }
	   if (shapeName.equals("can")) {
		   mTopBasisX = 145;
		   mTopBasisY = 80;
		   mTopRotate = 17;
	   }
	   if (shapeName.equals("sperical")) {
		   mTopBasisX = 145;
		   mTopBasisY = 80;
		   mTopRotate = 17;
	   }
	   if (shapeName.equals("super-buoy")) {
		   mTopBasisX = 145;
		   mTopBasisY = 80;
		   mTopRotate = 0;
	   }
   }
   
   private void handleHabour (){
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("marina");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.MAGENTA);
		       aPathShape.draw(mCanvas, mPaint);
		   }
	   }
   }
   
   
   
   private void handleWreck() {
	  
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("wreck_d");
	   
	   /*
	    * <path d="M 10,40 L 90,40 M 50,10 L 50,70 M 30,25 L 30,55 M 70,25 L 70,55" 
	    */
	  /* Path path = new Path();
	   path.moveTo(10,40);path.lineTo(90,40);path.moveTo(50,10);path.lineTo(50,70);
	   path.moveTo(30,25);path.lineTo(30,55);path.moveTo(70,25);path.lineTo(70,55);
	   mSymbolWidth = 100;
	   mSymbolHeight = 80;
	   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
       aPathShape.resize(bitmapWidth ,bitmapHeight);
	   //aPathShape.resize(mSymbolWidth, mSymbolHeight);
       mPaint.setStrokeWidth(5);
       //mPaint.setStyle(Style.FILL);
       mPaint.setStyle(Paint.Style.STROKE);
       mPaint.setColor(Color.BLACK);
       aPathShape.draw(canvas, mPaint);*/
	   
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   //path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   mBitmapWidth = mBase;
			   mBitmapHeight = mBase;
			   mSeamarkBitmap = Bitmap.createBitmap(mBitmapWidth,mBitmapHeight,mBitmapConfig);
			   mCanvas = new Canvas(mSeamarkBitmap);
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
			   //aPathShape.resize(mSymbolWidth, mSymbolHeight);
		       mPaint.setStrokeWidth(2);
		       mPaint.setStyle(Paint.Style.STROKE);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
	   } 
		  
   }
   private void handleSmallCraftFacility (){
	   
	   String category = mSeamarkNode.getValueToKey("seamark:small_craft_facility:category");
	   
	   if (category == null) {
		   String category2 = mSeamarkNode.getValueToKey("seamark:small_craft_facility:2:category"); 
		   if (category2 != null) {
			   category = category2;
		   }
	   } 
       if (category != null) {
		   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol(category);
		   int base = 4;
		   mBitmapWidth = base*4;
		   mBitmapHeight = base*4;
		   mSeamarkBitmap = Bitmap.createBitmap(mBitmapWidth,mBitmapHeight,mBitmapConfig);
		   mCanvas = new Canvas(mSeamarkBitmap);
		   if (aSymbol!= null) {
			   Path path = aSymbol.getPath();
			   if (path!= null) {
				   path.setFillType(FillType.INVERSE_EVEN_ODD);
				  /* mSymbolWidth = aSymbol.getWidth();
				   mSymbolHeight = aSymbol.getHeight();
				   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
			       aPathShape.resize(bitmapWidth ,bitmapHeight);*/
				   float symbolWidth = aSymbol.getWidth();
				   float symbolHeight = aSymbol.getHeight();
				   PathShape aPathShape = new PathShape(path, symbolWidth,symbolHeight);
				   float aFactor = mBitmapWidth / symbolWidth;
			       //aPathShape.resize(symbolWidth / aFactor ,symbolHeight / aFactor);
			       aPathShape.resize(mBitmapWidth,mBitmapHeight);
			       mPaint.setStrokeWidth(2);
			       mPaint.setStyle(Style.FILL);
			       mPaint.setColor(Color.BLUE);
			       aPathShape.draw(mCanvas, mPaint);
			   }
		   } 
	   }
	   
   }
   
   private void handleNotice(){
	   String noticeCategory = mSeamarkNode.getValueToKey("seamark:notice:category");
	   if (noticeCategory == null) {  // there are some numbered notices we do not handle yet
		   mDrawable = null;
 		   mSeamarkBitmap = null; 
 		   return;
	   }
	      float base = mBase * 0.7f;
		  float base08 = base * 0.8f;
		  float base03 = base * 0.3f;
	   if (noticeCategory.equals("no_entry")){
		// we handle seperate as it has no red frame and red diagonal line
		   mPaint.setStyle(Style.FILL);  
			  mPaint.setColor(Color.WHITE);
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
			  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
			  mPaint.setColor(Color.RED); 
			  mCanvas.drawRect(rectF,mPaint); // draw  a inner red frame
			  mPaint.setStyle(Style.FILL);
			  mPaint.setColor(Color.RED);
			  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY -base03);
			  mCanvas.drawRect(rectF,mPaint);
			  rectF.set(centerX-base08,centerY +base03, centerX+base08, centerY +base08);
			  mCanvas.drawRect(rectF,mPaint);
			  mDrawable = new BitmapDrawable(mSeamarkBitmap);
		  return;
	   }
	   // we handle seperate as it is round
	   if (noticeCategory != null &&noticeCategory.equals("closed_area") ) {
		      
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  mPaint.setStyle(Style.FILL);
			  mPaint.setColor(Color.RED);
			  mCanvas.drawCircle(centerX,centerY, base,mPaint);
			  mPaint.setStyle(Style.FILL);
			  mPaint.setColor(Color.WHITE);
			  RectF rectF = new RectF(centerX-base,centerY -base*0.2f, centerX+base, centerY +base*0.2f);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawCircle(centerX,centerY, base,mPaint);
			  mDrawable = new BitmapDrawable(mSeamarkBitmap);
			  return;
	   }
	   if (mZoomLevel < 14){
 		   mDrawable = null;
 		   mSeamarkBitmap = null;
 		   return;
 	   }
	   if ( noticeCategory.equals("no_mooring")||noticeCategory.equals("no_anchoring") || noticeCategory.equals("no_motor_craft") 
		    || noticeCategory.equals("no_sport_craft") || noticeCategory.equals("no_sailing_craft")|| noticeCategory.equals("no_unpowered_craft")) {
		        handleNoticeA(noticeCategory);
	   }else if (noticeCategory.equals("stop")||noticeCategory.equals("speed_limit")|| noticeCategory.equals("make_radio_contact")) {
		   handleNoticeB(noticeCategory);
	   }
	   else if (noticeCategory.equals("limited_headroom")) {
		   handleNoticeC(noticeCategory);
	   }
	   else if (noticeCategory.equals("weir") || noticeCategory.equals("mooring_permitted")
			   || noticeCategory.equals("radio_information")) {
		   handleNoticeE(noticeCategory);
	   }else {
		   mSeamarkBitmap  = null;
		   // handleDefaultNotice();
	   }
	   
	   
   }
   
   private void handleDefaultNotice(){
	      mPaint.setStyle(Style.FILL);  
		  mPaint.setColor(Color.WHITE);
		  float centerX = mBitmapWidth *0.5f;
		  float centerY = mBitmapHeight *0.5f; 
		  float base = mBase * 0.7f;
		  float base08 = base * 0.8f;
		  float base03 = base * 0.3f;
		  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
		  mCanvas.drawRect(rectF,mPaint);
		  mPaint.setStyle(Style.STROKE);
		  mPaint.setColor(Color.BLACK);
		  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
		  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
		  mPaint.setColor(Color.RED); 
		  mCanvas.drawRect(rectF,mPaint); // draw  a inner red frame
		  mPaint.setStrokeWidth(1);
		  mPaint.setTextSize(14);
		  mPaint.setColor(Color.BLACK);
          mCanvas.drawText("?", centerX-base *0.7f, centerY +base *0.5f, mPaint);
         
   }
   
   private void handleNoticeA(String noticeCategory) {
	   if (noticeCategory != null) {
		      mPaint.setStyle(Style.FILL);  
			  mPaint.setColor(Color.WHITE);
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  float base = mBase * 0.7f;
			  float base08 = base * 0.8f;
			  float base03 = base * 0.3f;
			  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
			  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
			  mPaint.setColor(Color.RED); 
			  mCanvas.drawRect(rectF,mPaint); // draw  a inner red frame
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setStrokeWidth(3);
			  mPaint.setStrokeCap(Cap.ROUND);
			  mPaint.setColor(Color.RED);   // draw red diagonal line
			  mCanvas.drawLine(centerX-base08, centerY -base08, centerX +base08, centerY+base08, mPaint);
			  ArrayList<SeamarkSymbol> symbolsPartList = new ArrayList<SeamarkSymbol>();
			  SeamarkSymbol aSymbol = null;
			  Matrix moveMatrix = new Matrix();
  		       
			  if (noticeCategory.equals("no_anchoring")) {
				  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_anchor");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
			  } else if (noticeCategory.equals("no_mooring")) {
				  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard1");// three symbols to combine
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard2");// three symbols to combine
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard3");// three symbols to combine
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
			  } else if ( noticeCategory.equals("no_motor_craft")){
				  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_motor");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
		      } else if ( noticeCategory.equals("no_sport_craft")){
				  mPaint.setStrokeWidth(1);
				  mPaint.setTextSize(6);
				  mPaint.setColor(Color.BLACK);
                  mCanvas.drawText("SPORT", centerX-base *0.7f, centerY +base *0.5f, mPaint);
		      }
		      else if ( noticeCategory.equals("no_sailing_craft")){
		    	  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_sailboat1");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_sailboat2");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
				  
		      } else if ( noticeCategory.equals("no_unpowered_craft")){
		    	  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_rowboat1");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_rowboat2");
				  if (aSymbol != null) symbolsPartList.add(aSymbol);
		      } // end of if else 
			  // // now look if there is anything in the symbolsPartList to drwa
			  int count = symbolsPartList.size();
			  if (count > 0){
				   mSymbolWidth = aSymbol.getWidth();
				   mSymbolHeight = aSymbol.getHeight();
				   
				   for (int index = 0; index < count; index++){
					  aSymbol = symbolsPartList.get(index);
					  if (aSymbol!=null){
						  Path path = aSymbol.getPath();
						   if (path!= null) {
							   
				   		       Path transformedPath = new Path();
						       path.transform(moveMatrix,transformedPath);
					           PathShape aPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
							   path.setFillType(FillType.EVEN_ODD);
							   mSymbolWidth = aSymbol.getWidth();
							   mSymbolHeight = aSymbol.getHeight();
						       aPathShape.resize(mBitmapWidth*0.3f ,mBitmapHeight*0.3f);
						       mPaint.setColor(Color.BLACK);
						       mPaint.setStyle(Style.FILL);
						       aPathShape.draw(mCanvas, mPaint);
						   }
					  } // aSymbol != null
				  }
			  }
	      mDrawable = new BitmapDrawable(mSeamarkBitmap);	 
	   } // noticecategory != null
   }
   
   private void handleNoticeB(String noticeCategory) {
	   if (noticeCategory != null) {
		      mPaint.setStyle(Style.FILL);  
			  mPaint.setColor(Color.WHITE);
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  float base = mBase * 0.7f;
			  float base08 = base * 0.8f;
			  float base03 = base * 0.3f;
			  float base06 = base * 0.6f;
			  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
			  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
			  mPaint.setColor(Color.RED);
			  mCanvas.drawRect(rectF,mPaint); // draw  a inner red frame
			  /* mPaint.setStyle(Style.STROKE);
			  mPaint.setStrokeWidth(3);
			  mPaint.setStrokeCap(Cap.ROUND);
			  mPaint.setColor(Color.RED);
			  mCanvas.drawLine(centerX-base08, centerY -base08, centerX +base08, centerY+base08, mPaint);  // draw red line from top left to bottom right
              */
			  if ( noticeCategory.equals("stop")){  // v="stop" B5 seamark:notice:function=regulation" 
				  mPaint.setStyle(Style.STROKE);
				  mPaint.setStrokeWidth(6);
				  mPaint.setStrokeCap(Cap.ROUND);
				  mPaint.setColor(Color.BLACK);
				  mCanvas.drawLine(centerX-base03,centerY,centerX+base03,centerY,mPaint);
		      } else if (noticeCategory.equals("speed_limit")){
				  String speedLimit = mSeamarkNode.getValueToKey("seamark:notice:information");
				  if (speedLimit != null){
					  mPaint.setTextSize(14);
					  mPaint.setStrokeWidth(1);
					  mPaint.setColor(Color.BLACK);
					  mPaint.setTextAlign(Align.CENTER);
					  mCanvas.drawText(speedLimit,centerX ,centerY +base03, mPaint);
				  }
			  }else if (noticeCategory.equals("make_radio_contact")) {
				  String channel = mSeamarkNode.getValueToKey("seamark:notice:information");
				  if (channel != null){
					  mPaint.setColor(Color.BLACK);
					  mPaint.setTextSize(8);
					  mPaint.setStrokeWidth(1);
					  mPaint.setTextAlign(Align.CENTER);
					  
					  //mCanvas.drawText("VHF", centerX - 6, centerY, mPaint);
					  String fields[] = channel.split(" ");
					  if (fields.length ==2) {
						  mCanvas.drawText(fields[0],centerX ,centerY -base*0.1f, mPaint);
						  mCanvas.drawText(fields[1],centerX ,centerY + base06, mPaint); 
					  } else {
						  mCanvas.drawText(channel,centerX ,centerY , mPaint);
					  }
					  
				  }
			  }
		  mDrawable = new BitmapDrawable(mSeamarkBitmap);
	   } // noticeCategory != null
   }
   
   private void handleNoticeC(String noticeCategory){
	   if (noticeCategory != null) {
		      mPaint.setStyle(Style.FILL);  
			  mPaint.setColor(Color.WHITE);
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  float base = mBase * 0.7f;
			  float base08 = base * 0.8f;
			  float base03 = base * 0.3f;
			  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
			  rectF.set(centerX-base08,centerY -base08, centerX+base08, centerY +base08);
			  mPaint.setColor(Color.RED);
			  mCanvas.drawRect(rectF,mPaint);
			  SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_c2");
			  if (aSymbol!=null && noticeCategory.equals("limited_headroom")){
				  Path path = aSymbol.getPath();
				   if (path!= null) {
					   mSymbolWidth = aSymbol.getWidth();
					   mSymbolHeight = aSymbol.getHeight();
					   Matrix moveMatrix = new Matrix();
		   		       moveMatrix.postTranslate(70,70);
		   		       Path transformedPath = new Path();
				       path.transform(moveMatrix,transformedPath);
			           PathShape aPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
					   path.setFillType(FillType.EVEN_ODD);
					   mSymbolWidth = aSymbol.getWidth();
					   mSymbolHeight = aSymbol.getHeight();
				       aPathShape.resize(mBitmapWidth*0.3f ,mBitmapHeight*0.3f);
				       mPaint.setColor(Color.BLACK);
				       mPaint.setStyle(Style.FILL);
				       aPathShape.draw(mCanvas, mPaint);
				   }
				   String headroom = mSeamarkNode.getValueToKey("seamark:notice:information");
					  if (headroom != null){
						  mPaint.setColor(Color.BLACK);
						  mPaint.setTextSize(9);
						  mPaint.setTextAlign(Align.CENTER);
						  mCanvas.drawText(headroom,centerX ,centerY + base *0.6f, mPaint);
					  }   
			  }
		  mDrawable = new BitmapDrawable(mSeamarkBitmap);
	   } 
   }
   
   private void handleNoticeE(String noticeCategory){
	   if (noticeCategory != null) {
		      mPaint.setStyle(Style.FILL);  
			  mPaint.setColor(Color.BLUE);
			  float centerX = mBitmapWidth *0.5f;
			  float centerY = mBitmapHeight *0.5f; 
			  float base = mBase * 0.7f;
			  float base03 = base * 0.3f;
			  float base06 = base * 0.6f;
			  float base07 = base * 0.7f;
			  float base08 = base * 0.8f;
			  float base09 = base *0.9f;
			  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
			  mCanvas.drawRect(rectF,mPaint);
			  mPaint.setStyle(Style.STROKE);
			  mPaint.setColor(Color.BLACK);
			  mCanvas.drawRect(rectF,mPaint);  // draw a black frame 
			  rectF.set(centerX-base09,centerY -base09, centerX+base09, centerY +base09);
			  mPaint.setColor(Color.WHITE);
			  mCanvas.drawRect(rectF,mPaint);
			  ArrayList<SeamarkSymbol> symbolsPartList = new ArrayList<SeamarkSymbol>();
			  SeamarkSymbol aSymbol = null;
			  Matrix moveMatrix = new Matrix();
  		      
			  if ( noticeCategory.equals("weir")){
				  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_e3"); 
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
				  
			  }else if (noticeCategory.equals("mooring_permitted")) {
				  moveMatrix.postTranslate(70,70);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard1"); 
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard2"); 
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
				  aSymbol = SeamarkSymbol.getSeamarkSymbol("notice_bollard3");
				  if (aSymbol!=null) symbolsPartList.add(aSymbol);
			  } // mooring permitted
			  else if ( noticeCategory.equals("radio_information")) {
				  String channel = mSeamarkNode.getValueToKey("seamark:notice:information");
				  if (channel != null){
					  mPaint.setColor(Color.WHITE);
					  mPaint.setTextSize(9);
					  mPaint.setStrokeWidth(1);
					  mPaint.setTextAlign(Align.CENTER);
					  //mCanvas.drawText("VHF", centerX - 6, centerY, mPaint);
					  String fields[] = channel.split(" ");
					  if (fields.length ==2) {
						  mCanvas.drawText(fields[0],centerX ,centerY -base*0.1f, mPaint);
						  mCanvas.drawText(fields[1],centerX ,centerY + base07, mPaint); 
					  } else {
						  mCanvas.drawText(channel,centerX - base06,centerY , mPaint);
					  }
					  
				  }
			  }
			// // now look if there is anything in the symbolsPartList to draw
			  int count = symbolsPartList.size();
			  if (count > 0){
				   mSymbolWidth = aSymbol.getWidth();
				   mSymbolHeight = aSymbol.getHeight();
				   
				   for (int index = 0; index < count; index++){
					  aSymbol = symbolsPartList.get(index);
					  if (aSymbol!=null){
						  Path path = aSymbol.getPath();
						   if (path!= null) {
				   		       Path transformedPath = new Path();
						       path.transform(moveMatrix,transformedPath);
					           PathShape aPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
							   path.setFillType(FillType.EVEN_ODD);
							   mSymbolWidth = aSymbol.getWidth();
							   mSymbolHeight = aSymbol.getHeight();
						       aPathShape.resize(mBitmapWidth*0.3f ,mBitmapHeight*0.3f);
						       mPaint.setColor(Color.WHITE);
						       mPaint.setStyle(Style.FILL);
						       aPathShape.draw(mCanvas, mPaint);
						   }
					  } // aSymbol != null
				  }
			  }
	   mDrawable = new BitmapDrawable(mSeamarkBitmap);
	   }
   }
   
   
   
   
   private void handlePlatform(){
	      mPaint.setStyle(Style.STROKE);
		  mPaint.setColor(Color.BLACK);
		  float centerX = mBitmapWidth *0.5f;
		  float centerY = mBitmapHeight *0.5f; 
		  float base = mBase * 0.7f;
		  float base08 = base * 0.8f;
		  float base03 = base * 0.3f;
		  RectF rectF = new RectF(centerX-base,centerY -base, centerX+base, centerY +base);
		  mCanvas.drawRect(rectF,mPaint);
		  String platformColourStr = mSeamarkNode.getValueToKey("seamark:platform:colour");
		  if (platformColourStr != null){
			 setColour(platformColourStr);
			 mPaint.setStyle(Style.FILL);
			 mCanvas.drawRect(rectF,mPaint);
		  }
   }
   
   private void handleCableOverhead(){
	   
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("cable");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   /*bitmapWidth = mBase;
			   bitmapHeight = mBase;
			   seamarkBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,mBitmapConfig);
			   canvas = new Canvas(seamarkBitmap);*/
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
			   //aPathShape.resize(mSymbolWidth, mSymbolHeight);
		       mPaint.setStrokeWidth(2);
		       mPaint.setStyle(Paint.Style.STROKE);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
	   }  
	   String verticalClearanceStr = mSeamarkNode.getValueToKey("seamark:cable_overhead:vertical_clearance_safe");
	   
			  if (verticalClearanceStr != null) {
				  mPaint.setStyle(Style.FILL);
				  mPaint.setColor(Color.WHITE);
				  float centerX = mBitmapWidth *0.5f;
				  float centerY = mBitmapHeight *0.5f;
				  mPaint.setStrokeWidth(1);
				  mCanvas.drawCircle( centerX,centerY,15, mPaint);
				  mPaint.setColor(Color.BLACK);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,15, mPaint);
				  mPaint.setStrokeWidth(2);
				  float deltaHorizontal = 5.0f;
				  float deltaVertical = 11.0f;
				  mCanvas.drawLine(centerX - deltaHorizontal, centerY - deltaVertical,centerX + deltaHorizontal, centerY - deltaVertical, mPaint);
				  //mCanvas.drawLine(centerX, centerY - deltaVertical ,centerX, centerY + deltaVertical, mPaint);
				  mCanvas.drawLine(centerX - deltaHorizontal, centerY + deltaVertical,centerX + deltaHorizontal, centerY + deltaVertical, mPaint);
				  mPaint.setStyle(Style.FILL);
				  mPaint.setTextAlign(Align.CENTER);
				  mCanvas.drawText(verticalClearanceStr, centerX ,centerY + 5, mPaint);
			  }
		  
   }
   
private void handleDistance_mark(){
	
	//    <tag k="seamark:distance_mark:distance" v="2344"/>
	//    <tag k="seamark:distance_mark:units" v="kilometres"/>
	//     <tag k="seamark:type" v="distance_mark"/>

	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("distance_mark");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   /*bitmapWidth = mBase;
			   bitmapHeight = mBase;
			   seamarkBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,mBitmapConfig);
			   canvas = new Canvas(seamarkBitmap);*/
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
			   //aPathShape.resize(mSymbolWidth, mSymbolHeight);
		       mPaint.setStrokeWidth(2);
		       mPaint.setStyle(Paint.Style.STROKE);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
	   }  
	   if (mZoomLevel >= 13) {
	   String distanceMarkStr = mSeamarkNode.getValueToKey("seamark:distance_mark:distance");
	   
			  if (distanceMarkStr != null) {
				  //mPaint.setStyle(Style.FILL);
				  //mPaint.setColor(Color.WHITE);
				  float centerX = mBitmapWidth *0.5f;
				  float centerY = mBitmapHeight *0.5f;
				  mPaint.setStrokeWidth(1);
				  //mCanvas.drawCircle( centerX,centerY,15, mPaint);
				  mPaint.setColor(Color.BLACK);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,3, mPaint);
				  mPaint.setStrokeWidth(2);
				  //float deltaHorizontal = 5.0f;
				  //float deltaVertical = 11.0f;
				  //mCanvas.drawLine(centerX - deltaHorizontal, centerY - deltaVertical,centerX + deltaHorizontal, centerY - deltaVertical, mPaint);
				  //mCanvas.drawLine(centerX, centerY - deltaVertical ,centerX, centerY + deltaVertical, mPaint);
				  //mCanvas.drawLine(centerX - deltaHorizontal, centerY + deltaVertical,centerX + deltaHorizontal, centerY + deltaVertical, mPaint);
				  mPaint.setStyle(Style.FILL);
				  mPaint.setTextAlign(Align.CENTER);
				  float savedTextSize = mPaint.getTextSize();
				  mPaint.setTextSize(10);
				  mCanvas.drawText(distanceMarkStr, centerX ,centerY + 15, mPaint);
				  mPaint.setTextSize(savedTextSize);
			  }
	   }
		  
   }
   
   private void handleBridgeWithSymbol() {
   String bridgeCategory = mSeamarkNode.getValueToKey("seamark:bridge:category");
	   
	   if (bridgeCategory != null) {
		  mPaint.setStyle(Style.FILL);
		  mPaint.setColor(Color.WHITE);
		  float centerX = mBitmapWidth *0.5f;
		  float centerY = mBitmapHeight *0.5f;
		  float deltaX = 8.0f;
		  float deltaY = 6.0f;
		  RectF aRect = new RectF();
		  aRect.set(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
		  mCanvas.drawOval(aRect, mPaint);
		  //mCanvas.drawCircle( centerX,centerY,radius, mPaint);
		  if (bridgeCategory.equals("fixed")) {
			  mPaint.setColor(Color.RED); 
		  }
		  else if (bridgeCategory.equals("lifting")) {
			  mPaint.setColor(Color.RED); 
		  }
          else if (bridgeCategory.equals("opening")) {
        	  mPaint.setColor(Color.GREEN);
		  } else {
			  mPaint.setColor(Color.BLACK);
		  }
		  mPaint.setStyle(Style.STROKE);
		  mCanvas.drawOval(aRect, mPaint);
	   }// bridgecategory != null
   }
   
   private void handleBridgeWithInfo() {
	   String bridgeCategory = mSeamarkNode.getValueToKey("seamark:bridge:category");
	   
	   if (bridgeCategory != null) {
		   String info = "  ?  "; // there may be a category that we do not handle or a fault in the data, see e.g nordhollands kanal Burgervlotbrug
		   if (bridgeCategory.equals("fixed"))  {  
			  String clearance_heightStr = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height");
			  if (clearance_heightStr != null) {
				 info = clearance_heightStr;
			  }
		   }
		   if (bridgeCategory.equals("opening"))  {
			  String clearance_height_closedStr = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_closed");
			  if (clearance_height_closedStr != null) {
				  info = clearance_height_closedStr +"/ - ";
			  }
		   }
		   if (bridgeCategory.equals("lifting"))  {
				  String clearance_height_close_Str = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_closed");
				  String clearance_height_open_Str = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_open");
				  if (clearance_height_close_Str != null && clearance_height_open_Str  != null) {
					info = clearance_height_close_Str + "/" + clearance_height_open_Str;
				  }
		   }
			
		  float textwidth = 20.0f;
		  float widthOfText = mPaint.measureText(info); 
		  if (widthOfText > textwidth  ) {
			  textwidth = widthOfText;
		  }
		// we make a white Circle with red or black frame
		  mPaint.setStyle(Style.FILL);
		  mPaint.setColor(Color.WHITE);
		  float centerX = mBitmapWidth *0.5f;
		  float centerY = mBitmapHeight *0.5f;
		  float deltaX = textwidth / 2.0f + 2.0f;
		  float deltaY = 12.0f;
		  float distY = deltaY - 3;
		  float radius = 20.0f;
		  RectF aRect = new RectF();
		  aRect.set(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
		  mCanvas.drawOval(aRect, mPaint);
		  //mCanvas.drawCircle( centerX,centerY,radius, mPaint);
		  if (bridgeCategory.equals("opening"))  {  
			  mPaint.setColor(Color.GREEN);
		  } else {
		     mPaint.setColor(Color.RED);
		  }
		  mPaint.setStyle(Style.STROKE);
		  mCanvas.drawOval(aRect, mPaint);
		  //mCanvas.drawCircle( centerX,centerY,radius, mPaint);
		  mPaint.setColor(Color.BLACK);
		  mCanvas.drawLine(centerX - 5, centerY - distY,centerX + 5, centerY - distY, mPaint);
		  mCanvas.drawLine(centerX, centerY - distY ,centerX, centerY - 7, mPaint);
		  mCanvas.drawLine(centerX, centerY + 7 ,centerX, centerY +distY, mPaint);
		  mCanvas.drawLine(centerX - 5, centerY + distY,centerX + 5, centerY + distY, mPaint);
		  mPaint.setStyle(Style.FILL);
		  mPaint.setTextAlign(Align.CENTER);
		  if (info.length() != 0)  {  
			  mCanvas.drawText(info, centerX ,centerY + 5, mPaint);
		  } else {
			  mCanvas.drawLine(centerX, centerY - 11 ,centerX, centerY + 11, mPaint); 
		  }
		     
	   } // bridgecategory != null
   }
   
   // we used the old symbols to 13_04_04
   private void handleBridgeOld() {
	   String bridgeCategory = mSeamarkNode.getValueToKey("seamark:bridge:category");
	   if (bridgeCategory != null) {
		   if (bridgeCategory.equals("fixed"))  {
				// we make a white Circle with red frame
				  mPaint.setStyle(Style.FILL);
				  mPaint.setColor(Color.WHITE);
				  float centerX = mBitmapWidth *0.5f;
				  float centerY = mBitmapHeight *0.5f;
				  mCanvas.drawCircle( centerX,centerY,15, mPaint);
				  mPaint.setColor(Color.RED);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,15, mPaint);
				  mCanvas.drawLine(centerX - 5, centerY - 11,centerX + 5, centerY - 11, mPaint);
				  //mCanvas.drawLine(centerX, centerY - 11 ,centerX, centerY + 11, mPaint);
				  mCanvas.drawLine(centerX - 5, centerY + 11,centerX + 5, centerY + 11, mPaint);
				  
				  String clearance_heightStr = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height");
				  if (clearance_heightStr != null) {
					 mPaint.setStyle(Style.FILL);
					 mPaint.setTextAlign(Align.CENTER);
				     mCanvas.drawText(clearance_heightStr, centerX ,centerY + 5, mPaint);
			      } else {
			    	  mCanvas.drawLine(centerX, centerY - 11 ,centerX, centerY + 11, mPaint); 
			      }
		   }
		   if (bridgeCategory.equals("opening"))  {
			   // we make a white Circle with black frame
			      mPaint.setStyle(Style.FILL);
				  mPaint.setColor(Color.WHITE);
				  float centerX = mBitmapWidth *0.5f;
				  float centerY = mBitmapHeight *0.5f;
				  mCanvas.drawCircle( centerX,centerY,20, mPaint);
				  mPaint.setColor(Color.BLACK);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,20, mPaint);
				  // the bridge can be open
				  mCanvas.drawLine(centerX - 11, centerY - 11, centerX + 5, centerY - 16, mPaint);
				  mCanvas.drawLine(centerX - 13, centerY +  3, centerX + 7, centerY -  4, mPaint);
				  mCanvas.drawLine(centerX - 9, centerY - 12 ,centerX -9, centerY + 3, mPaint);
				  mCanvas.drawLine(centerX + 1, centerY - 14 ,centerX + 1, centerY + 1, mPaint);
				  
				  mCanvas.drawLine(centerX - 5, centerY + 4 ,centerX + 5, centerY +4, mPaint);
				  //mCanvas.drawLine(centerX, centerY +5 ,centerX, centerY + 16, mPaint); if we have info no vertical line
				  mCanvas.drawLine(centerX - 5, centerY + 17,centerX + 5, centerY + 17, mPaint);
				  String clearance_height_closedStr = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_closed");
				  if (clearance_height_closedStr != null) {
					  // the bridge can be closed 
					  
					  mPaint.setStyle(Style.FILL);
					  mPaint.setTextAlign(Align.CENTER);
					  mCanvas.drawText(clearance_height_closedStr, centerX ,centerY + 15, mPaint);
				  } else {
					  mCanvas.drawLine(centerX, centerY +7 ,centerX, centerY + 17, mPaint);
				  }
			 }  // opening
		   
		   if (bridgeCategory.equals("lifting"))  {
			   // we make two  white Circle with red frame
				  mPaint.setStyle(Style.FILL);
				  mPaint.setColor(Color.WHITE);
				  float centerX = mBitmapWidth *0.5f;
				  float centerY = mBitmapHeight *0.5f;
				  // left circle closed
				  float radius = 13;
				  centerX = centerX - radius *1.3f;
				  mCanvas.drawCircle( centerX,centerY,radius, mPaint);
				  mPaint.setColor(Color.RED);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,radius, mPaint);
				  float delta = 8;
				  float horizontalDelta = 5.0f;
				  mCanvas.drawLine(centerX - horizontalDelta, centerY - delta,centerX + horizontalDelta, centerY - delta, mPaint);
				  //mCanvas.drawLine(centerX, centerY - 11 ,centerX, centerY + 11, mPaint);
				  mCanvas.drawLine(centerX - horizontalDelta, centerY + delta,centerX + horizontalDelta, centerY + delta, mPaint);
				  String clearance_heightStr = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_closed");
				  if (clearance_heightStr != null) {
					 mPaint.setStyle(Style.FILL);
					 mPaint.setTextAlign(Align.CENTER);
				     mCanvas.drawText(clearance_heightStr, centerX ,centerY + 5, mPaint);
			      } else {
			    	  mCanvas.drawLine(centerX, centerY - delta ,centerX, centerY + delta, mPaint); 
			      }
				  
				  // right circle open (lifted)
				  centerX = centerX + radius * 2 * 1.3f;
				  mPaint.setStyle(Style.FILL);
				  mPaint.setColor(Color.WHITE);
				  mCanvas.drawCircle( centerX,centerY,radius, mPaint);
				  mPaint.setColor(Color.RED);
				  mPaint.setStyle(Style.STROKE);
				  mCanvas.drawCircle( centerX,centerY,radius, mPaint);
				  mCanvas.drawLine(centerX - horizontalDelta, centerY - delta,centerX + horizontalDelta, centerY - delta, mPaint);
				  //mCanvas.drawLine(centerX, centerY - 11 ,centerX, centerY + 11, mPaint);
				  mCanvas.drawLine(centerX - horizontalDelta, centerY + delta,centerX + horizontalDelta, centerY + delta, mPaint);
				  String clearance_height_open_Str = mSeamarkNode.getValueToKey("seamark:bridge:clearance_height_open");
				  if (clearance_heightStr != null) {
					 mPaint.setStyle(Style.FILL);
					 mPaint.setTextAlign(Align.CENTER);
				     mCanvas.drawText(clearance_height_open_Str, centerX ,centerY + 5, mPaint);
			      } else {
			    	  mCanvas.drawLine(centerX, centerY - delta ,centerX, centerY + delta, mPaint); 
			      }
			 }  // lifting
		   
	   }
   }
   
   private void handleAnchorage() {
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("anchorage");
	   if (aSymbol !=  null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			  /* mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(bitmapWidth ,bitmapHeight);*/
			   float symbolWidth = aSymbol.getWidth();
			   float symbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, symbolWidth,symbolHeight);
			   float aFactor = mBitmapWidth / symbolWidth;
		       //aPathShape.resize(symbolWidth / aFactor ,symbolHeight / aFactor);
		       aPathShape.resize(mBitmapWidth,mBitmapHeight);
		       mPaint.setStrokeWidth(2);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.MAGENTA);
		       aPathShape.draw(mCanvas, mPaint);
		   } 
	   }
   }
   
   private void handleMooring() {
	   String mooringCategory = mSeamarkNode.getValueToKey("seamark:mooring:category");
	   if (mooringCategory != null){
		   if (mooringCategory.equals("dolphin")){
			   int base = 4;
			   mBitmapWidth = base*4;
			   mBitmapHeight = base*4;
			   mSeamarkBitmap = Bitmap.createBitmap(mBitmapWidth,mBitmapHeight,mBitmapConfig);
			   mCanvas = new Canvas(mSeamarkBitmap); 
			   mPaint.setStrokeWidth(2);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.YELLOW);
		       mCanvas.drawCircle(mBitmapWidth/2, mBitmapHeight /2, 5, mPaint);
		   }
	   }
   }
 
   
   private void handleChimney() {
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("chimneypart1");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   } 
	   }
	   aSymbol = SeamarkSymbol.getSeamarkSymbol("chimneypart2");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   } 
	   } 
   }
   private void handleLandMark ( ){
	   String seamarkStr = mSeamarkNode.getValueToKey("seamark");
	   if ((seamarkStr != null) && (seamarkStr.equals("lighthouse"))){
		  handleLightHouse();
		  return;
	   }
	   if ((seamarkStr != null) && (seamarkStr.equals("landmark"))){
			  handleLightHouse();
			  return;
		   }
	  
	   String landmarkCategory = mSeamarkNode.getValueToKey("seamark:landmark:category");
	   if ((landmarkCategory != null) && landmarkCategory.equals("chimney")){
		  handleChimney();
		 return;
	   }
	   SeamarkSymbol aSymbol = null;
	   aSymbol = SeamarkSymbol.getSeamarkSymbol("light_minor");  // default
	   if ((landmarkCategory != null) && landmarkCategory.equals("windmotor")){
		   aSymbol = SeamarkSymbol.getSeamarkSymbol("windmotor");
	   }
	   if ((landmarkCategory != null) && landmarkCategory.equals("mast")){
		   aSymbol = SeamarkSymbol.getSeamarkSymbol("mast");
	   }
	   
	   if ((landmarkCategory != null) && landmarkCategory.equals("tower")){
		   aSymbol = SeamarkSymbol.getSeamarkSymbol("land_tower");
	   }
	   
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   //path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(4);
		       //mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   } 
	   }
	   
	   
	   String lightColour =  mSeamarkNode.getValueToKey("seamark:light:colour");
	   if (lightColour != null){
		   SeamarkSymbol aLightSymbol = SeamarkSymbol.getSeamarkSymbol("light");
    	   if (aLightSymbol != null) {
    		   float aLightSymbolWidth = aLightSymbol.getWidth();
    		   float aLightSymbolHeight = aLightSymbol.getHeight();
	    	   Path aLightPath = aLightSymbol.getPath();
	    	   if (aLightPath!= null) {
	    		   aLightPath.setFillType(FillType.EVEN_ODD);
	    		   PathShape aPathShape = new PathShape(aLightPath,aLightSymbolWidth,aLightSymbolHeight);
	    	       aPathShape.resize(mCanvas.getWidth() ,mCanvas.getHeight());
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       setLightColour(lightColour);
	    	       
	    	       aPathShape.draw(mCanvas, mPaint);
	    	   }
    	   }  
	   }
	   
   }
   
   
   
   private void handleLightHouse(){
	   String lightColour =  mSeamarkNode.getValueToKey("seamark:light:colour");
		  
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("lighthouse");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
		   SeamarkSymbol aLightSymbol = SeamarkSymbol.getSeamarkSymbol("light");
    	   if (aLightSymbol != null) {
    		   float aLightSymbolWidth = aLightSymbol.getWidth();
    		   float aLightSymbolHeight = aLightSymbol.getHeight();
	    	   Path aLightPath = aLightSymbol.getPath();
	    	   if (aLightPath!= null) {
	    		   aLightPath.setFillType(FillType.EVEN_ODD);
	    		   PathShape aPathShape = new PathShape(aLightPath,aLightSymbolWidth,aLightSymbolHeight);
	    	       aPathShape.resize(mCanvas.getWidth() ,mCanvas.getHeight());
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       setLightColour(lightColour);
	    	       
	    	       aPathShape.draw(mCanvas, mPaint);
	    	   }
    	   }
	   }
   }
   
   private void handleLightMinor (){
		  
	   String lightColour =  mSeamarkNode.getValueToKey("seamark:light:colour");
	   if (lightColour== null) {
		   lightColour =  mSeamarkNode.getValueToKey("seamark:light:1:colour");
	   }
  
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("light_minor");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
		   SeamarkSymbol aLightSymbol = SeamarkSymbol.getSeamarkSymbol("light");
    	   if (aLightSymbol != null) {
    		   float aLightSymbolWidth = aSymbol.getWidth();
    		   float aLightSymbolHeight = aSymbol.getHeight();
	    	   Path aLightPath = aLightSymbol.getPath();
	    	   if (aLightPath!= null) {
	    		   aLightPath.setFillType(FillType.EVEN_ODD);
	    		   PathShape aPathShape = new PathShape(aLightPath,aLightSymbolWidth,aLightSymbolHeight);
	    	       aPathShape.resize(mCanvas.getWidth() ,mCanvas.getHeight());
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       setLightColour(lightColour);
	    	       
	    	       aPathShape.draw(mCanvas, mPaint);
	    	   }
    	   }
	   }
   
}
   
   private void handleLightMajor (){
		  
	   String lightColour =  mSeamarkNode.getValueToKey("seamark:light:colour");
  
	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("light_major");
	   if (aSymbol!= null) {
		   Path path = aSymbol.getPath();
		   if (path!= null) {
			   path.setFillType(FillType.EVEN_ODD);
			   mSymbolWidth = aSymbol.getWidth();
			   mSymbolHeight = aSymbol.getHeight();
			   PathShape aPathShape = new PathShape(path, mSymbolWidth,mSymbolHeight);
		       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
		       mPaint.setStrokeWidth(5);
		       mPaint.setStyle(Style.FILL);
		       mPaint.setColor(Color.BLACK);
		       aPathShape.draw(mCanvas, mPaint);
		   }
		   SeamarkSymbol aLightSymbol = SeamarkSymbol.getSeamarkSymbol("light");
    	   if (aLightSymbol != null) {
    		   float aLightSymbolWidth = aSymbol.getWidth();
    		   float aLightSymbolHeight = aSymbol.getHeight();
	    	   Path aLightPath = aLightSymbol.getPath();
	    	   if (aLightPath!= null) {
	    		   aLightPath.setFillType(FillType.EVEN_ODD);
	    		   PathShape aPathShape = new PathShape(aLightPath,aLightSymbolWidth,aLightSymbolHeight);
	    	       aPathShape.resize(mCanvas.getWidth() ,mCanvas.getHeight());
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       setLightColour(lightColour);
	    	       
	    	       aPathShape.draw(mCanvas, mPaint);
	    	   }
    	   }
	   }
   
}
   

   
   private void handleType_buoy_or_beacon(String type, String shapeName) {
	   
	   //drawCenterAndFrame(canvas);
	     
       if ((shapeName != null )) { 
    	   String aName = mSeamarkNode.getValueToKey("seamark:name");
    	   setTopBasisFromShape(shapeName);
    	   String aLightColour = mSeamarkNode.getValueToKey("seamark:light:colour");
    	   String aColour = mSeamarkNode.getValueToKey("seamark:"+type+":colour");
    	   //Log.d(TAG,"Seamark " + aName + "  symbol: " + shapeName + " colour "+ aColour );
    	  
    	   mPaint.setColor(Color.BLACK);
    	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol(shapeName);
    	   if (aSymbol != null) {
    		   String symbolName = aSymbol.getName();
    	   
	    	   Path path = aSymbol.getPath();
	    	   if (path!= null) {
	    		   path.setFillType(FillType.EVEN_ODD);
	    		   mSymbolWidth = aSymbol.getWidth();
	    		   mSymbolHeight = aSymbol.getHeight();
	    		   PathShape aPathShape = new PathShape(path, mSymbolWidth, mSymbolHeight);
	    	       aPathShape.resize(mBitmapWidth ,mBitmapHeight);
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       mPaint.setColor(Color.BLACK);
	    	       aPathShape.draw(mCanvas, mPaint);
	    	   }
	    	   
	    	   // display  inner body  
	    	   Matrix matrix = null;  // we do not turn the symbol
	    	   displayInnerBody(type,mCanvas,null,shapeName); 
	    	   //display name
	    	   //displayName(canvas);
	    	   // handle light
	    	   aLightColour = mSeamarkNode.getValueToKey("seamark:light:colour");;
	    	   if (aLightColour != null){
	    		   displayLight(mCanvas);
	    	   }
	    	   displayTopMark(mCanvas);
    	   }
	   }
   }
   
   private void displayName(Canvas canvas) {
	   if (mZoomLevel > 13) {
		   String nameStr =  mSeamarkNode.getValueToKey("seamark:name");
		   if(nameStr!=null){
			   mPaint.setColor(Color.BLACK);
			   boolean measureForwards = true;
			   int countChars = mPaint.breakText(nameStr, measureForwards, mBitmapWidth * 0.35f,null);
			   canvas.drawText(nameStr, mBitmapWidth *0.65f, mBitmapHeight *0.4f, mPaint);
		   }
	   }
   }
   
   private void displayLight(Canvas canvas) {
	   String aLightColour = mSeamarkNode.getValueToKey("seamark:light:colour");
	   if (aLightColour != null){
	   
    	   SeamarkSymbol aSymbol = SeamarkSymbol.getSeamarkSymbol("light");
    	   if (aSymbol != null) {
    		   float aLightSymbolWidth = aSymbol.getWidth();
    		   float aLightSymbolHeight = aSymbol.getHeight();
	    	   Path path = aSymbol.getPath();
	    	   if (path!= null) {
	    		   path.setFillType(FillType.EVEN_ODD);
	    		   PathShape aPathShape = new PathShape(path,aLightSymbolWidth,aLightSymbolHeight);
	    	       aPathShape.resize(canvas.getWidth() ,canvas.getHeight());
	    	       mPaint.setStrokeWidth(5);
	    	       mPaint.setStyle(Style.FILL);
	    	       setLightColour(aLightColour);
	    	       
	    	       aPathShape.draw(canvas, mPaint);
	    	   }
    	   }
	   }
   }
   
  
   
   private void displayInnerBody(String type , Canvas canvas, Matrix matrix, String shapeName) {
	   String aColour = mSeamarkNode.getValueToKey("seamark:"+type+":colour");
	   String aPattern = mSeamarkNode.getValueToKey("seamark:"+type+":colour_pattern");
	   if (aPattern == null) {
		   SeamarkSymbol innerSeamarkSymbol = SeamarkSymbol.getSeamarkSymbol(shapeName+"1");
	       if (innerSeamarkSymbol != null ) {
	       	Path innerPath = innerSeamarkSymbol.getPath();
	           if (innerPath != null ){
	        	    if (matrix == null) {
				        innerPath.setFillType(FillType.EVEN_ODD);
				 	    PathShape innerPathShape = new PathShape(innerPath,mSymbolWidth,mSymbolHeight);
				        innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
				        mPaint.setStyle(Style.FILL);
				        setColour(aColour);
				        innerPathShape.draw(canvas, mPaint);
	        	    } else {
	        	       Path transformedPath = new Path();
	 	   		       innerPath.transform(matrix,transformedPath);
	 	   	           PathShape innerPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
	 	   	           innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
	 	   	           setColour(aColour);
	 	   	           mPaint.setStyle(Style.FILL);
	 	   	           innerPathShape.draw(canvas, mPaint);
	        	    }
	           }
	       }
	   } else {
		   // we have a pattern 
		   if (aPattern.equals("horizontal")){
			 String key = "seamark:"+type+":colour";
			 String colourStr =  mSeamarkNode.getValueToKey(key); 
			 String[] colours = null;
			 if (colourStr != null) colours = colourStr.split(";");
			 if (colours != null && colours.length > 0) {
			 /*String[] colours = colourStr.split(";"); // this caused a null ptr exc
			 if (colours.length > 0) {*/
				 for (int index = 0; index < colours.length;index++) {
					String nrOfElems = Integer.toString(colours.length);
					String selectStr = nrOfElems+"H";
					displayInnerBodySegmentHorizontal(canvas,shapeName+selectStr,matrix, index, colours[index]);
				 }
			 }
		   } 
		   if (aPattern.equals("vertical")){
			     String key = "seamark:"+type+":colour";
				 String colourStr =  mSeamarkNode.getValueToKey(key);
				 String[] colours = null;
				 if (colourStr != null ) {
					 colours = colourStr.split(";");
				 }
				 if (colours != null && colours.length > 0) {
					 for (int index = 0; index < colours.length;index++) {
						String nrOfElems = Integer.toString(colours.length);
						String selectStr = nrOfElems+"V";
						displayInnerBodySegmentVertical(canvas,shapeName+selectStr,matrix, index, colours[index]);
					 }
				 } 
		   }
	   }
   }
   
   private void displayInnerBodySegmentHorizontal(Canvas canvas, String shapeName,Matrix matrix, int index, String aColour) {
	   shapeName = shapeName + Integer.toString(index+1);
	   SeamarkSymbol innerSeamarkSymbol = SeamarkSymbol.getSeamarkSymbol(shapeName);
       if (innerSeamarkSymbol != null ) {
       	Path innerPath = innerSeamarkSymbol.getPath();
       	if (matrix == null) {
	        innerPath.setFillType(FillType.EVEN_ODD);
	 	    PathShape innerPathShape = new PathShape(innerPath,mSymbolWidth,mSymbolHeight);
	        innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
	        mPaint.setStyle(Style.FILL);
	        setColour(aColour);
	        innerPathShape.draw(canvas, mPaint);
	    } else {
	       Path transformedPath = new Path();
		       innerPath.transform(matrix,transformedPath);
	           PathShape innerPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
	           innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
	           setColour(aColour);
	           mPaint.setStyle(Style.FILL);
	           innerPathShape.draw(canvas, mPaint);
	    }
       }
   }
   
   private void displayInnerBodySegmentVertical(Canvas canvas, String shapeName,Matrix matrix, int index, String aColour) {
	   shapeName = shapeName + Integer.toString(index+1);
	   SeamarkSymbol innerSeamarkSymbol = SeamarkSymbol.getSeamarkSymbol(shapeName);
       if (innerSeamarkSymbol != null ) {
       	Path innerPath = innerSeamarkSymbol.getPath();
       	if (matrix == null) {
	        innerPath.setFillType(FillType.EVEN_ODD);
	 	    PathShape innerPathShape = new PathShape(innerPath,mSymbolWidth,mSymbolHeight);
	        innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
	        mPaint.setStyle(Style.FILL);
	        setColour(aColour);
	        innerPathShape.draw(canvas, mPaint);
	    } else {
	       Path transformedPath = new Path();
		       innerPath.transform(matrix,transformedPath);
	           PathShape innerPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
	           innerPathShape.resize(canvas.getWidth(),canvas.getHeight());
	           setColour(aColour);
	           mPaint.setStyle(Style.FILL);
	           innerPathShape.draw(canvas, mPaint);
	    }
       }
   }
   
   
   
   private void displayTopMark(Canvas canvas ) {
	   String aTopmarkShape = mSeamarkNode.getValueToKey("seamark:topmark:shape");
	   String aDaymarkShape =  mSeamarkNode.getValueToKey("seamark:daymark:shape");
	   boolean hasDaymarkShape = false;
	   if (aTopmarkShape== null && aDaymarkShape != null) { // sometime topmarks a coded as daymarks
		   hasDaymarkShape = true;
	       aTopmarkShape = aDaymarkShape;
	       }
	   if (aTopmarkShape != null)  {
    	   if (aTopmarkShape.equals("cylinder")) aTopmarkShape = "top_can";
    	   if (aTopmarkShape.equals("cone, point up")) aTopmarkShape ="top_cone_up";
    	   if (aTopmarkShape.equals("cone, point down")) aTopmarkShape ="top_cone_down";
    	   if (aTopmarkShape.equals("triangle, point up")) aTopmarkShape ="top_cone_up";
    	   if (aTopmarkShape.equals("triangle, point down")) aTopmarkShape ="top_cone_down";
    	   if (aTopmarkShape.equals("sphere")) aTopmarkShape ="top_sphere";
    	   if (aTopmarkShape.equals("2 cones point together")) aTopmarkShape ="top_west";
    	   if (aTopmarkShape.equals("2 cones base together")) aTopmarkShape ="top_east";
    	   if (aTopmarkShape.equals("2 cones down")) aTopmarkShape ="top_south";
    	   if (aTopmarkShape.equals("2_cones_up")) aTopmarkShape ="top_north";
    	   if (aTopmarkShape.equals("2 cones up")) aTopmarkShape ="top_north";
    	   if (aTopmarkShape.equals("x-shape")) {
    		   aTopmarkShape = "top_saltire";
    		   mTopBasisX -= 8;
    	   }
    	   if (aTopmarkShape.equals("2 spheres")) {
    		   aTopmarkShape = "top_isol";   
    	   }
    	                                          
    	   SeamarkSymbol topCanSymbol = SeamarkSymbol.getSeamarkSymbol(aTopmarkShape);
    	   if (topCanSymbol!= null) {
    		   float topCanCenterX = topCanSymbol.getWidth() / 2f;
   	           float topCanCenterY = topCanSymbol.getHeight() / 2f;
	    	   //String aTopColour = mSeamarkNode.getValueToKey("seamark:topmark:colour");
	    	   Matrix topCanMatrix = new Matrix();
   	           topCanMatrix.setRotate(mTopRotate,topCanCenterX,topCanCenterY);
   		       topCanMatrix.postTranslate(mTopBasisX,mTopBasisY);
	    	   Path path = topCanSymbol.getPath();
	    	   if (path!= null) {
	    		   path.setFillType(FillType.EVEN_ODD);
	    		   //float aTopSymbolWidth = topCanSymbol.getWidth();
	    		   //float TopSymbolHeight = topCanSymbol.getHeight();
	    		   Path topCanPath = topCanSymbol.getPath();
	   	           
	   	           Path transformedPath = new Path();
	   		       topCanPath.transform(topCanMatrix,transformedPath);
	   	           PathShape topCanPathShape = new PathShape(transformedPath,mSymbolWidth,mSymbolHeight);
	   	           topCanPathShape.resize(canvas.getWidth(),canvas.getHeight());
	   	           mPaint.setColor(Color.BLACK);
	   	           mPaint.setStyle(Style.FILL);
	   	           topCanPathShape.draw(canvas, mPaint);
	    		  
	    	   }
	    	// fill the  topmark, maybe a daymark
	    	   if (hasDaymarkShape) {
	    		   displayInnerBody("daymark",canvas,topCanMatrix,aTopmarkShape); 
	    	   }else {
	    		   displayInnerBody("topmark",canvas,topCanMatrix,aTopmarkShape);  
	    	   }
		        
    	   } // topcan symbol != null
    	
	   } // top mark shape != null
	  
   }
   
  
   public Drawable getDrawable (){    
		 return mDrawable;  
	   }
   
   public Bitmap getBitmap () {
	   return mSeamarkBitmap;
   }
   
  public void destroy() {
	  mSeamarkBitmap.recycle();
  }
}
