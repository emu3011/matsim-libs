package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import org.matsim.contrib.parking.parkingsearch.DynAgent.ParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * DynLeg for Parking Guidance System (PGS). Adapted from ParkingDynLeg.
 * 
 * Uses the ParkingGuidanceSystem to get a path to the closest free parking.
 * 
 * If the parking, which is routed to, gets occupied the ParkingGuidanceSystem is used to reroute to the next 
 * closest free parking.
 * 
 * If the PGS can not provide guidance, the driver is behaving like a random searcher, i.e. drives to destination and starts 
 * a random search for a free parking spot.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class PGSDynLeg extends ParkingDynLeg {

    private final ParkingGuidanceSystem PGS;
	private Path path; // guidance path from PGS if guidanceMode is true and navigation otherwise
	private boolean guidanceMode;

	/**
	 * Constructor calculates the path to closest parking with help of the PGS.
	 * If PGS can not guide, calculate navigation to destination (and do random search later)
	 * 
	 * @param mode
	 * @param route
	 * @param logic
	 * @param parkingManager
	 * @param vehicleId
	 * @param timer
	 * @param events
	 * @param PGS
	 */
    public PGSDynLeg(String mode,
                     NetworkRoute route,
                     ParkingSearchLogic logic,
                     ParkingSearchManager parkingManager,
                     Id<Vehicle> vehicleId,
                     MobsimTimer timer,
                     EventsManager events,
                     ParkingGuidanceSystem PGS) {
        super(mode,
			  route,
			  logic,
			  parkingManager,
			  vehicleId,
			  timer,
			  events);

		double time = this.timer.getTimeOfDay();

        this.PGS = PGS;

		// try to get guidance
		this.guidanceMode = true;
		this.path = this.PGS.guide(this.route.getStartLinkId(),
								   this.route.getEndLinkId(),
								   time);

		// if PGS fails to guide, get navigation to destination (and just random search when arrived)
		if (this.path == null) {
			this.processFailedGuidanceEvent(time);
		}
    }

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.path.getEndLinkId();
	}

	@Override
	public void movedOverNode(final Id<Link> newLinkId) {
		// update index and link
		this.currentLinkIdx++;
		this.currentLinkId = newLinkId;

		/**
		 * 1. We need to check whether we reached the destination and have to go in parking mode.
		 * 2. We need to check whether we are passing free parking spots. This must exclude the starting link and the
		 * 	  destination link and only needs to be reported when we are in guidance mode.
		 */

		if (!this.parkingMode) { // if not in parking mode, check whether we reached the destination link
			double time = this.timer.getTimeOfDay();

			if (this.currentLinkId.equals(this.getDestinationLinkId())) { // destination reached
				// go into parking mode
				this.parkingMode = true;

				// report start of parking search event
				this.events.processEvent(new StartParkingSearchEvent(time,
																 	 this.vehicleId,
																	 this.currentLinkId));

				// try to park on current link
				this.hasFoundParking = this.parkingManager.reserveSpaceIfVehicleCanParkHere(this.vehicleId, this.currentLinkId);
			} else if (this.guidanceMode && this.parkingManager.isThereFreeParkingSpaceAt(this.currentLinkId)) { // check if we are passing a free parking space during guidance
				/**
				 * we are not in parking mode,
				 * we are in guidance mode,
				 * we are not on the starting link (since we just moved over a node), and
				 * we did not yet reach the destination link
				 * 
				 * we just passing a free parking space => report event
				 */ 
				this.events.processEvent(new PassingFreeParkingEvent(time, this.vehicleId));
			}
		} else { // if in parking mode, try to park at current link
			this.hasFoundParking = this.parkingManager.reserveSpaceIfVehicleCanParkHere(this.vehicleId, this.currentLinkId);
		}
	}

	@Override
	public Id<Link> getNextLinkId() {
		if (!this.parkingMode) {
			// if we are in guidance mode, we have to check for rerouting event
			if (this.guidanceMode) {
				// if the parking which is routed to gets occupied we have a rerouting event
				if (!this.parkingManager.isThereFreeParkingSpaceAt(this.getDestinationLinkId())) {
					this.processReroutingEvent(this.timer.getTimeOfDay());
				}
			}

			// since we are not in parking mode, we are simply following along the path (might be guidance or navigation)
			Id<Link> nextLinkId = this.path.getLinkIdAtIdx(currentLinkIdx + 1);

			return nextLinkId;
		} else {
			if (this.hasFoundParking) {
				// easy, we can just park here at our destination link
				return null;
			} else {
				// need to find the next link with parking search logic (in this case random search)
				Id<Link> nextLinkId = this.logic.getNextLink(this.currentLinkId, this.vehicleId, this.mode);
				return nextLinkId;
			}
		}
	}

	// gets called when the parking space the PGS guides to gets occupied
	private void processReroutingEvent(final double time) {
		// report event
		this.events.processEvent(new ReroutingEvent(time, this.vehicleId));

		// reset current link index since we get a new route now
		this.currentLinkIdx = -1;

		// request new guidance from PGS
		this.path = this.PGS.guide(this.currentLinkId,
								   this.route.getEndLinkId(),
								   time);
		
		// if PGS failed to guide, get navigation
		if (this.path == null) processFailedGuidanceEvent(time);
	}

	// gets called when requested a guidance from PGS but PGS failed to provide a guidance
	private void processFailedGuidanceEvent(final double time) {
		// deactivate guidance mode
		this.guidanceMode = false;

		// report event
		this.events.processEvent(new GuidanceFailedEvent(time, this.vehicleId));

		// get navigation to destination
		this.path = this.PGS.navigate(this.currentLinkId,
									  this.route.getEndLinkId(),
									  time);
	}
}
