/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSimplifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.network.algorithms.intersectionSimplifier;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.intersectionSimplifier.containers.Cluster;
import org.matsim.core.network.algorithms.intersectionSimplifier.containers.ClusterActivity;
import org.matsim.core.utils.collections.QuadTree;

/**
 * Class to simplify a given network's intersections.
 * 
 * @author jwjoubert
 */
public class IntersectionSimplifier {
	final private static Logger LOG = Logger.getLogger(IntersectionSimplifier.class);
	final private double pmin;
	final private int epsilon;

	/* TODO I may want to look into rewriting the (clustering) code so that it
	 * accepts a list of Nodes to cluster, and not just its (unidentifiable)
	 * Coords. */
	private DensityCluster djc = null;
	private QuadTree<Coord> clusteredNodes = null;
	private QuadTree<Node> clusteredCentroids = null;


	public IntersectionSimplifier(double pmin, int epsilon) {
		this.pmin = pmin;
		this.epsilon = epsilon;
	}

	public Network simplify(Network network, String clusterFile) {
		/* TODO The clusterFile argument can eventually be removed. */

		if(this.djc != null) {
			LOG.error("This NetworkSimplifier has already been used to simplify a network!");
			throw new RuntimeException("Should instantiate a new NetworkSimplifier");
		}

		LOG.info("Simplifying the intersections...");
		reportNetworkStatistics(network);
		Network newNetwork = NetworkUtils.createNetwork();

		/* Get all the network's node coordinates that must be clustered. */
		List<Coord> nodes = new ArrayList<Coord>();
		for(Node node : network.getNodes().values()) {
			nodes.add(node.getCoord());
		}

		/* Set up the clustering infrastructure. */
		LOG.info("Clustering the network nodes...");
		this.djc = new DensityCluster(nodes, true);
		djc.clusterInput(pmin, epsilon);
		djc.writeClustersToFile(clusterFile);
		LOG.info("Done clustering.");

		/* Do the mapping of clustered points. */
		List<Cluster> clusters = djc.getClusterList();

		/* Populate a QuadTree with all the clustered nodes, each with a 
		 * reference to the cluster they belong to. */
		LOG.info("Populating QuadTree with clustered points.");
		this.clusteredNodes = new QuadTree<>(
				djc.getClusteredPoints().getMinEasting(), 
				djc.getClusteredPoints().getMinNorthing(), 
				djc.getClusteredPoints().getMaxEasting(), 
				djc.getClusteredPoints().getMaxNorthing());
		this.clusteredCentroids = new QuadTree<>(
				djc.getClusteredPoints().getMinEasting(), 
				djc.getClusteredPoints().getMinNorthing(), 
				djc.getClusteredPoints().getMaxEasting(), 
				djc.getClusteredPoints().getMaxNorthing());
		for(Cluster cluster : clusters) {
			Id<Node> newId = Id.createNodeId("simplified_" + cluster.getId().toString());
			Node newNode = network.getFactory().createNode(newId, cluster.getCenterOfGravity());
			for(ClusterActivity clusterPoint : cluster.getPoints()) {
				Coord clusterPointCoord = clusterPoint.getCoord();
				clusteredNodes.put(clusterPointCoord.getX(), clusterPointCoord.getY(), clusterPointCoord);
				clusteredCentroids.put(clusterPointCoord.getX(), clusterPointCoord.getY(), newNode);
			}
		}
		LOG.info("Done populating QuadTree. Number of nodes affected: " + clusteredNodes.size());

		/* Go through each network link, in given network, and evaluate it's nodes. */
		for(Link link : network.getLinks().values()) {
			
 			Node fromNode = NetworkUtils.createNode(link.getFromNode().getId(), link.getFromNode().getCoord());
			Node fromCentroid = getClusteredNode(fromNode);

			Node toNode = NetworkUtils.createNode(link.getToNode().getId(), link.getToNode().getCoord());
			Node toCentroid = getClusteredNode(toNode);

			Node newFromNode = fromCentroid != null ? fromCentroid : fromNode;
			Node newToNode = toCentroid != null ? toCentroid : toNode;
			
			/* FIXME currently the new link carries no additional information 
			 * from the original network. */
			Link newLink = NetworkUtils.createLink(
					link.getId(), newFromNode, newToNode, newNetwork, 
					link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			
			if(newLink.getFromNode().getCoord().equals(newLink.getToNode().getCoord())) {
				/* If both link nodes are part of the same cluster, their node
				 * Coords will now be the same. The link can be completely ignored, 
				 * so we need to process it here any further. */
			} else {
				
				/* FIXME Remove after debugging. */
				if(newFromNode.getId().toString().equalsIgnoreCase("2") || newToNode.getId().toString().equalsIgnoreCase("2")) {
					LOG.info("Got node \"2\"");
				}
				
				
				
				if(!newNetwork.getNodes().containsKey(newLink.getFromNode().getId())) {
					/* FIXME currently the new node carries no additional 
					 * information from the original network. */
					NetworkUtils.createAndAddNode(newNetwork, newLink.getFromNode().getId(), newLink.getFromNode().getCoord());
				}
				if(!newNetwork.getNodes().containsKey(newLink.getToNode().getId())) {
					NetworkUtils.createAndAddNode(newNetwork, newLink.getToNode().getId(), newLink.getToNode().getCoord());
				}
				newLink.setFromNode(newNetwork.getNodes().get(newFromNode.getId()));
				newLink.setToNode(newNetwork.getNodes().get(newToNode.getId()));
				newNetwork.addLink(newLink);
			}
		}

		/* Update the network name. */
		String oldName = network.getName();
		if(oldName != null) {
			oldName += oldName.endsWith(".") ? " Simplified." : ". Simplified.";
			newNetwork.setName(oldName);
		} else {
			LOG.warn("The given network does not have a description. This makes reproducibility hard!");
		}

		LOG.info("Done simplifying the intersections");
		reportNetworkStatistics(newNetwork);
		return newNetwork;
	}
	

	protected Node getClusteredNode(Node node) {
		Node n = null;
		if(isClustered(node)) {
			n = clusteredCentroids.getClosest(node.getCoord().getX(), node.getCoord().getY());
		}

		return n;
	}


	protected boolean isClustered(Node node) {
		boolean result = false;
		if(this.clusteredNodes == null) {
			LOG.warn("No network simplification has been done yet. So NO nodes are clustered.");
		} else {
			Coord closestCoord = clusteredNodes.getClosest(node.getCoord().getX(), node.getCoord().getY());
			if(closestCoord.equals(node.getCoord())){
				result = true;
			}
		}

		return result;
	}

	public List<Cluster> getClusters() {
		if(this.djc == null) {
			LOG.warn("The network has not been simplified yet. Returning 0 clusters");
			return new ArrayList<Cluster>(0);
		}
		return this.djc.getClusterList();
	}

	public static void reportNetworkStatistics(Network network) {
		LOG.info("--- Network statistics: ------------------------------------------------------");
		LOG.info("   Network description: " + network.getName());
		LOG.info("       Number of nodes: " + network.getNodes().size());
		LOG.info("       Number of links: " + network.getLinks().size());
		LOG.info("------------------------------------------------------------------------------");
	}


}
