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
    public static final String configXMLFilePath = "/Users/emanuesk/Documents/PGSFiles/config.xml";

    // number of MATSim iterations
    public static final int numIterations = 1;

    // =================================================================================================


    // =================================================================================================
    // IdModifier
    // -------------------------------------------------------------------------------------------------

    // the source and result population file paths
    public static final String sourcePopulationFilePath = "/Users/emanuesk/Documents/PGSFiles/population100.xml";
    public static final String resultPopulationFilePath = "/Users/emanuesk/Documents/PGSFiles/population100_PGS.xml";
    // shares of parking search strategies
    public static final double shareOfPGS = 0.3;
    public static final double shareOfRandom = 0.7;
    public static final double shareOfBenenson = 0.0;
    public static final double shareOfDistanceMemory = 0.0;
    public static final double shareOfNearestParkingSpot = 0.0;

    // the source and result facility file paths
    public static final String sourceFacilitiesFilePath = "/Users/emanuesk/Documents/PGSFiles/parkingFacilities.xml";
    public static final String resultFacilitiesFilePath = "/Users/emanuesk/Documents/PGSFiles/parkingFacilities_PGS.xml";
    // share of facilities with sensor
    public static final double shareOfFacilitiesWithSensor = 1.0;
    // flag whether the capacity of the facilities should be modified
    public static final boolean modifyCapacityFlag = true;
    // maximum parking space capacity
    public static final int maximumCapacity = 10;
    // probability of facility getting capacity 0
    public static final double probabilityZeroCapacity = 0.7;

    // =================================================================================================

    private PGSConfigurator() {
        // prevent creating instances of this class
    }

    // check whether the configuration parameters are legal
    public static void checkIfParametersAreLegal() throws Exception {
        double[] shares = {shareOfPGS, shareOfRandom, shareOfBenenson, shareOfDistanceMemory, shareOfNearestParkingSpot};
        double sum = 0;

        for (double share : shares) {
            // check that the shares of the parking search strategies are between 0 and 1
            if (share < 0) throw new Exception("a share of the parking search strategies is not in [0, 1]");
            sum += share;
        }

        // check that the shares of the parking search strategies sum up to 1
        if (sum != 1.0) throw new Exception("shares of the parking search strategies do not sum up to 1");

        // check that stop-radius is bigger than the initial radius
        if (initialSearchRadius > stopRadius) throw new Exception("stop-radius is not bigger than the initial search radius");

        if (shareOfFacilitiesWithSensor < 0 || shareOfFacilitiesWithSensor > 1) throw new Exception("share of facilities with sensor is not in [0, 1]");
    }
}
