package org.openseamap.openseamapviewer.locationlistener;

import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.viewer.SplitOpenSeaMapWithAerialOverlayViewer;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MySplitLocationListernerWithAerialOverlay implements LocationListener{
	private final SplitOpenSeaMapWithAerialOverlayViewer splitOpenseamapviewer;
	private boolean centerAtFirstFix;

	public MySplitLocationListernerWithAerialOverlay(SplitOpenSeaMapWithAerialOverlayViewer openseamapMapViewer) {
		this.splitOpenseamapviewer = openseamapMapViewer;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (!this.splitOpenseamapviewer.isShowMyLocationEnabled()) {
			return;
		}
		GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
		
		
		this.splitOpenseamapviewer.requestOverlayRedraw(newPoint);
		

		if (this.centerAtFirstFix || this.splitOpenseamapviewer.isSnapToLocationEnabled()) {
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
