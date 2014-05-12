package org.openseamap.openseamapviewer.overlay;

import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;

import android.graphics.drawable.Drawable;

public class SeamarkOverlayItem {
	private SeamarkNode mSeamarkNode;
	private GeoPoint mGeoPoint;
	private Drawable mMarker;
	 public SeamarkOverlayItem (GeoPoint point, 
				Drawable marker , SeamarkNode seamarkNode) {
			mGeoPoint = point;
			mMarker = marker;
			mSeamarkNode = seamarkNode;
	 }
	 
	 public Drawable getMarker() {
		 return mMarker;
	 }
	 
	 public GeoPoint getPoint() {
		 return mGeoPoint;
	 }
	 
	 public void destroyMarker() {
		 this.mMarker = null;
	 }
}
