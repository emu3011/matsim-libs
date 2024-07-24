/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.sim;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.BenensonParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.MemoryBasedParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.NearestParkingSpotAgentLogic;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSAgentLogic;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSLibrary;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSSearchLogic;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSUtils;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.ParkingGuidanceSystem;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.router.RoutingModule;

import com.google.inject.name.Named;

/**
 * @author jbischoff, extended by Ricardo Ewert
 *
 */

public class ParkingAgentFactory implements AgentFactory {

	/**
	 *
	 */
	@Inject
	@Named(value = TransportMode.walk) RoutingModule walkRouter;
	@Inject
	ParkingSearchManager parkingManager;

	@Inject
	EventsManager events;
	@Inject
	ParkingRouter parkingRouter;
	@Inject
	Network network;
	@Inject
	VehicleTeleportationLogic teleportationLogic;
	@Inject // by Emanuel Skodinis (emanuesk@ethz.ch): inject the qsim instead of putting it in the constructor
	QSim qsim;
	@Inject
	Config config;
	@Inject
	ParkingGuidanceSystem PGS; // by Emanuel Skodinis (emanuesk@ethz.ch): inject the parking guidance system

	/**
	 *
	 */
	@Inject
	public ParkingAgentFactory() {
		// by Emanuel Skodinis (emanuesk@ethz.ch): note that qsim is not passed in the constructor anymore
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		ParkingSearchConfigGroup psConfigGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		ParkingSearchLogic parkingLogic;
		ParkingAgentLogic agentLogic = null;

		switch (psConfigGroup.getParkingSearchStrategy()) {
			// following case by Emanuel Skodinis (emanuesk@ethz.ch): add support for the parking strategy ParkingGuidance 
			case ParkingGuidance -> {
				// for parking guidance, every person has a parking search strategy
				String parkingSearchStrategy = PGSUtils.getAttribute(PGSLibrary.parkingSearchStrategy, p);

				// depending on the parking search strategy, create parking logic and agent logic
				switch (parkingSearchStrategy) {
					case PGSLibrary.PGS -> {
						parkingLogic = new PGSSearchLogic(network);
						agentLogic = new PGSAgentLogic(p.getSelectedPlan(),
												   	   parkingManager,
												   	   walkRouter,
												   	   network,
												   	   parkingRouter,
													   events,
													   parkingLogic,
													   ((QSim) qsim).getSimTimer(),
													   teleportationLogic,
													   psConfigGroup,
													   PGS);
					}
					case PGSLibrary.random -> {
						parkingLogic = new RandomParkingSearchLogic(network);
						agentLogic = new ParkingAgentLogic(p.getSelectedPlan(),
														   parkingManager,
														   walkRouter,
														   network,
														   parkingRouter,
														   events,
														   parkingLogic,
														   ((QSim) qsim).getSimTimer(),
														   teleportationLogic,
														   psConfigGroup);
					}
					case PGSLibrary.benenson -> {
						parkingLogic = new BenensonParkingSearchLogic(network, psConfigGroup);
						agentLogic = new BenensonParkingAgentLogic(p.getSelectedPlan(),
																   parkingManager,
																   walkRouter,
																   network,
																   parkingRouter,
																   events,
																   parkingLogic,
																   ((QSim) qsim).getSimTimer(),
																   teleportationLogic,
																   psConfigGroup);
					}
					case PGSLibrary.distanceMemory -> {
						parkingLogic = new DistanceMemoryParkingSearchLogic(network);
						agentLogic = new MemoryBasedParkingAgentLogic(p.getSelectedPlan(),
																	  parkingManager,
																	  walkRouter,
																	  network,
																	  parkingRouter,
																	  events,
																	  parkingLogic,
																	  ((QSim) qsim).getSimTimer(),
																	  teleportationLogic,
																	  psConfigGroup);
					}
					case PGSLibrary.nearestParkingSpot -> {
						int numberOfAgents = qsim.getScenario()
												 .getPopulation()
												 .getPersons()
												 .size();
						int currentAgentNumber = qsim.getAgents().size() + 1;
						int numberReserved = (int) Math.round(psConfigGroup.getFractionCanReserveParkingInAdvanced() * numberOfAgents);
						int numberCapacityCheck = (int) Math.round(psConfigGroup.getFractionCanCheckFreeCapacitiesInAdvanced() * numberOfAgents);

						if (currentAgentNumber == 1){
							LogManager.getLogger(getClass()).info("Number of agents, who can reserve a parking slot in advanced: " + numberReserved);
							LogManager.getLogger(getClass()).info("Number of agents, who can check a free parking slot in advanced: " + numberCapacityCheck);
							LogManager.getLogger(getClass()).info("Number of agents, who have no technical support to find a parking slot: " + (numberOfAgents - numberReserved -numberCapacityCheck));

						}
						if (numberReserved >= currentAgentNumber) {
							parkingLogic = new NearestParkingSpotSearchLogic(network,
																			 parkingRouter,
																			 parkingManager,
																			 true,
																			 true);
						} else if (numberCapacityCheck + numberReserved >= currentAgentNumber){
							parkingLogic = new NearestParkingSpotSearchLogic(network,
																			 parkingRouter,
																			 parkingManager,
																			 false,
																			 true);
						}
						else
							parkingLogic = new NearestParkingSpotSearchLogic(network,
																			 parkingRouter,
																			 parkingManager,
																			 false,
																			 false);
						agentLogic = new NearestParkingSpotAgentLogic(p.getSelectedPlan(),
																	  parkingManager,
																	  walkRouter,
																	  network,
																	  parkingRouter,
																	  events,
																	  parkingLogic,
																	  ((QSim) qsim).getSimTimer(),
																	  teleportationLogic,
																	  psConfigGroup);
					}
				}
			}
            case Benenson -> {
                parkingLogic = new BenensonParkingSearchLogic(network, psConfigGroup);
				agentLogic = new BenensonParkingAgentLogic(p.getSelectedPlan(), parkingManager, walkRouter, network,
                        parkingRouter, events, parkingLogic, ((QSim) qsim).getSimTimer(), teleportationLogic, psConfigGroup);
            }
            case Random -> {
                parkingLogic = new RandomParkingSearchLogic(network);
				agentLogic = new ParkingAgentLogic(p.getSelectedPlan(), parkingManager, walkRouter, network,
                        parkingRouter, events, parkingLogic, ((QSim) qsim).getSimTimer(), teleportationLogic, psConfigGroup);
            }
            case DistanceMemory -> {
                parkingLogic = new DistanceMemoryParkingSearchLogic(network);
				agentLogic = new MemoryBasedParkingAgentLogic(p.getSelectedPlan(), parkingManager, walkRouter, network,
                        parkingRouter, events, parkingLogic, ((QSim) qsim).getSimTimer(), teleportationLogic, psConfigGroup);
            }
			case NearestParkingSpot -> {
				int numberOfAgents = qsim.getScenario().getPopulation().getPersons().size();
				int currentAgentNumber = qsim.getAgents().size() + 1;
				int numberReserved = (int)Math.round(psConfigGroup.getFractionCanReserveParkingInAdvanced() * numberOfAgents);
				int numberCapacityCheck = (int)Math.round(psConfigGroup.getFractionCanCheckFreeCapacitiesInAdvanced() * numberOfAgents);

				if (currentAgentNumber == 1){
					LogManager.getLogger(getClass()).info("Number of agents, who can reserve a parking slot in advanced: " + numberReserved);
					LogManager.getLogger(getClass()).info("Number of agents, who can check a free parking slot in advanced: " + numberCapacityCheck);
					LogManager.getLogger(getClass()).info("Number of agents, who have no technical support to find a parking slot: " + (numberOfAgents - numberReserved -numberCapacityCheck));

				}
				if (numberReserved >= currentAgentNumber) {
					parkingLogic = new NearestParkingSpotSearchLogic(network, parkingRouter, parkingManager, true, true);
				} else if (numberCapacityCheck + numberReserved >= currentAgentNumber){
					parkingLogic = new NearestParkingSpotSearchLogic(network, parkingRouter, parkingManager, false, true);
				}
				else
					parkingLogic = new NearestParkingSpotSearchLogic(network, parkingRouter, parkingManager, false, false);
				agentLogic = new NearestParkingSpotAgentLogic(p.getSelectedPlan(), parkingManager, walkRouter,
					network, parkingRouter, events, parkingLogic, ((QSim) qsim).getSimTimer(),
					teleportationLogic, psConfigGroup);
			}
        };

        Id<Link> startLinkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		if (startLinkId == null) {
			throw new NullPointerException(" No start link found. Should not happen.");
		}
		DynAgent agent = new DynAgent(p.getId(), startLinkId, events, agentLogic);
		return agent;
	}

}
