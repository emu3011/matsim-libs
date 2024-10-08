/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSConfigurator;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSUtils;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author jbischoff, schlenther, Ricardo Ewert
 */
public class FacilityBasedParkingManager implements ParkingSearchManager, ShutdownListener { // added ShutdownListener by Emanuel Skodinis (emanuesk@ethz.ch): needed for closing the CSV-writer

	protected Map<Id<Link>, Integer> capacity = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> occupation = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> reservationsRequests = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> rejectedParkingRequest = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> numberOfParkedVehicles = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> numberOfWaitingActivities = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> numberOfStaysFromGetOffUntilGetIn = new HashMap<>();
	protected Map<Id<ActivityFacility>, MutableLong> numberOfParkingBeforeGetIn = new HashMap<>();
	protected Map<Id<Link>, TreeMap<Double, Id<Vehicle>>> waitingVehicles = new HashMap<>();
	protected TreeMap<Integer, MutableLong> rejectedReservationsByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> foundParkingByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> unparkByTime = new TreeMap<>();
	protected Map<Id<ActivityFacility>, ActivityFacility> parkingFacilities;
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations = new HashMap<>();
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservation = new HashMap<>();
	protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();
	protected Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();
	protected Network network;
	protected ParkingSearchConfigGroup psConfigGroup;
	protected boolean canParkOnlyAtFacilities;
	private QSim qsim;
	private final int maxSlotIndex;
	private final int maxTime;
	private final int timeBinSize;
	private final int startTime;
	private ICSVWriter writer; // added by Emanuel Skodinis (emanuesk@ethz.ch): needed for writing the occupation.csv file which is needed for visualising the occupation in Via

	@Inject
	public FacilityBasedParkingManager(Scenario scenario) throws IOException {
		// added by Emanuel Skodinis (emanuesk@ethz.ch): create new CSW-writer and write the header
		writer = new CSVWriterBuilder(new FileWriter(PGSConfigurator.occupationCSVFilePath, false)).withQuoteChar(ICSVWriter.NO_QUOTE_CHARACTER).build(); // append is false such that the file gets overwritten every time, NO_QUOTE_CHARACTER such that it does not write the strings with quotes
		// write the header row
		String[] header = {"X", "Y", "FACILITY_ID", "TIME", "CAPACITY", "OCCUPATION", "FREE", "PERCENTAGE_OF_OCCUPATION"};
		writer.writeNext(header);

		psConfigGroup = (ParkingSearchConfigGroup) scenario.getConfig().getModules().get(
			ParkingSearchConfigGroup.GROUP_NAME);
		canParkOnlyAtFacilities = psConfigGroup.getCanParkOnlyAtFacilities();
		this.network = scenario.getNetwork();
		parkingFacilities = scenario.getActivityFacilities()
			.getFacilitiesForActivityType(ParkingUtils.ParkingStageInteractionType);
		LogManager.getLogger(getClass()).info(parkingFacilities.toString());
		this.timeBinSize = 15 * 60;
		this.maxTime = 24 * 3600 - 1;
		this.maxSlotIndex = (this.maxTime / this.timeBinSize) + 1;
		this.startTime = 0 * 3600; // changed by Emanuel Skodinis (emanuesk@ethz.ch)

		for (ActivityFacility fac : this.parkingFacilities.values()) {
			Id<Link> linkId = fac.getLinkId();
			Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
			if (this.facilitiesPerLink.containsKey(linkId)) {
				parkingOnLink = this.facilitiesPerLink.get(linkId);
			}
			parkingOnLink.add(fac.getId());
			this.facilitiesPerLink.put(linkId, parkingOnLink);
			this.waitingVehicles.computeIfAbsent(linkId, (k) -> new TreeMap<>());
			this.occupation.put(fac.getId(), new MutableLong(0));

			// added by Emanuel Skodinis (emanuesk@ethz.ch): write initial occupation of the facilities
			int capacity = PGSUtils.getCapacity(fac);
			String[] row = {String.valueOf(fac.getCoord().getX()), String.valueOf(fac.getCoord().getY()), fac.getId().toString(), "0.0", String.valueOf(capacity), "0", String.valueOf(capacity), "0.0"};
			writer.writeNext(row);

			this.reservationsRequests.put(fac.getId(), new MutableLong(0));
			this.rejectedParkingRequest.put(fac.getId(), new MutableLong(0));
			this.numberOfParkedVehicles.put(fac.getId(), new MutableLong(0));
			this.numberOfWaitingActivities.put(fac.getId(), new MutableLong(0));
			this.numberOfStaysFromGetOffUntilGetIn.put(fac.getId(), new MutableLong(0));
			this.numberOfParkingBeforeGetIn.put(fac.getId(), new MutableLong(0));
		}
		int slotIndex = getTimeSlotIndex(startTime);
		while (slotIndex <= maxSlotIndex) {
			rejectedReservationsByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			foundParkingByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			unparkByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			slotIndex++;
		}
	}

	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		boolean canPark = false;

