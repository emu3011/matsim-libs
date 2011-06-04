/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalDepartureHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.multimodalsimengine;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.mobsim.framework.PlanDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

public class MultiModalDepartureHandler implements DepartureHandler {

	private QSim qSim;
	private MultiModalSimEngine simEngine;
	private Set<String> handledModes = new HashSet<String>();
	
	public MultiModalDepartureHandler(QSim qSim, MultiModalSimEngine simEngine, MultiModalConfigGroup multiModalConfigGroup) {
		this.qSim = qSim;
		this.simEngine = simEngine;
		
		String simulatedModes = multiModalConfigGroup.getSimulatedModes();
		if (simulatedModes.contains("walk")) handledModes.add(TransportMode.walk);
		if (simulatedModes.contains("bike")) handledModes.add(TransportMode.bike);
		if (simulatedModes.contains("ride")) handledModes.add(TransportMode.ride);
		if (simulatedModes.contains("pt")) handledModes.add(TransportMode.pt);
	}
	
	@Override
	public boolean handleDeparture(double now, PlanAgent personAgent, Id linkId, Leg leg) {

		if (handledModes.contains(leg.getMode())) {
			if (personAgent instanceof PlanDriverAgent) {
				handleMultiModalDeparture(now, (PlanDriverAgent)personAgent, linkId, leg);
				return true;
			} else {
				throw new UnsupportedOperationException("PersonAgent is not from type PersonDriverAgent - cannot handle departure. Found PersonAgent class is " + personAgent.getClass().toString());
			}
		}
		
		return false;
	}
	
	private void handleMultiModalDeparture(double now, PlanDriverAgent personAgent, Id linkId, Leg leg) {
		
		Route route = leg.getRoute();
		MultiModalQLinkExtension extension = simEngine.getMultiModalQLinkExtension(qSim.getNetsimNetwork().getNetsimLink(linkId));
		
		if ((route.getEndLinkId().equals(linkId)) && (personAgent.chooseNextLinkId() == null)) {
			personAgent.endLegAndAssumeControl(now);
		} else {
			extension.addDepartingAgent(personAgent, now);
		}				
	}
}	
