/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculator.java
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

package org.matsim.core.router.util;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public interface LeastCostPathCalculator {

	Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, final Person person, final Vehicle vehicle);

	class Path {
		public List<Node> nodes;
		public final List<Link> links;
		public final double travelTime;
		public final double travelCost;

		public Path(final List<Node> nodes, final List<Link> links, final double travelTime, final double travelCost) {
			this.nodes = nodes;
			this.links = links;
			this.travelTime = travelTime;
			this.travelCost = travelCost;
		}

		public Node getFromNode() {
			return nodes.get(0);
		}

		public Node getToNode() {
			return nodes.get(nodes.size() - 1);
		}

		/**
		 * helper method to get end-link ID
		 * 
		 * written by Emanuel Skodinis (emanuesk@ethz.ch)
		 */
		public Id<Link> getEndLinkId() {
			return links.get(links.size() - 1).getId();
		}

		/**
		 * helper method to get link ID of given index (of path position)
		 * 
		 * written by Emanuel Skodinis (emanuesk@ethz.ch)
		 */
		public Id<Link> getLinkIdAtIdx(int idx) {
			return this.links.get(idx).getId();
		}

		/**
		 * helper method to append a link to a path
		 * throws an exception if the old path does not lead to the link to be appended
		 * 
		 * written by Emanuel Skodinis (emanuesk@ethz.ch)
		 */
		public void appendLink(Link link) throws Exception {
			if (this.getToNode() != link.getFromNode()) {
				throw new Exception("tried to append a link to a path that does not lead to that link");
			}
			this.nodes.add(link.getToNode());
        	this.links.add(link);
		}
	}
}
