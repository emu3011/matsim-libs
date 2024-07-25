package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;

/**
 * The parking search logic for users of the Parking Guidance System (PGS).
 * The logic is only needed if the PGS fails to provide guidance.
 * 
 * The search logic is simply adapted (1-to-1) from RandomSearchLogic.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class PGSSearchLogic implements ParkingSearchLogic {
    
	private Network network;
	private final Random random = MatsimRandom.getLocalInstance();

	public PGSSearchLogic (Network network) {
		this.network = network;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId, String mode) {
		Link currentLink = PGSUtils.getLinkOf(currentLinkId, this.network);

		// from all outgoing links, choose one at random
		List<Link> outgoingLinksIds = ParkingUtils.getOutgoingLinksForMode(currentLink, mode);
		int randInt = this.random.nextInt(outgoingLinksIds.size());
		Id<Link> randomOutgoingLinkId = outgoingLinksIds.get(randInt).getId();
		return randomOutgoingLinkId;
	}

	@Override
	public void reset() {
	}

}
