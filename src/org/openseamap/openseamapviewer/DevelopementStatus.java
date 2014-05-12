package org.openseamap.openseamapviewer;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.BoundingBox;

import android.graphics.Canvas;
import android.graphics.Point;

public class DevelopementStatus {
   // 13_01_11
	/*
	 * Version 4.03 für Test auf 4.03 Devices
	 * 
	 * Probleme mit dem Menu
	 * <uses-sdk
     *   android:minSdkVersion="8"
     *  android:targetSdkVersion="10" />
     *  sdk muss <= 10 sein , sonst wird der Compatibilitätsmodus mit dem Menubutton nicht gesetzt
     *  Siehe dazu Action Bar, der funktioniert hier nicht , warum?
     *  
     *  Seamarks werden über Pois dargestellt, Speicherprobleme bei grossen Karten und vielen Seamarks ( > 1000)
     *  bisher nur wenige Karten verfügbar, eigenes map-tagging für seamark-ways
     *  Peillinien werden über den eigenes RendererTheme erzeugt,  siehe assets
     *  
     *  Alle Seamarks liegen in einem seamarks file mit gleichem Namen wie die .map
     *  
     *  13_01_12
     *  
     *  Neues Overlay Seamarks
     *  
     *  Es wird direkt gezeichnet. ohne Pois
     *  
     *  build 005
     *  Fehler:
	 *  Verschiedene Lights werden nicht gezeichnet (Schiermonikoog, Borkum)
     *  Das sind Kombination von Rundum mit Sektorfeuern
     *   Zuordung zu den Listen andern:
     *   Siehe neu: preparePaintNumberedLightsIfNecessary() und preparePaintSingleLightsIfNecessary()
     *   und drawSectorLights
     *   
	 *   Build 006 13_01_17
	 *    Fehler koorigiert
	 *    Terschelling und Vlieland tauchen erst bei zoom 11 auf, koorigiert
	 *    
	 *    Schwerer Fehler in SemarkItemizedOverlay.drawSectorFires  und SeamarksOverlay.drawNumberedFires 
     *    unter bestimmten Bedinungen wurde die While-Schleife nicht terminiert, deshalb der Thread nie beendet
     *    wurde, auch nicht nach Ende der activity!!!
     *    app musste in den Einstellungen gestoppt werden
     *    damit spielte der GC verrückt und blockierte alles. 
     *    Build 007 13_01_17
     *    Fehler korrigiert
     *    
     *    OpenSeamapTileAndSeamarksDownloader überarbeitet
     *    getTileFromCache writeTileToCache,  assertDataDirectoryExists
     *    cache  mCacheDirPathStr =  stdDir +"/BackupCachedata/OpenSeaMap";
     *    weitere Unterverteilung durch zoomLevel
     *    
     *    13_01_19
     *    Version 0.51 build 008 
     *    OpenseamapDowloadViewer um Infofeld und centerOverlay ergänzt
     *    
     *    Eniro Nautical und Eniro Aerial hinzugefügt
     *    Centermap Overlay auf allen online Karten
     *    
     *    
     *    13_01_22 build 009
     *    
     *    Viewer mit pois
     *    
     *    Ist  der poi ein hafen seamark:type =habour und ist der hafen im Skipperguide so wird die URL angezeigt
     *    Der Browser kann direkt mit dieser URL gestartet werden
     *    
     *    13_01_23 build 010
     *    
     *    OpenSeaMapViewer with pois , OpenSeamapViewer :
     *    
     *    Routen die mit dem Törnplaner als gml-Files erstellt worden sind werden angezeigt.
     *    
     *    Menu settings aus Main Activity entfernt
     *    
     *    13_01_25 build 011
     *    Verschiedene notices eingebaut (Hinweis Dirk test@grade.de
     *    A: no_entry, closed_area, no_motor_craft, no_sport_craft, no_sailing_craft no_unpowered craft
     *    B: speed_limit, make radio_contact
     *    C: limited_headroom
     *    E: weir, mooring_permitted, radio_information
     *    
     *    Getestet mi Isselmoond kampen , no_sailing_craft ist verschwunden
     *    
     *    13_02_02 build 012
     *    
     *    Underlay bing Aerial eingbaut
     *    
     *    Eniro Nautical und Eniro Aerial versteckt ,
     *    
     *    in den shared prefs wird das letzte Kartenzentrum verwaltet
     *    
     *    13_02_05 Version 0.51 build 013
     *    
     *    Eniro Nautical und Eniro Aerial sichtbar,
     *    
     *    Routen laden mit gpx oder gml (Törnplaner) in Eniro Nautical und Aerial ,
     *    OpenSeamap online
     *    Openseamap mit Bing  ,
     *    Openseamap offline, openseamap mit pois
     *    
     *    MainActivity verwaltet Version und Build in prefs und kopiert notwendige Änderungen
     *    ( symbol
     *    
     *    FEhler auf der KölnWesel-Karte
     *    
     *    in SeamarkDrawable.handleNotice
     *    semark:notice:1:category wird nicht erkannt , null pointer exception 
     *    erst mal abgeschaltet
     *    
     *    13_02_19 Version 0.51 build 014
     *    
     *    App crashed wenn die SD-Karte nicht verfügbar war, in onPause , die gemerkte Karte war dann nicht verfügbar,  repariert
     *    Splitscreen für gleichzeitige Darstellung in derschieden Variationen
     *    
     *    weisses Leitfeuer wird jetzt als ausgefüllter Sektor dargestellt, ab Zoom 14
     *    
     *    13_02_21 
     *    
     *    Fehler in drawSectorFires
     *    
     *    manchmal wird sectorEnd oder sectorStart als v="shore" angegeben, das wurde als 0 ausgewertet
     *    korrigiert in SemarksOverlay,SeamarksItemizedOverlay, SplitSeamarksWithAerialOverlay, SplitSeamarksItemezedOverlay, SEamarksWithAerialOverlay
     *    
     *    13_03_18
     *    
     *    In seammarkOsm werden jetzt auch navigation_lines erfasst. Dann wäre eine Kartenerzeugung mit navigation_lines im mapping Prozess 
     *    überflüssig
     *    NavigationLine ist eine neue Klasse, die einen Way mit dem Tag navigation_line aus xxx_seamarks.xml enthält
     *    mit getNavigationLinesAsArrayList() wird die Liste der NavigationsLine's geliefert
     *    
     *    1. Versuch einer Auswertung in SeamarksOverlay und OpenSeamapviewer
     *    SeamarksOverlay.drawNavLines(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel)
     *    OpenseamapViewer.updateNavLinesOnOverlay(BoundingBox boundingBox)
     *    
     *    Die Routinen werden in OpenseamapViewer.updateSeamarkNodesOnOverlay() angestoßen
     *    
     *    bisher: die navigationLines werden blau gestrichelt gezeichnet
     *    
     *    neuer Build 051 B 16
     *    diese Version führt zu einem Absturz  beim ersten Start des OpSeamapViewers in onPause()
     *    da noch kein mapfile eingetragen ist führt ist mapView.getMapFile == null und 
     *    zu einem gründlichen Absturz
     *    korrigiert
     *    Fehler auch in OpenSeamapWithAerialOverlayViewer , korrigiert
     *    war schon abgefangen in SplitOpenSeaMapWithAerialOverlayAndPoisViewer
     *    war schon abgefangen in OpenSeaMapViewerWithPois
     *    war schon abgefangen in SplitOpenSeaMapWithAerialOverlayViewer
     *    kein Fehler in SplitScreenViewer, da map nicht benutzt
     *    
     *    Fehler im OpenSeamapViewerWithPOIS, bzw SeamarkItemizedOverlay in onTap()
     *    
     *    Die netzwerkabfrage nach der Url des hafen im harbourInfo führt ab SDK 8 ( Abhängig vom Betriebssystem auf dem Device)
     *    zu einem Absturz
     *    Die Ausführung von Netzwerkaktivitäten im UIThread ist ab 3.0 verboten und führt zu derm Fehler
     *    
     *    korrigiert, Aufgabe wird durch eine Asyc-Task erledigt, Kommentierung in onTap()
     *    todo:
     *    
     *    weitere tags aus opmsemaprenderer001
     *    Siehe Karte _NL_Terschelling_Emden_seamarks.xml
     *    <tag k="waterway" v="drawbridge"/>
     *    
     *    <tag k="seamark:type" v="bridge"/>
     *    <tag k="seamark:bridge:category" v="opening"/>
	 *	  <tag k="seamark:bridge:clearance_height_closed" v="0.98"/>
	 *    <tag k="seamark:bridge:category" v="fixed"/>
     *    <tag k="seamark:bridge:clearance_height_closed" v="0.95"/>
     *    
     *    bridge ist erledigt
     *    
     *    13_03_19 neuer Build 051 B17
     *    
     *    OpenSeaMapViewer: Menu Zeige GPS-Position:
     *    Fehler: Die GPS-Position wird nicht angezeigt
     *    Es fehlte eine Routine zum Zeichnen der GPS-Position in SeamarksOverlay
     *    Ergänzt: 
     *       SeamarksOverlay.showMyUpdatedPosition(....)
     *       und Aufruf in  SeamarksOverlay.drawOverlayBitmap(..) 
     *     Fehler korrigiert
     *     
     *    13_04_02 neuer Build 051 B18 Release Version: "public" 
     *    
     *    13_04_04 
     *    
     *    GPS Position wird in DownloadOpenSeaMapTileAndSeamarksViwer nicht abgezeigt
     *      neuer Location Listener MyLocationListenerWithDownload
     *    GPS-Funktion für alle Activities in public relase vorhanden
     *    
     *    Die Einbindung des GPS muss auf neu gemacht werden, noch gibt es für jede Activity einen eigenen LocationListener
     *    neuer Build 051 B19 Release Version "public"
     *    
     *    
     *    13_04_07
     *    
     *    Fehler in der Anzeige von Brücken, die eine unbekannte Category haben
     *    Siehe dazu nordhollandse Kanal Burgervlotbrug , da gibt es ein Tag "Pontonbrug"
     *    
     *    Korrigiert, jetzt wird ein ? angezeigt
     *    
     *    neuer Build erforderlich
     *    
     *    13_04_11 
     *    
     *    Fehler bei den Topzeichen der Kardinaltonnen, mur ein Teil wurden ausgefüllt
     *    korrigiert, jeweils den Eintrag im symbol-File geändert
     *    Symbol file verändert,
     *    Build auf 22 gesetzt
     *    neuer Build V51 B22
     *    
     *    13_04_14
     *    
     *    Änderung der package-Struktur,
     *    Auswirkungen:
     *    1. Verschiedene Methoden und Konstruktoren die ohne public definiert waren,
     *    sind jetzt nicht mehr sichtbar. Abhilfe: als public deklarieren, fixed
     *    2. Im Manifest stehen die Pfade durch die packages auf die Activities, anpassen !!!, fixed
     *    
     *    13_04_17
     *    
     *    OpenSeaMapViewerWithInfoPois zeigt die NavLines nicht!!
     *    
     *    Beim Ausprobieren von Teifenlinien ist aufgefallen, dass die nav-lines allgemeine seamark-ways sind.
     *    Die nav-lines sind aber getagged, das wird nicht aufgenommen.
     *    Es braucht eine Struktur für seamarkWays, dabei müssen die refs auf die nodes und die tags verwaltet werden!!!
     *    
     *    Testweise baue ich erst mal die Tiefenpunkte im Overlay ein z.B.
     *    <node id="-5164" lat="43.8346829" lon="15.5290091" version="1" timestamp="2013-04-17T10:33:55Z" changeset="0">
		      <tag k="seamark:sounding" v="3.8"/>
		      <tag k="seamark:sounding:quality" v="reported_unconfirmed"/>
		      <tag k="seamark:type" v="sounding"/>
	</node>
	*       OpenSeaMapViewerWithInfoPois es fehlt die Angabe der Namen für die Tonnen und der dazugehörige 
	*       Menupunkt
	*       
	*       
	*       13_04_25 
	*       Navigation_line durch Seamark_way ersetzt, jetzt werden auch die Tags zum Way (navigation_line. depth_contour ) verwaltet
	*       Fehlender Menupunkt zur Anzeige der Seezeichennamen ergänzt
	*       OpenSeamapViewer ist nicht mehr sichtbar, es wird OpenSeaMapViewer with infoPois in der Main Activity gezeigt
    *       neuer Build V051 B24 (public)
    *       
    *       
    *       
    *       13_04_27  Problem mit navigation_line , siehe SeamarkItemizedOverlayWithInfoPois.drawSeamarkWay , Zeile 503
    *       
    *       if (navLineOrientationStr != null){
	*					// there may be a Problem see example v="180Â°18&#39;" from Wismarbucht4 way id="139699229" 
	*					// or v="006Â°24&#39;"  from way id="139691959" 
	*				}
	*				
	*	    Wie soll so was geparsed werden?
    *       
    *     13_10_13
    *     
    *     river kilometer angaben eingefügt
    *     neuer build als public 
    *     
    *     13_11_10 Einrichtung git repository 
    *     
    *     2013_11_10 Fehler online base-map nicht mehr verfügbar
    *     2013_11_16 base-map Server auf osm2.wtnwt.de geändert
     *    
     *    
     *    
     *    2014_01_12  semarks-files auch als .dat files
     *    
     *   geändert in allen Viewern .setSeamarkFilePathAndRead
     *   neue mapsforge bibliothek 030_1 eingebaiút , siehe  AISPlotter vermeidet auf dem Nexus den Crash 
     *   
     *   2014_01_14
     *   Version 055 (Versionsprung wegen google play
     *   im infotext Anzahl ways und pois sowie seamarkupdates entfernt
     *   
     *   2014_01_29
     *   
     *   Der Verweis auf harbour in SeamarkItemizedOverlayWithInfoPois.getHarbourInfoWithAsyncTask funktioneiert wieder
     *   Fehler bei der Erzeugung der URL und Änderungen im Get von Mario Avalone
     *   siehe Java Projekt Harbour_test_2014_02
     *   Environment2.getCardinfo() sollte jetzt auch bei Lifetab von Medion funktionieren
     *   Version 057 public im Playstore 
     *   
     *    todo:
     *    LocationListener abstrahieren
     *    Viewer abstrahieren
     *    Overlays abstrahieren
     *    Downloader abstrahieren
     *    
     *    Es muss überall geprüft werden ob die GPS-Funktion implementiert ist
     *    Sie fehlt in DownloadOpenSeaMapTileAnsSeamarksViewer
     *    
     *    <tag k="seamark:type" v="cable_submarine"/>
     *    <tag k="seamark:type" v="separation_zone"/>
     *    <tag k="seamark:type" v="separation_lane"/>
     *    <tag k="seamark:type" v="separation_boundary"/>
     *    
     *    <tag k="seamark:type" v="pipeline_submarine"/>
     *    <tag k="seamark:type" v="cable_submarine"/>


     *    
     *    <tag k="seamark:type" v="restricted_area"/>
     *    <tag k="seamark:type" v="anchorage"/>
     *    




	 */
	
	
	
}
