package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacility;

/**
 * A little library of util functions for everything having to do with Parking Guidance System (PGS).
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class PGSUtils {
    
    private PGSUtils() {
        // avoid creating instances of this class
    }

    // retrieves link from link-ID
    public static Link getLinkOf(Id<Link> linkID, Network network) {
        return network.getLinks().get(linkID);
    }

    // retrieves link of facility
    public static Link getLinkOf(ActivityFacility facility, Network network) {
        return PGSUtils.getLinkOf(facility.getLinkId(), network);
    }

    // retrieves link-ID of facility
    public static Id<Link> getLinkIdOf(ActivityFacility facility, Network network) {
        return getLinkOf(facility, network).getId();
    }

    // retrieves the from-node from a link-ID
    public static Node getFromNodeOf(Id<Link> linkId, Network network) {
        return PGSUtils.getLinkOf(linkId, network).getFromNode();
    }

    // retrieves the to-node from a link-ID
    public static Node getToNodeOf(Id<Link> linkId, Network network) {
        return PGSUtils.getLinkOf(linkId, network).getToNode();
    }

    // retrieves all facilities of a scenario
    public static Collection<ActivityFacility> getFacilities(Scenario scenario) {
        return scenario.getActivityFacilities()
                       .getFacilitiesForActivityType(ParkingUtils.ParkingStageInteractionType)
                       .values();
    }

    // retrieves attribute by name of a facliity
    public static String getAttribute(String attributeName, ActivityFacility facility) {
        return (String) facility.getAttributes().getAttribute(attributeName);
    }

    // retrieves attribute by name of a person
    public static String getAttribute(String attributeName, Person person) {
        return (String) person.getAttributes().getAttribute(attributeName);
    }

    // retrieves capacity of a facility
    public static double getCapacity(ActivityFacility facility) {
        return facility.getActivityOptions()
                       .get(ParkingUtils.ParkingStageInteractionType)
                       .getCapacity();
    }

    // create a leastCostPathCalculator with the SpeedyALTFactory (stolen from NetworkRouteValidator)
    public static LeastCostPathCalculator makeLeastCostPathCalculator(Network network) {
        SpeedyALTFactory speedyALTFactory = new SpeedyALTFactory();
		FreeSpeedTravelTime freeSpeedTravelTime = new FreeSpeedTravelTime(); //TODO: is this correct?
        LeastCostPathCalculator leastCostPathCalculator = speedyALTFactory.createPathCalculator(network,
                                                                                                new OnlyTimeDependentTravelDisutility(freeSpeedTravelTime),//TODO: is this correct?
                                                                                                freeSpeedTravelTime);

        return leastCostPathCalculator;
    }
}
