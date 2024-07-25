package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

/**
 * Library of Strings needed for the Parking Guidance System (PGS).
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public final class PGSLibrary {

    // =================================================================================================
    // ActivityFacility
    // -------------------------------------------------------------------------------------------------
    
    // name of attribute of facility that tells whether it has a sensor or not
    public static final String hasSensor = "hasSensor";

    // values of attribute if it has a sensor and if it does not
    public static final String sensor = "sensor";
    public static final String noSensor = "noSensor";

    // =================================================================================================


    // =================================================================================================
    // Person
    // -------------------------------------------------------------------------------------------------

    // name of attribute of person that tells the parking search strategy of the person
    public static final String parkingSearchStrategy = "parkingSearchStrategy";

    // values of attribute (different parking search strategies)
    public static final String PGS = "PGS";
    public static final String random = "random";
    public static final String benenson = "benenson";
    public static final String distanceMemory = "distanceMemory";
    public static final String nearestParkingSpot = "nearestParkingSpot";

    // =================================================================================================

    private PGSLibrary() {
        // prevent creating instances of this class
    }
}
