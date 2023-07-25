/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * BicycleLegScoring.java                                                  *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author dziemke
 */
class BicycleScoreEventsCreator implements
//		SumScoringFunction.LegScoring, SumScoringFunction.ArbitraryEventScoring
		VehicleEntersTrafficEventHandler, LinkLeaveEventHandler,VehicleLeavesTrafficEventHandler
{
	private static final Logger log = LogManager.getLogger( BicycleScoreEventsCreator.class ) ;
	private final Network network;
	private final EventsManager eventsManager;
	private final AdditionalBicycleLinkScore additionalBicycleLinkScore;

	private final Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();
	private final Map<Id<Vehicle>,Id<Link>> firstLinkIdMap = new LinkedHashMap<>();

	@Inject BicycleScoreEventsCreator( Scenario scenario, EventsManager eventsManager, AdditionalBicycleLinkScore additionalBicycleLinkScore ) {
		this.eventsManager = eventsManager;
		this.network = scenario.getNetwork();
		this.additionalBicycleLinkScore = additionalBicycleLinkScore;
	}

	@Override public void reset( int iteration ){
		vehicle2driver.reset( iteration );
	}
	@Override public void handleEvent( VehicleEntersTrafficEvent event ){
		vehicle2driver.handleEvent( event );
		// ---
		this.firstLinkIdMap.put( event.getVehicleId(), event.getLinkId() );
	}
	@Override public void handleEvent( LinkLeaveEvent event ){
		if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			// can be null on first link (why?)

			double amount = additionalBicycleLinkScore.computeLinkBasedScore( network.getLinks().get( event.getLinkId() ) );

			final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
			Gbl.assertNotNull( driverOfVehicle );
			this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
		}
	}
	@Override public void handleEvent( VehicleLeavesTrafficEvent event ){
		vehicle2driver.handleEvent( event );
		// ---
		if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			if( !Objects.equals( this.firstLinkIdMap.get( event.getVehicleId() ), event.getLinkId() ) ){

				double amount = additionalBicycleLinkScore.computeLinkBasedScore( network.getLinks().get( event.getLinkId() ) );

				final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
				Gbl.assertNotNull( driverOfVehicle );
				this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
			}
		}
	}

}
