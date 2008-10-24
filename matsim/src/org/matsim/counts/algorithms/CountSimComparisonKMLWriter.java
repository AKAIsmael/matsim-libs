/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonKMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.counts.algorithms;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.TimeSpanType;
import net.opengis.kml._2.UnitsEnumType;
import net.opengis.kml._2.Vec2Type;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.matsim.basic.v01.Id;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;
import org.matsim.counts.algorithms.graphs.BoxPlotErrorGraph;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24Graph;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraph;
import org.matsim.gbl.MatsimResource;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.utils.vis.matsimkml.NetworkFeatureFactory;

/**
 * @author dgrether
 */
public class CountSimComparisonKMLWriter extends CountSimComparisonWriter {
	/**
	 * constant for the name of the links
	 */
	private static final String LINK = "Link: ";
	/**
	 * constant for the link description
	 */
	private static final String COUNTVALUE = "Count Value: ";
	/**
	 * constant for the link description
	 */
	private static final String MATSIMVALUE = "MATSim Value: ";
	/**
	 * constant for the link description
	 */
	private static final String RELERROR = "Relative Error: ";
	/**
	 * constant for the link description
	 */
	private static final String IMG = "<img src=\"./";
	/**
	 * constant for the link description
	 */
	private static final String IMGEND = "\">";
	/**
	 * constant for the link description
	 */
	private static final String H24OVERVIEW = "24 h overview";
	/**
	 * constant for the link description
	 */
	private static final String DETAILSFROM = "Details from ";
	/**
	 * constant for the link description
	 */
	private static final String OCLOCKTO = " o'clock to ";
	/**
	 * constant for the link description
	 */
	private static final String OCLOCK = " o'clock";
	/**
	 * constant for the link description
	 */
	private static final String ZERO = "0";
	/**
	 * the icons
	 */
	private static final String CROSSICON = "icons/plus.png";
	/**
	 * the icons
	 */
	private static final String MINUSICON = "icons/minus.png";
	/**
	 * the scale for the icons
	 */
	private static final double ICONSCALE = 0.5;
	/**
	 * height of the charts
	 */
	private static final int CHARTHEIGHT = 300;
	/**
	 * width of the  charts
	 */
	private static final int CHARTWIDTH = 400;
	/**
	 * constant for the file suffix of graphs
	 */
	private static final String PNG = ".png";
	/**
	 * constant for the file name of the CountsSimRealPerHourGraphs
	 */
	private static final String SIMREALGRAPHNAME = "countsSimRealPerHour_";
	/**
	 * the network
	 */
	private final NetworkLayer network;
	/**
	 * the srs transformation used
	 */
	private CoordinateTransformation coordTransform = null;
	/**
	 * The object factory for KML types.
	 */
	private ObjectFactory kmlObjectFactory = null;
	/**
	 * main kml, doc and folder
	 */
	private KmlType mainKml = null;
	/**
	 * the kml main document
	 */
	private DocumentType mainDoc = null;
	/**
	 * The kml main folder
	 */
	private FolderType mainFolder = null;
	/**
	 * the kmz writer
	 */
	private KMZWriter writer = null;
	/**
	 * style for one of the icons
	 */
	private StyleType redCrossStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType redMinusStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType yellowCrossStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType yellowMinusStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType greenMinusStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType greenCrossStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType greyCrossStyle;
	/**
	 * style for one of the icons
	 */
	private StyleType greyMinusStyle;
	/**
	 * maps linkids to filenames in the kmz
	 */
	private Map<String, String> countsLoadCurveGraphMap;

	/** The logging object for this class. */
	private static final Logger log = Logger.getLogger(CountSimComparisonKMLWriter.class);

	/**
	 * Sets the data to the fields of this class
	 * @param countSimCompList
	 * @param network
	 * @param coordTransform
	 */
	public CountSimComparisonKMLWriter(final List<CountSimComparison> countSimCompList, final NetworkLayer network, final CoordinateTransformation coordTransform) {
		super(countSimCompList);
		this.network = network;
		this.coordTransform = coordTransform;
		this.kmlObjectFactory = new ObjectFactory();
	}

