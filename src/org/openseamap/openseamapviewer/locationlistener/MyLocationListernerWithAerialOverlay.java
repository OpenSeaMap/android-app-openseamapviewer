package org.openseamap.openseamapviewer.locationlistener;

import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapWithAerialOverlayViewer;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MyLocationListernerWithAerialOverlay implements LocationListener{
	private final OpenSeaMapWithAerialOverlayViewer openseamapviewer;
	private boolean centerAtFirstFix;

	public MyLocationListernerWithAerialOverlay(OpenSeaMapWithAerialOverlayViewer openseamapMapViewer) {
		this.openseamapviewer = openseamapMapViewer;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (!this.openseamapviewer.isShowMyLocationEnabled()) {
			return;
		}
		GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
		
		
		this.openseamapviewer.requestOverlayRedraw(newPoint);
		

		if (this.centerAtFirstFix || this.openseamapviewer.isSnapToLocationEnabled()) {
			this.centerAtFirstFix = false;
			//this.openseamapviewer.mapController.setCenter(newPoint);

		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// do nothing
	}

	@Override
	public void onProviderEnabled(String provider) {
		// do nothing
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// do nothing
	}

	public boolean isCenterAtFirstFix() {
		return this.centerAtFirstFix;
	}

	public void setCenterAtFirstFix(boolean centerAtFirstFix) {
		this.centerAtFirstFix = centerAtFirstFix;
	}
}
