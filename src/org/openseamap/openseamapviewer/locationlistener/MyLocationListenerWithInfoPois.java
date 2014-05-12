/*
 * Copyright 2012 V.Klein
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openseamap.openseamapviewer.locationlistener;

import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapViewerWithInfoPois;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * @author vkADM
 *
 */
public class MyLocationListenerWithInfoPois implements LocationListener {
	private final OpenSeaMapViewerWithInfoPois openseamapviewer;
	private boolean centerAtFirstFix;

	public MyLocationListenerWithInfoPois(OpenSeaMapViewerWithInfoPois advancedMapViewer) {
		this.openseamapviewer = advancedMapViewer;
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