	/**
	 * This method initializes the styles for the different icons used.
	 */
	private void createStyles() {

		this.redCrossStyle = kmlObjectFactory.createStyleType(); this.redCrossStyle.setId("redCrossStyle");
		this.redMinusStyle = kmlObjectFactory.createStyleType(); this.redMinusStyle.setId("redMinusStyle");
		this.yellowCrossStyle = kmlObjectFactory.createStyleType(); this.yellowCrossStyle.setId("yellowCrossStyle");
		this.yellowMinusStyle = kmlObjectFactory.createStyleType(); this.yellowMinusStyle.setId("yellowMinusStyle");
		this.greenCrossStyle = kmlObjectFactory.createStyleType(); this.greenCrossStyle.setId("greenCrossStyle");
		this.greenMinusStyle = kmlObjectFactory.createStyleType(); this.greenMinusStyle.setId("greenMinusStyle");
		this.greyCrossStyle = kmlObjectFactory.createStyleType(); this.greyCrossStyle.setId("greyCrossStyle");
		this.greyMinusStyle = kmlObjectFactory.createStyleType(); this.greyMinusStyle.setId("greyMinusStyle");

		Byte[] red = new Byte[]{(byte) 0xFF, (byte) 0x0F, (byte) 0x0F, (byte) 0xBE};
		Byte[] green = new Byte[]{(byte) 0xFF, (byte) 0x14, (byte) 0xDC, (byte) 0x0A};
		Byte[] yellow = new Byte[]{(byte) 0xFF, (byte) 0x14, (byte) 0xE6, (byte) 0xE6};
		Byte[] grey = new Byte[]{(byte) 0xFF, (byte) 0x42, (byte) 0x42, (byte) 0x42};

		HashMap<StyleType, Byte[]> colors = new HashMap<StyleType, Byte[]>();
		colors.put(this.redCrossStyle, red);
		colors.put(this.redMinusStyle, red);
		colors.put(this.yellowCrossStyle, yellow);
		colors.put(this.yellowMinusStyle, yellow);
		colors.put(this.greenCrossStyle, green);
		colors.put(this.greenMinusStyle, green);
		colors.put(this.greyCrossStyle, grey);
		colors.put(this.greyMinusStyle, grey);

		HashMap<StyleType, String> hrefs = new HashMap<StyleType, String>();
		hrefs.put(this.redCrossStyle, CROSSICON);
		hrefs.put(this.redMinusStyle, MINUSICON);
		hrefs.put(this.yellowCrossStyle, CROSSICON);
		hrefs.put(this.yellowMinusStyle, MINUSICON);
		hrefs.put(this.greenCrossStyle, CROSSICON);
		hrefs.put(this.greenMinusStyle, MINUSICON);
		hrefs.put(this.greyCrossStyle, CROSSICON);
		hrefs.put(this.greyMinusStyle, MINUSICON);

		for (StyleType styleType : new StyleType[]{
				this.redCrossStyle, this.redMinusStyle, this.yellowCrossStyle, this.yellowMinusStyle, 
				this.greenCrossStyle, this.greenMinusStyle, this.greyCrossStyle, this.greyMinusStyle}) {

			IconStyleType icon = kmlObjectFactory.createIconStyleType();
			icon.setColor(
					new byte[]{
							colors.get(styleType)[0],
							colors.get(styleType)[1],
							colors.get(styleType)[2],
							colors.get(styleType)[3]}); 
			icon.setScale(ICONSCALE);

			LinkType link = kmlObjectFactory.createLinkType();
			link.setHref(hrefs.get(styleType));
			icon.setIcon(link);

			styleType.setIconStyle(icon);

			this.mainDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(styleType));

		}

	}

	/**
	 * Writes the data to the file at the path given as String
	 *
	 * @param filename
	 */
	public void writeFile(final String filename) {

		// init kml
		this.mainKml = kmlObjectFactory.createKmlType();
		this.mainDoc = kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));

		// create the styles and the folders
		createStyles();
		// create a folder
		this.mainFolder = kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Comparison, Iteration " + this.iterationNumber);
		this.mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(this.mainFolder));
		// the writer
		this.writer = new KMZWriter(filename);

		try {
			//try to create the legend
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(createLegend()));
		} catch (IOException e) {
			log.error("Cannot add legend to the KMZ file.", e);
		}
		try {
			//add the matsim logo to the kml
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(MatsimKMLLogo.writeMatsimKMLLogo(writer)));
		} catch (IOException e) {
			log.error("Cannot add logo to the KMZ file.", e);
		}

		try {
			// copy required icons to the kmz
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/plus.png"), CROSSICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/minus.png"), MINUSICON);
		} catch (IOException e) {
			log.error("Could not copy copy plus-/minus-icons to the KMZ.", e);
		}

		// prepare folders for simRealPerHour-Graphs (top-left, xy-plots)
		FolderType simRealFolder = kmlObjectFactory.createFolderType();
		simRealFolder.setName("XY Comparison Plots");
		this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(simRealFolder));

