package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Configuration parameters for all classes having to do with Parking Guidance System.
 * The parameters are uninitialized which probably produces errors.
 * The method initializeFromXML must be used to initialize the parameters correctly.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public final class PGSConfigurator {

    // =================================================================================================
    // ParkingGuidanceSystem
    // -------------------------------------------------------------------------------------------------
    
    // initial search radius for search (choose to be small but big enough to find free spot in most cases)
    public static double initialSearchRadius;
    // step size in which the search radius increases
    public static double stepSize;
    // radius at which the PGS gives up the search
    public static double stopRadius;
    // NOTE: the initial search radius, the step size and the stopRadius could be given to the function for more control for caller.

    // =================================================================================================


    // =================================================================================================
    // FacilityBasedParkingManager
    // -------------------------------------------------------------------------------------------------

    public static String occupationCSVFilePath;

    // =================================================================================================


    // =================================================================================================
    // RunParkingSearchExample
    // -------------------------------------------------------------------------------------------------

    // path to config XML-file
    public static String configXMLFilePath;

    // number of MATSim iterations
    public static int numIterations;

    // =================================================================================================


    // =================================================================================================
    // IdModifier
    // -------------------------------------------------------------------------------------------------

    // the network file path
    public static String networkFilePath;
    // the source and result population file paths
    public static String sourcePopulationFilePath;
    public static String resultPopulationFilePath;
    // shares of parking search strategies
    public static double shareOfPGS;
    public static double shareOfRandom;
    public static double shareOfBenenson;
    public static double shareOfDistanceMemory;
    public static double shareOfNearestParkingSpot;

    // the source and result facility file paths
    public static String sourceFacilitiesFilePath;
    public static String resultFacilitiesFilePath;
    // share of facilities with sensor
    public static double shareOfFacilitiesWithSensor;
    // flag whether the capacity of the facilities should be modified
    public static boolean modifyCapacityFlag;
    // maximum parking space capacity
    public static int maximumCapacity;
    // probability of facility getting capacity 0
    public static double probabilityZeroCapacity;

    // =================================================================================================

    private PGSConfigurator() {
        // prevent creating instances of this class
    }

    public static void initializeFromXML(String PGSConfigFilePath) throws Exception {
        File PGSConfigFile = new File(PGSConfigFilePath);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document PGSConfigDocument = documentBuilder.parse(PGSConfigFile);

        PGSConfigDocument.getDocumentElement().normalize();

        initialSearchRadius = Double.parseDouble(getTagValue("initialSearchRadius", PGSConfigDocument));
        stepSize = Double.parseDouble(getTagValue("stepSize", PGSConfigDocument));
        stopRadius = Double.parseDouble(getTagValue("stopRadius", PGSConfigDocument));

        occupationCSVFilePath = getTagValue("occupationCSVFilePath", PGSConfigDocument);

        configXMLFilePath = getTagValue("configXMLFilePath", PGSConfigDocument);
        numIterations = Integer.parseInt(getTagValue("numIterations", PGSConfigDocument));

        networkFilePath = getTagValue("networkFilePath", PGSConfigDocument);
        sourcePopulationFilePath = getTagValue("sourcePopulationFilePath", PGSConfigDocument);
        resultPopulationFilePath = getTagValue("resultPopulationFilePath", PGSConfigDocument);
        shareOfPGS = Double.parseDouble(getTagValue("shareOfPGS", PGSConfigDocument));
        shareOfRandom = Double.parseDouble(getTagValue("shareOfRandom", PGSConfigDocument));
        shareOfBenenson = Double.parseDouble(getTagValue("shareOfBenenson", PGSConfigDocument));
        shareOfDistanceMemory = Double.parseDouble(getTagValue("shareOfDistanceMemory", PGSConfigDocument));
        shareOfNearestParkingSpot = Double.parseDouble(getTagValue("shareOfNearestParkingSpot", PGSConfigDocument));
        sourceFacilitiesFilePath = getTagValue("sourceFacilitiesFilePath", PGSConfigDocument);
        resultFacilitiesFilePath = getTagValue("resultFacilitiesFilePath", PGSConfigDocument);
        shareOfFacilitiesWithSensor = Double.parseDouble(getTagValue("shareOfFacilitiesWithSensor", PGSConfigDocument));
        modifyCapacityFlag = Boolean.parseBoolean(getTagValue("modifyCapacityFlag", PGSConfigDocument));
        maximumCapacity = Integer.parseInt(getTagValue("maximumCapacity", PGSConfigDocument));
        probabilityZeroCapacity = Double.parseDouble(getTagValue("probabilityZeroCapacity", PGSConfigDocument));
    }

    private static String getTagValue(String tag, Document document) {
        return document.getElementsByTagName(tag).item(0).getTextContent();
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
