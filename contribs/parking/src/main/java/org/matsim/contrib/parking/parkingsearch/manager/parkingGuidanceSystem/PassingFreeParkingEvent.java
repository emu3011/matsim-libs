package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.vehicles.Vehicle;

/**
 * A passing free parking event happens if the user is currently guided by the Parking Guidance System (PGS)
 * and a free parking space is passed (due to lack of sensor at this parking).
 * 
 * Note: the event is currently always thrown and does not care whether the parking space is interesting
 * for the user or not.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public final class PassingFreeParkingEvent extends Event implements HasVehicleId {

    public static final String EVENT_TYPE = "passingFreeParking";

    private final Id<Vehicle> vehicleId;

    public PassingFreeParkingEvent(final double time, final Id<Vehicle> vehicleId) {
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