//		// error graphs and awtv graph
		ScreenOverlayType errorGraph = createBiasErrorGraph();
		errorGraph.setVisibility(true);
		this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(errorGraph));
		errorGraph = createBoxPlotErrorGraph();
		if (errorGraph != null) {
			errorGraph.setVisibility(false);
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(errorGraph));
		}
		ScreenOverlayType awtv=this.createAWTVGraph();
		if (awtv != null) {
			awtv.setVisibility(false);
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(awtv));
		}

		// link graphs
		this.createCountsLoadCurveGraphs();

		// hourly data...
		for (int h = 1; h < 25; h++) {
			// the timespan for this hour
			TimeSpanType timespan = kmlObjectFactory.createTimeSpanType();
			timespan.setBegin("1999-01-01T" + Time.writeTime(((h - 1) * 3600)));
			timespan.setEnd("1999-01-01T" + Time.writeTime((h * 3600)));

			// first add the xyplot ("SimRealPerHourGraph") as overlay
			this.addCountsSimRealPerHourGraphs(simRealFolder, h, timespan);

			// add the placemarks for the links in this hour
			FolderType subfolder = kmlObjectFactory.createFolderType();
			subfolder.setName(createFolderName(h));
			subfolder.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timespan));
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(subfolder));

			writeLinkData(this.countComparisonFilter.getCountsForHour(Integer.valueOf(h)), subfolder);
		}
		finish();
	}

	/**
	 * Creates the string for the foldername
	 * @param timestep
	 * @return a timestep specific standard string
	 */
	private String createFolderName(final int timestep) {
		StringBuffer buffer = new StringBuffer(30);
		buffer.append("Traffic from ");
		buffer.append(this.timestepToString(timestep - 1));
		buffer.append(" to ");
		buffer.append(this.timestepToString(timestep));
		buffer.append(" o'clock");
		return buffer.toString();
	}

	/**
	 * Creates a legend
	 * @return a ScreenOverlay read from a file
	 * @throws IOException
	 */
	private ScreenOverlayType createLegend() throws IOException {

		this.writer.addNonKMLFile(MatsimResource.getAsInputStream("countsKml/countsLegend240x300.png"), "countsLegend.png");
		ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
		LinkType icon = kmlObjectFactory.createLinkType();
		icon.setHref("./countsLegend.png");
		overlay.setIcon(icon);
		overlay.setName("Legend");
		// place the image bottom left
		Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0d);
		overlayXY.setY(0.0d);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02d);
		screenXY.setY(0.07d);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		return overlay;

	}
	/**
	 * Creates a placemark
	 *
	 * @param linkid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @return the Placemark instance with description and name set
	 */
	private PlacemarkType createPlacemark(final String linkid, final CountSimComparison csc, final double relativeError, final int timestep) {
		StringBuffer stringBuffer = new StringBuffer();
		PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
		stringBuffer.delete(0, stringBuffer.length());
		stringBuffer.append(LINK);
		stringBuffer.append(linkid);
		placemark.setDescription(createPlacemarkDescription(linkid, csc, relativeError, timestep));
		return placemark;
	}

	/**
	 * This method writes all the data for each of the links/counts to the kml
	 * document.
	 *
	 * @param countSimComparisonList provides "the data"
	 * @param folder The folder to which to add the data in the kml-file.
	 */
	private void writeLinkData(final List<CountSimComparison> countSimComparisonList, final FolderType folder) {
		Id linkid;
		Link link;
		PlacemarkType placemark;
		double relativeError;
		Coord coord;
		PointType point;
		for (CountSimComparison csc : countSimComparisonList) {
			linkid = csc.getId();
			link = this.network.getLink(linkid);

			coord = this.coordTransform.transform(calculatePlacemarkPosition(link));
			relativeError = csc.calculateRelativeError();
			// build placemark
			placemark = createPlacemark(linkid.toString(), csc, relativeError, csc.getHour());
			point = kmlObjectFactory.createPointType();
			point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
//			point.getCoordinates().add(Double.toString(coord.getX()));
//			point.getCoordinates().add(Double.toString(coord.getY()));
//			point.getCoordinates().add(Double.toString(0.0));
			placemark.setAbstractGeometryGroup(kmlObjectFactory.createPoint(point));
//			point = new Point(coord.getX(), coord.getY(), 0.0);
//			placemark.setGeometry(point);
			// cross
			if (csc.getSimulationValue() > csc.getCountValue()) {
				if (csc.getSimulationValue() < csc.getCountValue() * 1.5) {
					placemark.setStyleUrl(this.greenCrossStyle.getId());
				}
				else if (csc.getSimulationValue() < csc.getCountValue() * 2) {
					placemark.setStyleUrl(this.yellowCrossStyle.getId());
				}
				else {
					placemark.setStyleUrl(this.redCrossStyle.getId());
				}
			}
			// minus
			else {
				if (csc.getSimulationValue() > csc.getCountValue() * 0.75) {
					placemark.setStyleUrl("#greenMinusStyle");
				} else if (csc.getSimulationValue() > csc.getCountValue() * 0.5) {
					placemark.setStyleUrl("#yellowMinusStyle");
				} else {
					placemark.setStyleUrl("#redMinusStyle");
				}
			}
			folder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(placemark));
		}
	}
	/**
	 * Calculates the position of a placemark in a way that it is 40 % of the link
	 * length away from the node where the link starts.
	 *
	 * @param l
	 * @return the CoordI instance
	 */
	private Coord calculatePlacemarkPosition(final Link l) {
		Coord coordFrom = l.getFromNode().getCoord();
		Coord coordTo = l.getToNode().getCoord();
		double xDiff = coordTo.getX() - coordFrom.getX();
		double yDiff = coordTo.getY() - coordFrom.getY();
		double length = Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
		double scale = 0.4;
		scale = l.getLength() * scale;
		Coord vec = new CoordImpl(coordFrom.getX() + (xDiff * scale/length), coordFrom.getY() + (yDiff * scale/length));
		return vec;
	}
	/**
	 *
	 * @param linkid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @return A String containing the description for each placemark
	 */
	private String createPlacemarkDescription(final String linkid, final CountSimComparison csc, final double relativeError, final int timestep) {
		StringBuffer buffer = new StringBuffer(100);
		buffer.append(NetworkFeatureFactory.STARTCDATA);
//		buffer.append(STARTH1);
//		buffer.append(LINK);
//		buffer.append(linkid);
//		buffer.append(ENDH1);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append(LINK);
		buffer.append(linkid);
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(H24OVERVIEW);
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(IMG);
		buffer.append(this.countsLoadCurveGraphMap.get(linkid));
		buffer.append(IMGEND);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(DETAILSFROM);
		buffer.append((this.timestepToString(timestep -1)));
		buffer.append(OCLOCKTO);
		buffer.append(this.timestepToString(timestep));
		buffer.append(OCLOCK);
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(COUNTVALUE);
		buffer.append(csc.getCountValue());
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(MATSIMVALUE);
		buffer.append(csc.getSimulationValue());
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(RELERROR);
		buffer.append(relativeError);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.ENDCDATA);
		return buffer.toString();
	}

	/**
	 * @param timestep
	 * @return A two digits string containing the given timestep
	 */
	private String timestepToString(final int timestep) {
		if (timestep < 10) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(ZERO);
			buffer.append(Integer.toString(timestep));
			return buffer.toString();
		}
		return Integer.toString(timestep);
	}

	/**
	 * Creates CountsSimRealPerHourGraphs and adds them to the kmz in the given folder. The creation of the graphs is
	 * only done if the map attribute of this class for CountsSimRealPerHourGraphs is null.
	 *
	 * @param folder
	 * @param timestep
	 * @param timespan
	 */
	private void addCountsSimRealPerHourGraphs(final FolderType folder, final int timestep, final TimeSpanType timespan) {
		StringBuffer filename;
		ScreenOverlayType overlay;

		try {
			//add the file to the kmz
			filename = new StringBuffer(SIMREALGRAPHNAME);
			filename.append(Integer.toString(timestep));
			filename.append(PNG);

			CountsSimRealPerHourGraph graph = new CountsSimRealPerHourGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, filename.toString());
			graph.createChart(timestep);

			this.writeChartToKmz(filename.toString(), graph.getChart());
			//and link with the overlay
			overlay = kmlObjectFactory.createScreenOverlayType();
			LinkType icon = kmlObjectFactory.createLinkType();
			icon.setHref("./" + filename.toString());
			overlay.setIcon(icon);
			overlay.setName(graph.getChartTitle());
			// place the image top left
			Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
			overlayXY.setX(0.0d);
			overlayXY.setY(1.0d);
			overlayXY.setXunits(UnitsEnumType.FRACTION);
			overlayXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setOverlayXY(overlayXY);
			Vec2Type screenXY = kmlObjectFactory.createVec2Type();
			screenXY.setX(0.02d);
			screenXY.setY(0.98d);
			screenXY.setXunits(UnitsEnumType.FRACTION);
			screenXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setScreenXY(screenXY);
			overlay.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timespan));
			//add the overlay to the folder
			folder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(overlay));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates CountsLoadCurveGraphs for each link and puts them in the kmz as pngs
	 */
	private void createCountsLoadCurveGraphs() {
		CountsLoadCurveGraphCreator cgc = new CountsLoadCurveGraphCreator("");
		List<CountsGraph> graphs= cgc.createGraphs(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber);

		this.countsLoadCurveGraphMap = new HashMap<String, String>(graphs.size());
		String linkid;
		StringBuffer filename;
		for (CountsGraph cg : graphs) {
			try {
				filename = new StringBuffer();
				linkid = ((CountsLoadCurveGraph) cg).getLinkId();
				filename.append(linkid);
				filename.append(PNG);
				writeChartToKmz(filename.toString(), cg.getChart());
				this.countsLoadCurveGraphMap.put(linkid, filename.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes the given JFreeChart to the kmz file specified for the kmz writer attribute of this class.
	 * @param filename the filename to use in the kmz
	 * @param chart
	 * @throws IOException
	 */
	private void writeChartToKmz(final String filename, final JFreeChart chart) throws IOException {
		byte [] img;
		img = ChartUtilities.encodeAsPNG(chart.createBufferedImage(CHARTWIDTH, CHARTHEIGHT));
		this.writer.addNonKMLFile(img, filename);
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBoxPlotErrorGraph() {

		CountsGraph ep;
		try {
			ep = new BoxPlotErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
			ep.createChart(0);
		} catch (IllegalArgumentException e) {
			log.error("Could not create BoxPlot-ErrorGraph.", e);
			return null;
		}

		String filename = "errorGraphBoxPlot.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
			LinkType icon1 = kmlObjectFactory.createLinkType();
			icon1.setHref("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("Error Graph [Box-Plot]");
			// place the image bottom right
			Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
			overlayXY.setX(1.0d);
			overlayXY.setY(0.0d);
			overlayXY.setXunits(UnitsEnumType.FRACTION);
			overlayXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setOverlayXY(overlayXY);
			Vec2Type screenXY = kmlObjectFactory.createVec2Type();
			screenXY.setX(0.98d);
			screenXY.setY(0.1d);
			screenXY.setXunits(UnitsEnumType.FRACTION);
			screenXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setScreenXY(screenXY);

			return overlay;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBiasErrorGraph() {
		CountsGraph ep = new BiasErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
		ep.createChart(0);

		String filename = "errorGraphErrorBias.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
			LinkType icon1 = kmlObjectFactory.createLinkType();
			icon1.setHref("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("Error Graph [Error/Bias]");
			// place the image bottom right
			Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
			overlayXY.setX(1.0d);
			overlayXY.setY(0.0d);
			overlayXY.setXunits(UnitsEnumType.FRACTION);
			overlayXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setOverlayXY(overlayXY);
			Vec2Type screenXY = kmlObjectFactory.createVec2Type();
			screenXY.setX(0.98d);
			screenXY.setY(0.1d);
			screenXY.setXunits(UnitsEnumType.FRACTION);
			screenXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setScreenXY(screenXY);
			return overlay;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates the CountsSimReal24Graph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createAWTVGraph() {
		CountsGraph awtv = new CountsSimReal24Graph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, "awtv graph");
		awtv.createChart(0);

		String filename = "awtv.png";
		try {
			writeChartToKmz(filename, awtv.getChart());
			ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
			LinkType icon1 = kmlObjectFactory.createLinkType();
			icon1.setHref("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("AWTV");
			// place the image bottom right
			Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
			overlayXY.setX(1.0d);
			overlayXY.setY(0.0d);
			overlayXY.setXunits(UnitsEnumType.FRACTION);
			overlayXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setOverlayXY(overlayXY);
			Vec2Type screenXY = kmlObjectFactory.createVec2Type();
			screenXY.setX(0.98d);
			screenXY.setY(0.1d);
			screenXY.setXunits(UnitsEnumType.FRACTION);
			screenXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setScreenXY(screenXY);
			return overlay;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * to be called when the kml stream shall be closed.
	 *
	 */
	private void finish() {
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}

}
