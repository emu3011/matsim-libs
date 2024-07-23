package org.matsim.api.core.v01.events;

/**
 * Interface for events that carry an occupancy-information.
 * Analogue to e.g. HasPersonId.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public interface HasOccupancy {
    String ATTRIBUTE_OCCUPANCY = "occupancy";

    Integer getOccupancy();
}
