package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.vehicles.Vehicle;

/**
 * A guidance failed event happens if the parking spot the Parking Guidance System (PGS) fails to provide a 
 * guidance (due to lack of free parking spots with sensor within stop-radius around the destination).
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class GuidanceFailedEvent extends Event implements HasVehicleId {
    
    public static final String EVENT_TYPE = "guidance failed";

	private final Id<Vehicle> vehicleId;

    public GuidanceFailedEvent (final double time, final Id<Vehicle> vehicleId) {
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
