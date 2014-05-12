package org.openseamap.openseamapviewer.viewer;

import org.mapsforge.core.GeoPoint;

public interface ViewerInterface {
	public boolean isShowMyLocationEnabled();
	public boolean isSnapToLocationEnabled();
	public void requestOverlayRedraw(GeoPoint newPoint);
}
