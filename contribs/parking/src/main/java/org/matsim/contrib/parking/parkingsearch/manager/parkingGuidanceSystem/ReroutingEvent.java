package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.vehicles.Vehicle;

/**
 * A rerouting event happens if the parking spot the Parking Guidance System (PGS) is guiding to gets occupied and therefore forces
 * the PGS to reroute.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public final class ReroutingEvent extends Event implements HasVehicleId {

    public static final String EVENT_TYPE = "rerouting";

	private final Id<Vehicle> vehicleId;

    public ReroutingEvent (final double time, final Id<Vehicle> vehicleId) {
        super(time);
        this.vehicleId = vehicleId;
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}
}
