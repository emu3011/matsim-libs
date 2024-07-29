package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;

/**
 * This class implements methods to modify the population and facilities file for the Parking Guidance System.
 * 
 * This is needed for
 *  1. the visualization in Via
 *  2. the agent factory
 *  3. the PGS construction
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class IdModifier {

    /**
     * Modifies the person-IDs to have suffix that tells what parking strategy the person has (for Via)
     * and add parking strategy attribute (for agent factory).
     * 
     * Given are the @param sourcePopulationFilePath and the @param resultPopulationFilePath and the 
     * shares of the population that have a certain parking search strategy:
     *  1. @param sharePGS
     *  2. @param shareRandom
     *  3. @param shareBenenson
     *  4. @param shareDistanceMemory
     *  5. @param shareNearestParkingSpot
     */
    public static void modifyPersonIdsToStrategy(String sourcePopulationFilePath,
                                                 String resultPopulationFilePath,
                                                 double shareOfPGS,
                                                 double shareOfRandom,
                                                 double shareOfBenenson,
                                                 double shareOfDistanceMemory,
                                                 double shareOfNearestParkingSpot) {
        
        // step 1: read the source population from the source population file into a scenario
        Config sourceConfig = ConfigUtils.createConfig();
        Scenario sourceScenario = ScenarioUtils.createMutableScenario(sourceConfig);
        PopulationReader populationReader = new PopulationReader(sourceScenario);
        populationReader.readFile(sourcePopulationFilePath);
        
        // step 2: prepare the result population
        Config resultConfig = ConfigUtils.createConfig();
        Scenario resultSenario = ScenarioUtils.loadScenario(resultConfig);
        PopulationFactory popFactory = resultSenario.getPopulation().getFactory();

        // step 3: get the total number of persons and calculate the total amount of persons of each strategy
        Collection<? extends Person> sourcePersons = PGSUtils.getPersons(sourceScenario);
        int numPersons = sourcePersons.size();

        int numRandom = (int) (numPersons * shareOfRandom);
        int numBenenson = (int) (numPersons * shareOfBenenson);
        int numDistanceMemory = (int) (numPersons * shareOfDistanceMemory);
        int numNearestParkingSpot = (int) (numPersons * shareOfNearestParkingSpot);
        int numPGS = numPersons - numRandom - numBenenson - numDistanceMemory - numNearestParkingSpot; // give rest to PGS

        // step 4: shuffle persons so that we can randomly assign the parking search strategies
        List<Person> listOfSourcePersons = new ArrayList<>(sourcePersons);
        Collections.shuffle(listOfSourcePersons);

        // step 5: go over all persons and assign parking strategies
        int currentPersonIdx = 1;
        for (Person sourcePerson : listOfSourcePersons) {

            // determine parking strategy
            String parkingStrategy = "";
            if (currentPersonIdx <= numPGS) {
                parkingStrategy = PGSLibrary.PGS;
            } else if (currentPersonIdx - numPGS <= numRandom) {
                parkingStrategy = PGSLibrary.random;
            } else if (currentPersonIdx - numPGS - numRandom <= numBenenson) {
                parkingStrategy = PGSLibrary.benenson;
            } else if (currentPersonIdx - numPGS - numRandom - numBenenson <= numDistanceMemory) {
                parkingStrategy = PGSLibrary.distanceMemory;
            } else {
                parkingStrategy = PGSLibrary.nearestParkingSpot;
            }

            // build modified ID
            String modifiedId = sourcePerson.getId().toString() + "_" + parkingStrategy;
            // create person with modified ID
            Person resultPerson = popFactory.createPerson(Id.createPersonId(modifiedId));
            resultPerson.getAttributes().putAttribute(PGSLibrary.parkingSearchStrategy, parkingStrategy);
            // add all attributes of the source person to the result person
            for (String attributeKey : sourcePerson.getAttributes().getAsMap().keySet()) {
                resultPerson.getAttributes().putAttribute(attributeKey, sourcePerson.getAttributes().getAttribute(attributeKey));
            }
            // add plan of source person to result person
            resultPerson.addPlan(sourcePerson.getSelectedPlan());

            // add result person to the result scenario
            resultSenario.getPopulation().addPerson(resultPerson);

            currentPersonIdx++;
        }

        // step 6: write the result population into the result population file
        PopulationWriter popWriter = new PopulationWriter(resultSenario.getPopulation());
        popWriter.write(resultPopulationFilePath);
    }

    /**
     * Modifies the facility-IDs to have suffix that tells whether the facility has a sensor or not (for Via)
     * and add attribute that tells the same (for agent factory).
     * 
     * Given are the @param sourceFacilitiesFilePath and the @param resultFacilitiesFilePath and the 
     * @param shareOfFacilitiesWithSensor
     */
    public static void modifyFacilityIdsToKnowledge(String sourceFacilitiesFilePath,
                                                    String resultFacilitiesFilePath,
                                                    double shareOfFacilitiesWithSensor) {
        // step 1: read the source facilities from the source facilities file path
        Config sourceConfig = ConfigUtils.createConfig();
        Scenario sourceScenario = ScenarioUtils.createMutableScenario(sourceConfig);
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(sourceScenario);
        facilitiesReader.readFile(sourceFacilitiesFilePath);
        
        // step 2: prepare the result facilities
        Config resultConfig = ConfigUtils.createConfig();
        Scenario resultSenario = ScenarioUtils.loadScenario(resultConfig);
        ActivityFacilitiesFactory facFactory = resultSenario.getActivityFacilities().getFactory();

        // step 3: get the total number of facilities and calculate the total amount of facilities with a sensor
        Collection<? extends ActivityFacility> sourceFacilities = sourceScenario.getActivityFacilities().getFacilities().values();
        int numFacilities = sourceFacilities.size();

        int numFacilitiesWithSensor = (int) (numFacilities * shareOfFacilitiesWithSensor);

        // step 4: shuffle facilities so that we can randomly assign the sensors
        List<ActivityFacility> listOfSourceFacilities = new ArrayList<>(sourceFacilities);
        Collections.shuffle(listOfSourceFacilities);

        // step 5: go over all facilities and assign sensors
        int currentFacilityIdx = 1;
        for (ActivityFacility sourceFacility : listOfSourceFacilities) {

            // determine whether facility has a sensor
            String hasSensor = "";
            if (currentFacilityIdx <= numFacilitiesWithSensor) {
                hasSensor = PGSLibrary.sensor;
            } else {
                hasSensor = PGSLibrary.noSensor;
            }

            // build modified ID
            String modifiedId = sourceFacility.getId().toString() + "_" + hasSensor;
            // create facility with modified ID
            ActivityFacility resultFacility = facFactory.createActivityFacility(Id.createFacilityId(modifiedId), sourceFacility.getCoord(), sourceFacility.getLinkId());

            // add all attributes of the source facility to the result person
            resultFacility.getAttributes().putAttribute(PGSLibrary.hasSensor, hasSensor);
            Set<String> attributeKeys = sourceFacility.getAttributes().getAsMap().keySet();
            for (String attributeKey : attributeKeys) {
                resultFacility.getAttributes().putAttribute(attributeKey, sourceFacility.getAttributes().getAttribute(attributeKey));
            }

            // add all activity options of the source facility to the result facility
            Collection<ActivityOption> activityOptions = sourceFacility.getActivityOptions().values();
            for (ActivityOption activityOption : activityOptions) {
                // if the activity option is the parking space capacity, modify the capacity before adding
                if (PGSConfigurator.modifyCapacityFlag && activityOption.getType().equals(ParkingUtils.ParkingStageInteractionType)) {
                    // give the facility a random capacity
                    int capacity = MatsimRandom.getRandom().nextInt(PGSConfigurator.maximumCapacity);
                    // zero the capacity with probabilityZeroCapacity
                    double randomDouble = MatsimRandom.getRandom().nextDouble();
                    if (randomDouble <= PGSConfigurator.probabilityZeroCapacity) capacity = 0;

                    activityOption.setCapacity(capacity);
                }
                resultFacility.addActivityOption(activityOption);
            }

            // add result facility to the result scenario
            resultSenario.getActivityFacilities().addActivityFacility(resultFacility);

            currentFacilityIdx++;
        }

        // step 6: write the result facility into the result facilities file
        FacilitiesWriter facWriter = new FacilitiesWriter(resultSenario.getActivityFacilities());
        facWriter.write(resultFacilitiesFilePath);
    }
}
