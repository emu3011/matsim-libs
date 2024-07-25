package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

/**
 * Configuration parameters for all classes having to do with Parking Guidance System.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public final class PGSConfigurator {

    // =================================================================================================
    // ParkingGuidanceSystem
    // -------------------------------------------------------------------------------------------------
    
    // initial search radius for search (choose to be small but big enough to find free spot in most cases)
    public static final double initialSearchRadius = 300;
    // step size in which the search radius increases
    public static final double stepSize = 100;
    // radius at which the PGS gives up the search
    public static final double stopRadius = 1000;
    // NOTE: the initial search radius, the step size and the stopRadius could be given to the function for more control for caller.

    // =================================================================================================


    // =================================================================================================
    // RunParkingSearchExample
    // -------------------------------------------------------------------------------------------------

    // path to config XML-file
    public static final String configXMLFilePath = "parkingsearch/config.xml";

    // number of MATSim iterations
    public static final int numIterations = 1;

    // =================================================================================================


    // =================================================================================================
    // RunIdModifier
    // -------------------------------------------------------------------------------------------------

    // the source and result population file paths
    public static final String sourcePopulationFilePath = "/Users/emanuesk/Documents/GitHub/matsim-libs/contribs/parking/src/main/resources/parkingsearch/population100.xml";
    public static final String resultPopulationFilePath = "/Users/emanuesk/Documents/GitHub/matsim-libs/contribs/parking/src/main/resources/parkingsearch/population100_PGS.xml";
    // shares of parking search strategies
    public static final double shareOfPGS = 0.5;
    public static final double shareOfRandom = 0.5;
    public static final double shareOfBenenson = 0.0;
    public static final double shareOfDistanceMemory = 0.0;
    public static final double shareOfNearestParkingSpot = 0.0;

    // the source and result facility file paths
    public static final String sourceFacilitiesFilePath = "/Users/emanuesk/Documents/GitHub/matsim-libs/contribs/parking/src/main/resources/parkingsearch/parkingFacilities.xml";
    public static final String resultFacilitiesFilePath = "/Users/emanuesk/Documents/GitHub/matsim-libs/contribs/parking/src/main/resources/parkingsearch/parkingFacilities_PGS.xml";
    // share of facilities with sensor
    public static final double shareOfFacilitiesWithSensor = 0.0;

    // =================================================================================================

    private PGSConfigurator() {
        // prevent creating instances of this class
    }

    public static boolean parametersAreLegal() {
        return false;
    }
}
