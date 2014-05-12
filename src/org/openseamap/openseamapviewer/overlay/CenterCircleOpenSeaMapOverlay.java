package org.openseamap.openseamapviewer.overlay;

import java.util.ArrayList;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.viewer.DownloadOpenSeamapTileAndSeamarksViewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;

public class CenterCircleOpenSeaMapOverlay extends Overlay {
	private boolean test = false;
	private static final String TAG ="CenterCircleOverlay" ;
	private boolean mMustShowCenter = true;
	private Paint mPaint = null;
	private RectF mOval;
	private DownloadOpenSeamapTileAndSeamarksViewer mContext;
	
	
	public ArrayList<GeoPoint> mRouteList = null;
	public boolean mShowRoute = false;
	
	public boolean mShowLocation = false;
	
	public CenterCircleOpenSeaMapOverlay(DownloadOpenSeamapTileAndSeamarksViewer context){
		super();
		mContext = context;
		mMustShowCenter = false;
	}
	
	public void setRouteList(ArrayList<GeoPoint> routePointList){
		mRouteList = routePointList;
	}
	
	public void setShowRoute(boolean show){
		mShowRoute = show;
	}
	
	public void setShowLocation(boolean show) {
		mShowLocation = show;
	}
	
	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			Projection projection, byte drawZoomLevel) {
		this.mPaint = new Paint();
		this.mPaint.setStyle(Paint.Style.STROKE);
		this.mPaint.setStrokeWidth(4);
		this.mPaint.setColor(Color.RED);
		if (test) Log.d(TAG,"drawOverlayBitmap begin");
		
		if (mMustShowCenter) {
			int aWidth = canvas.getWidth();
			int aHeight = canvas.getHeight();
			Point aP = new Point(aWidth / 2, aHeight / 2);
			// projection.toPoint(drawPosition, aPixelPoint, drawZoomLevel);
			this.mOval = new RectF(aP.x - 5, aP.y - 5, aP.x + 5, aP.y + 5);
			canvas.drawOval(this.mOval, this.mPaint);
		}
		if (mShowRoute){
			drawRoute(canvas,drawPosition,projection,drawZoomLevel);
		}
		if (mShowLocation){
			drawLocation(canvas,drawPosition,projection,drawZoomLevel);
		}

	}
	public boolean getMustShowCenter(){
		return mMustShowCenter;
	}
	
	public void setMustShowCenter(boolean pMustShow) {
		mMustShowCenter = pMustShow;
		
	}
	private void drawLocation(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if ( mContext.mMyLocationPoint != null && mContext.showMyLocation){
			 Paint paint = new Paint();
			 paint.setStrokeWidth(5);
			 paint.setColor(Color.BLUE);
			 Point aPixelPoint = new Point();
			 GeoPoint myPoint = mContext.mMyLocationPoint;
			 projection.toPoint(myPoint, aPixelPoint, drawZoomLevel);
			 float x = aPixelPoint.x;
		     x = x - drawPosition.x;
		     float y = aPixelPoint.y;
		     y = y - drawPosition.y;
			 canvas.drawCircle(x, y, 10, paint);
			 if (mContext.mMyLocationChanged) {
				 mContext.mMyLocationChanged = false;
				 // we handled it, see OpenSeaMapViewer.requestOverlayRedraw
			 }
		 }
	}
	
	private void drawRoute (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    paint.setStrokeWidth(2);
		  paint.setColor(Color.RED);
		if (this.mRouteList != null) {
		  int count = this.mRouteList.size();
		   if (count > 1 && this.mShowRoute){
			   GeoPoint prevPoint = this.mRouteList.get(0);
			   Point aPrevPixelPoint = new Point();
			   projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
			   for (int index = 1;index < count; index ++){
				 
				  GeoPoint nextPoint = this.mRouteList.get(index) ;
				  Point nextPixelPoint = new Point();
				  projection.toPoint(nextPoint, nextPixelPoint, drawZoomLevel);
				 
				  aPrevPixelPoint.x = aPrevPixelPoint.x - drawPosition.x;
				  aPrevPixelPoint.y = aPrevPixelPoint.y - drawPosition.y;
				  nextPixelPoint.x = nextPixelPoint.x - drawPosition.x;
				  nextPixelPoint.y = nextPixelPoint.y - drawPosition.y;
				  canvas.drawLine(aPrevPixelPoint.x,aPrevPixelPoint.y, nextPixelPoint.x,nextPixelPoint.y, paint);
				  prevPoint = nextPoint;
				  projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
				  
			   }
		   }
		}
	}
}
