package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
//FOR TRIPROUTER: import java.util.List;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.api.core.v01.Scenario;
//FOR TRIPROUTER: import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
//FOR TRIPROUTER: import org.matsim.api.core.v01.population.Leg;
//FOR TRIPROUTER: import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacility;
//FOR TRIPROUTER: import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
//FOR TRIPROUTER: import org.matsim.core.router.LinkWrapperFacility;
//FOR TRIPROUTER: import org.matsim.core.router.TripRouter;
//FOR TRIPROUTER: import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * This class is the heart of the Parking Guidance System (PGS). It hosts the algorithm and data structures needed for 
 * the guidance.
 * 
 * The PGS has knowledge of the occupancy of a fraction of the parking spots in the network and maintains 
 * a QuadTree of all facilities that can be used to get the closest facilities for a given destination.
 * 
 * Users of the PGS can request a guidance in which they specify the destination and give
 * the current link they are on. The PGS then guides to a free parking spot closest to the destination.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class ParkingGuidanceSystem {

    private Network network;
    private ParkingSearchManager parkingSearchManager;

    private QuadTree<ActivityFacility> quadTree;
    private LeastCostPathCalculator leastCostPathCalculator;
    //FOR TRIPROUTER: private TripRouter tripRouter;

    /**
     * Constructor builds the quadTree data structure from the parking facilities of @param scenario
     * (with parking) and creates a leastCostPathCalculator (SpeedyALT).
     * 
     * The @param network is needed for dealing with links later.
     * The @param parkingSearchManager is needed for getting the occupation of parking facilities later.
     */
    @Inject
    public ParkingGuidanceSystem(final Scenario scenario,
                                 final Network network,
                                 final ParkingSearchManager parkingSearchManager) {

        this.network = network;
        this.parkingSearchManager = parkingSearchManager;

        // get all facilities with positive capacity and sensor
        Collection<ActivityFacility> facilities = PGSUtils.getFacilities(scenario);
        Collection<ActivityFacility> facilitiesWithSensorAndCapacity = new LinkedList<ActivityFacility>();

        // go over all facilities and check whether they have a sensor and positive capacity
        for (ActivityFacility facility : facilities) {
            // this attribute tells whether the facility has a sensor
            String hasSensor = PGSUtils.getAttribute(PGSLibrary.hasSensor, facility);
            // if the attribute is null, we assume there is a sensor
            if (hasSensor == null || hasSensor.equals(PGSLibrary.sensor)) {
                // if the facility has a sensor, we check whether it has capacity
                double capacity = PGSUtils.getCapacity(facility);
                if (capacity > 0) {
                    facilitiesWithSensorAndCapacity.add(facility);
                }
            }
        }

        // if there are no facilities with a sensor and capacity, we can not create a quadTree so we set it to null
        if (facilitiesWithSensorAndCapacity.isEmpty()) {
            this.quadTree = null;
        } else {
            // create quadTree from all facilities that have parking spaces
            this.quadTree = QuadTrees.createQuadTree(facilitiesWithSensorAndCapacity);
        }

        this.leastCostPathCalculator = PGSUtils.makeLeastCostPathCalculator(this.network);
        
        //FOR TRIPROUTER: tripRouter = new TripRouter.Builder(scenario.getConfig()).build();
    }

    /**
     * This function is the heart of the PGS. Here lives the algorithm that computes the guidance for the users.
     * 
     * It gets called by a user that wants to drive at time @param startTime 
     * from link @param startLinkId to @param destinationLinkId.
     * 
     * The @return is a Path to the closest parking spot to the destination. Might be null if there is no free parking spot 
     * within a certain stop-radius around the destination.
     * 
     * The function is also called every time the parking spot, that is currently routed to, gets occupied (a so called "rerouting event").
     */
    public Path guide(final Id<Link> startLinkId,
                      final Id<Link> destinationLinkId,
                      final double time) {

        // step 1: find a free parking space closest to destination link
        ActivityFacility closestParking = this.getParkingClosestToDestination(destinationLinkId, time);
        if (closestParking == null) return null; // there is no parking within stop-radius around destination

        Id<Link> closestParkingLinkId = PGSUtils.getLinkOf(closestParking, this.network).getId();

        // step 2: calculate fastest route to closest parking space (the person and the vehicle are not used so we simply pass null for them)
        Path pathToParking = this.navigate(startLinkId,
                                           closestParkingLinkId,
                                           time);

        return pathToParking;
    }

    /**
     * Given a @param destinationLink, @return the facility with free parking spaces that is closest (in walking distance).
     * 
     * The function uses the QuadTree datastructure to search for facilities in discs around the destination with increasing radia.
     * 
     * If there are no free parking spaces within a certain stop radius, null is returned.
     */
    private ActivityFacility getParkingClosestToDestination(final Link destinationLink,
                                                            final double time) {

        if (this.quadTree == null) return null; // if there are no facilities with sensor and capacity, the quadTree will be null

        // step 1: find radius such that there are free parking spots in disc arround destination

        // search radius of the disc
        double searchRadius = PGSConfigurator.initialSearchRadius;
        
        // facilities within initial search radius
        Collection<ActivityFacility> closeFacilities = this.quadTree.getDisk(destinationLink.getCoord().getX(),
                                                                             destinationLink.getCoord().getY(),
                                                                             searchRadius);
        
        // step 2: go over all free parking spots in disk (or ring) and find parking closest to destination
        // facility closest to destination within initial search radius (might be null if there is no facility within disc)
        ActivityFacility closestFacility = this.getParkingClosestToDestinationFromFacilities(closeFacilities,
                                                                                             destinationLink,
                                                                                             time);
        
        // while there is no free parking spot found in disc, we need to increase the search radius. when stop-radius is reached, the PGS gives up
        while (closestFacility == null && searchRadius + PGSConfigurator.stepSize <= PGSConfigurator.stopRadius) {
            // for efficiency, search in ring instead of disk to avoid checking facilities twice
            closeFacilities = this.quadTree.getRing(destinationLink.getCoord().getX(),
                                                    destinationLink.getCoord().getY(),
                                                    searchRadius,
                                                    searchRadius + PGSConfigurator.stepSize);
            
            closestFacility = this.getParkingClosestToDestinationFromFacilities(closeFacilities,
                                                                                destinationLink,
                                                                                time);
            searchRadius += PGSConfigurator.stepSize;
        }
        
        return closestFacility;
    }

    /**
     * Of all @param facilities with free parking spaces, the one that is closest to the @param destinationLink will be @return.
     * We calculate how close a facility is to the destination with the tripRouter (TODO).
     * 
     * Returns null if there is no facility with free parking spots.
     */
    private ActivityFacility getParkingClosestToDestinationFromFacilities(final Collection<ActivityFacility> facilities,
                                                                          final Link destinationLink,
                                                                          final double time) {
        // closest facility found so far
        ActivityFacility closestFacility = null;
        // travel time from closest facility to destination
        double shortestTravelTime = -1;

        // go over all facilities and check whether there are free parking spots
        for (ActivityFacility facility : facilities) {
            if (this.parkingSearchManager.isThereFreeParkingSpaceAt(facility)) {
                // the facility has a free parking space => check whether it is closer than the closest facility found so far (we dont give time since we walk)
                Path path = this.navigate(facility,
                                          destinationLink,
                                          time);
                
                double travelTime = path.travelTime;

                //FOR TRIPROUTER: List<? extends PlanElement> route = this.tripRouter.calcRoute(TransportMode.walk,
                //FOR TRIPROUTER:                                                               facility,
                //FOR TRIPROUTER:                                                               new LinkWrapperFacility(destinationLink),
                //FOR TRIPROUTER:                                                               0,
                //FOR TRIPROUTER:                                                               null,
                //FOR TRIPROUTER:                                                               new AttributesImpl());
                //FOR TRIPROUTER: List<Leg> legs = TripStructureUtils.getLegs(route);
                //FOR TRIPROUTER: double travelTime = 0;
                //FOR TRIPROUTER: for (Leg leg : legs) {
                //FOR TRIPROUTER:     travelTime += leg.getTravelTime().seconds();
                //FOR TRIPROUTER: }
                
                if (closestFacility == null || travelTime < shortestTravelTime) {
                    // the facility is closer than the closest facility and takes its place
                    closestFacility = facility;
                    shortestTravelTime = travelTime;
                }
            }
        }

        return closestFacility;
    }

    /**
     * This function simply navigates. It is called as helper method by getParkingClosestToDestinationFromFacilities
     * but also by PGSDynLeg in the case where the PGS can not guide.
     * 
     * @return is the fastest route from @param startLinkId to @param destinationLinkId at 
     * given @param time
     */
    public Path navigate(final Id<Link> startLinkId,
                         final Id<Link> destinationLinkId,
                         final double time) {
        Node startLinkToNode = PGSUtils.getToNodeOf(startLinkId, this.network);
        Node destinationLinkFromNode = PGSUtils.getFromNodeOf(destinationLinkId, this.network);

        Path path = this.leastCostPathCalculator.calcLeastCostPath(startLinkToNode,
                                                                   destinationLinkFromNode,
                                                                   time,
                                                                   null,
                                                                   null);

        /**
        * since the leastCostPathCalculator is only routing from the to-node of the start link to the from-node of the closest parking link,
        * we need to append the link of the parking facility at the end (and therefore also the to-node from the closest parking link)
        */
        Link destinationLink = PGSUtils.getLinkOf(destinationLinkId, this.network);

        try {
            path.appendLink(destinationLink);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    // for convenience
    private Path navigate(final ActivityFacility facility,
                          final Link destinationLink,
                          final double time) {
        Id<Link> startLinkId = PGSUtils.getLinkIdOf(facility, this.network);
        Id<Link> destinationLinkId = destinationLink.getId();
        
        return navigate(startLinkId,
                        destinationLinkId,
                        time);
    }

    // for convenience
    private ActivityFacility getParkingClosestToDestination(final Id<Link> destinationLinkId,
                                                            final double time) {
        Link destinationLink = PGSUtils.getLinkOf(destinationLinkId, this.network);
        
        return this.getParkingClosestToDestination(destinationLink, time);
    }
}
