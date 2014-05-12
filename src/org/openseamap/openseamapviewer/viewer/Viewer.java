package org.openseamap.openseamapviewer.viewer;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;

public abstract class Viewer extends Activity {
	public abstract boolean isShowMyLocationEnabled() ;
	public abstract boolean isSnapToLocationEnabled();
	public abstract void requestOverlayRedraw(GeoPoint newPoint);
}