		if (linkIdHasAvailableParkingForVehicle(linkId, vehicleId)) {
			canPark = true;
			// LogManager.getLogger(getClass()).info("veh: "+vehicleId+" link
			// "+linkId + " can park "+canPark);
		}

		return canPark;
	}

	/**
	 * Checks if it is possible if you can park at this link for the complete time.
	 *
	 * @param linkId
	 * @param stopDuration
	 * @param getOffDuration
	 * @param pickUpDuration
	 * @param now
	 * @return
	 */
	public boolean canParkAtThisFacilityUntilEnd(Id<Link> linkId, double stopDuration, double getOffDuration, double pickUpDuration, double now) {
		Set<Id<ActivityFacility>> facilities = this.facilitiesPerLink.get(linkId);
		if (facilities != null) {
			double totalNeededParkingDuration = getOffDuration + stopDuration + pickUpDuration;
			for (Id<ActivityFacility> facility : facilities) {
				double maxParkingDurationAtFacilityInHours = Double.MAX_VALUE;
				if (this.parkingFacilities.get(facility).getAttributes().getAsMap().containsKey("maxParkingDurationInHours"))
					maxParkingDurationAtFacilityInHours = 3600 * (double) this.parkingFacilities.get(facility).getAttributes().getAsMap().get(
						"maxParkingDurationInHours");
				if (maxParkingDurationAtFacilityInHours > totalNeededParkingDuration) {
					ActivityOption parkingOptions = this.parkingFacilities.get(facility).getActivityOptions().get("parking");
					if (!parkingOptions.getOpeningTimes().isEmpty()) {
						if ((parkingOptions.getOpeningTimes().first().getStartTime() == 0 && parkingOptions.getOpeningTimes().first().getEndTime() == 24 * 3600))
							if (parkingOptions.getOpeningTimes().first().getStartTime() <= now && parkingOptions.getOpeningTimes().first().getEndTime() >= now + totalNeededParkingDuration)
								return true;
					} else
						return true;
				}
			}
		}
		return false;
	}

	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid) {
		// LogManager.getLogger(getClass()).info("link "+linkId+" vehicle "+vid);
		if (!this.facilitiesPerLink.containsKey(linkId) && !canParkOnlyAtFacilities) {
			// this implies: If no parking facility is present, we suppose that
			// we can park freely (i.e. the matsim standard approach)
			// it also means: a link without any parking spaces should have a
			// parking facility with 0 capacity.
			// LogManager.getLogger(getClass()).info("link not listed as parking
			// space, we will say yes "+linkId);

			return true;
		} else if (!this.facilitiesPerLink.containsKey(linkId)) {
			return false;
		}
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		for (Id<ActivityFacility> facId : parkingFacilitiesAtLink) {
			double cap = this.parkingFacilities.get(facId).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType)
				.getCapacity();
			this.reservationsRequests.get(facId).increment();
			double occ = this.occupation.get(facId).doubleValue();
			if (occ < cap) {
				// LogManager.getLogger(getClass()).info("occ:
				// "+this.occupation.get(fac).toString()+" cap: "+cap);
				this.occupation.get(facId).increment();
				this.parkingReservation.put(vid, facId);

				return true;
			}
			this.rejectedParkingRequest.get(facId).increment();
		}
		return false;
	}

	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingLocations.containsKey(vehicleId)) {
			return this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getLinkId();
		} else return this.parkingLocationsOutsideFacilities.getOrDefault(vehicleId, null);
	}

	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		return parkVehicleAtLink(vehicleId, linkId, time);
	}

	protected boolean parkVehicleAtLink(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
			return true;
		} else {
			Id<ActivityFacility> facId = this.parkingReservation.remove(vehicleId);
			if (facId != null) {
				this.parkingLocations.put(vehicleId, facId);
				this.numberOfParkedVehicles.get(facId).increment();

				// added by Emanuel Skodinis (emanuesk@ethz.ch): write new row since vehicle got parked and therefore the occupation change has to be logged
				ActivityFacility fac = parkingFacilities.get(facId);
				int capacity = PGSUtils.getCapacity(fac);
				int occ = this.occupation.get(facId).intValue();
				int free = capacity - occ;
				double percentageOfOccupancy = (double) occ / (double) capacity;
				String[] row = {String.valueOf(fac.getCoord().getX()), String.valueOf(fac.getCoord().getY()), facId.toString(), String.valueOf(time), String.valueOf(capacity), String.valueOf(occ), String.valueOf(free), String.valueOf(percentageOfOccupancy)};
				writer.writeNext(row);
				try {
					writer.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}

				foundParkingByTime.get(getTimeSlotIndex(time) * timeBinSize).increment();
				return true;
			} else {
				throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
					+ " arrival on link " + linkId + " with parking restriction");
			}
		}
	}

	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingLocations.containsKey(vehicleId)) {
			this.parkingLocationsOutsideFacilities.remove(vehicleId);
			return true;

			// we assume the person parks somewhere else
		} else {
			Id<ActivityFacility> facId = this.parkingLocations.remove(vehicleId);
			this.occupation.get(facId).decrement();

			// added by Emanuel Skodinis (emanuesk@ethz.ch): write new row since vehicle got unparked and therefore the occupation change has to be logged
			ActivityFacility fac = parkingFacilities.get(facId);
			int capacity = (int) this.parkingFacilities.get(facId).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
			int occ = this.occupation.get(facId).intValue();
			int free = capacity - occ;
			double percentageOfOccupancy = (double) occ / (double) capacity;
			String[] row = {String.valueOf(fac.getCoord().getX()), String.valueOf(fac.getCoord().getY()), facId.toString(), String.valueOf(time), String.valueOf(capacity), String.valueOf(occ), String.valueOf(free), String.valueOf(percentageOfOccupancy)};
			writer.writeNext(row);
			try {
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			unparkByTime.get(getTimeSlotIndex(time) * timeBinSize).increment();
			return true;
		}
	}

	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<ActivityFacility>, MutableLong> e : this.occupation.entrySet()) {
			Id<Link> linkId = this.parkingFacilities.get(e.getKey()).getLinkId();
			double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
				.get(ParkingUtils.ParkingStageInteractionType).getCapacity();
			double x = this.parkingFacilities.get(e.getKey()).getCoord().getX();
			double y = this.parkingFacilities.get(e.getKey()).getCoord().getY();

			String s = linkId.toString() + ";" + x + ";" + y + ";" + e.getKey().toString() + ";" + capacity + ";" + e.getValue().toString() + ";" + this.reservationsRequests.get(
				e.getKey()).toString() + ";" + this.numberOfParkedVehicles.get(e.getKey()).toString() + ";" + this.rejectedParkingRequest.get(
				e.getKey()).toString() + ";" + this.numberOfWaitingActivities.get(
				e.getKey()).toString() + ";" + this.numberOfStaysFromGetOffUntilGetIn.get(e.getKey()).intValue() + ";" + this.numberOfParkingBeforeGetIn.get(e.getKey()).intValue();
			stats.add(s);
		}
		return stats;
	}

	public List<String> produceTimestepsStatistics() {
		List<String> stats = new ArrayList<>();
		for (int time : rejectedReservationsByTime.keySet()) {

			String s = Time.writeTime(time, Time.TIMEFORMAT_HHMM) + ";" + rejectedReservationsByTime.get(time) + ";" + foundParkingByTime.get(
				time) + ";" + unparkByTime.get(time);
			stats.add(s);

		}
		return stats;
	}

	public double getNrOfAllParkingSpacesOnLink(Id<Link> linkId) {
		double allSpaces = 0;
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (!(parkingFacilitiesAtLink == null)) {
			for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
				allSpaces += this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
			}
		}
		return allSpaces;
	}

	public double getNrOfFreeParkingSpacesOnLink(Id<Link> linkId) {
		double allFreeSpaces = 0;
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			return 0;
		} else {
			for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
				int cap = (int) this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
				allFreeSpaces += (cap - this.occupation.get(fac).intValue());
			}
		}
		return allFreeSpaces;
	}

	public Map<Id<ActivityFacility>, ActivityFacility> getParkingFacilities() {
		return this.parkingFacilities;
	}

	public void registerRejectedReservation(double now) {
		rejectedReservationsByTime.get(getTimeSlotIndex(now) * timeBinSize).increment();
	}

	public TreeSet<Integer> getTimeSteps() {
		TreeSet<Integer> timeSteps = new TreeSet<>();
		int slotIndex = 0;
		while (slotIndex <= maxSlotIndex) {
			timeSteps.add(slotIndex * timeBinSize);
			slotIndex++;
		}
		return timeSteps;
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	/**
	 * Gives the duration of the staging activity of parking
	 *
	 * @return
	 */
	public double getParkStageActivityDuration() {
		return psConfigGroup.getParkduration();
	}

	/**
	 * Gives the duration of the staging activity of unparking
	 *
	 * @return
	 */
	public double getUnParkStageActivityDuration() {
		return psConfigGroup.getUnparkduration();
	}

	@Override
	public void reset(int iteration) {
		for (Id<ActivityFacility> fac : this.rejectedParkingRequest.keySet()) {
			this.rejectedParkingRequest.get(fac).setValue(0);
			this.reservationsRequests.get(fac).setValue(0);
			this.numberOfParkedVehicles.get(fac).setValue(0);
			this.numberOfWaitingActivities.get(fac).setValue(0);
		}
		waitingVehicles.clear();
	}

	public void addVehicleForWaitingForParking(Id<Link> linkId, Id<Vehicle> vehicleId, double now) {
//		System.out.println(now + ": vehicle " +vehicleId.toString() + " starts waiting here: " + linkId.toString());
		waitingVehicles.get(linkId).put(now + getParkStageActivityDuration() + 1, vehicleId);
		for (Id<ActivityFacility> fac : this.facilitiesPerLink.get(linkId)) {
			this.numberOfWaitingActivities.get(fac).increment();
			break;
		}

	}

	public void checkFreeCapacitiesForWaitingVehicles(QSim qSim, double now) {
		for (Id<Link> linkId : waitingVehicles.keySet()) {
			if (!waitingVehicles.get(linkId).isEmpty()) {
				for (Id<ActivityFacility> fac : this.facilitiesPerLink.get(linkId)) {
					int cap = (int) this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
					while (this.occupation.get(fac).intValue() < cap && !waitingVehicles.get(linkId).isEmpty()) {
						double startWaitingTime = waitingVehicles.get(linkId).firstKey();
						if (startWaitingTime > now)
							break;
						Id<Vehicle> vehcileId = waitingVehicles.get(linkId).remove(startWaitingTime);
						DynAgent agent = (DynAgent) qSim.getAgents().get(Id.createPersonId(vehcileId.toString()));
						reserveSpaceIfVehicleCanParkHere(vehcileId, linkId);
						agent.endActivityAndComputeNextState(now);
						qsim.rescheduleActivityEnd(agent);
					}
				}
			}
		}
	}

	public void setQSim(QSim qSim) {
		qsim = qSim;
	}

	public void registerStayFromGetOffUntilGetIn(Id<Vehicle> vehcileId) {
		this.numberOfStaysFromGetOffUntilGetIn.get(parkingLocations.get(vehcileId)).increment();
	}

	public void registerParkingBeforeGetIn(Id<Vehicle> vehcileId) {
		this.numberOfParkingBeforeGetIn.get(parkingLocations.get(vehcileId)).increment();
	}

	/**
	 * At shutdown close the CSV-writer
	 * 
	 * @author Emanuel Skodinis (emanuesk@ethz.ch)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return number of free parking spaces at @param linkId.
	 * 
	 * @author Emanuel Skodinis (emanuesk@ethz.ch)
	 */
	public int getNumFreeParkingSpacesAt(Id<Link> linkId) {
		int numFreeParkingSpaces = 0;
		// get all parking facilities at the link
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) return numFreeParkingSpaces;
		
		// go over all facilities at the link and add up all numbers of free parking spaces
		for (Id<ActivityFacility> facility : parkingFacilitiesAtLink) {
			int capacity = PGSUtils.getCapacity(this.parkingFacilities.get(facility));
			int occupation = this.occupation.get(facility).intValue();
			numFreeParkingSpaces += (capacity - occupation);
		}

		return numFreeParkingSpaces;
	}

	/**
	 * @return number of free parking spaces at @param facility.
	 * 
	 * @author Emanuel Skodinis (emanuesk@ethz.ch)
	 */
	@Override
	public int getNumFreeParkingSpacesAt(ActivityFacility facility) {
		int capacity = PGSUtils.getCapacity(this.parkingFacilities.get(facility.getId()));
		int occupation = this.occupation.get(facility.getId()).intValue();
		
		return capacity - occupation;
	}
}
