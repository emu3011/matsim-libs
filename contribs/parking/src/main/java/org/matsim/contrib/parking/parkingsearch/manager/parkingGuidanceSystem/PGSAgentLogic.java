package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;

import org.matsim.core.mobsim.framework.MobsimTimer;

import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.core.router.RoutingModule;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.api.core.v01.network.Network;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dynagent.DynAction;

/**
 * Agent logic for Parking Guidance System. Adapted from ParkingAgentLogic.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class PGSAgentLogic extends ParkingAgentLogic {

	private ParkingGuidanceSystem PGS;
    
	public PGSAgentLogic(Plan plan,
                         ParkingSearchManager parkingManager,
                         RoutingModule walkRouter,
                         Network network,
			             ParkingRouter parkingRouter,
                         EventsManager events,
                         ParkingSearchLogic parkingLogic,
                         MobsimTimer timer,
			             VehicleTeleportationLogic teleportationLogic,
                         ParkingSearchConfigGroup configGroup,
						 ParkingGuidanceSystem PGS) {
		super(plan,
			  parkingManager,
			  walkRouter,
			  network,
			  parkingRouter,
			  events,
			  parkingLogic,
			  timer,
			  teleportationLogic,
			  configGroup);

		this.PGS = PGS;
	}

	/**
	 * Defines what to do after the vehicle is unparked.
	 * A PGSDynLeg is created with the route from the parking to the destination.
	 */
	@Override
	protected DynAction nextStateAfterUnParkActivity(DynAction oldAction,
													 double now) {
		// we have unparked, now we need to get going by car again.

		// step 1: get the planned route
		Leg currentPlannedLeg = (Leg) this.currentPlanElement;
		Route plannedRoute = currentPlannedLeg.getRoute();
		
		// step 2: calculate actual route
		// QUESTION: should this route already be to the closest parking facility? No since we need the destination link.
		NetworkRoute actualRoute = this.parkingRouter.getRouteFromParkingToDestination(plannedRoute.getEndLinkId(), 
																					   now,
																					   this.agent.getCurrentLinkId());
		
		// QUESTION: why do we have to unpark the vehicle again if the method is called nextStateAfterUnParkActivity?
		if ((this.parkingManager.unParkVehicleHere(currentlyAssignedVehicleId,
												   agent.getCurrentLinkId(),
												   now)) 					  || isInitialLocation){
			// I dont understand this stuff...
			this.lastParkActionState = LastParkActionState.CARTRIP;
			isInitialLocation = false;
			Leg currentLeg = (Leg) this.currentPlanElement;
			// this could be Car, Carsharing, Motorcylce, or whatever else mode we have, so we want our leg to reflect this.

			// only difference to ParkingAgentLogic (other DynLeg)
			return new PGSDynLeg(currentLeg.getMode(),
								 actualRoute,
								 parkingLogic,
								 parkingManager,
								 currentlyAssignedVehicleId,
								 timer,
								 events,
								 PGS);
		}
		else throw new RuntimeException("parking location mismatch");
	}

}