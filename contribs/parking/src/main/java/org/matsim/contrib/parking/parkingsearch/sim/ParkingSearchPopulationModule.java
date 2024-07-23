package org.matsim.contrib.parking.parkingsearch.sim;

import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.ParkingGuidanceSystem;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

public class ParkingSearchPopulationModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "ParkingSearch";

	@Override
	protected void configureQSim() {
		if (getConfig().transit().isUseTransit()) {
			throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
		}

		bind(AgentFactory.class).to(ParkingAgentFactory.class).asEagerSingleton(); // (**)
		bind(ParkingPopulationAgentSource.class).asEagerSingleton();
		// this bind is added by Emanuel Skodinis (emanuesk@ethz.ch) and is binding the Parking Guidance System
		bind(ParkingGuidanceSystem.class).asEagerSingleton();
		addQSimComponentBinding( COMPONENT_NAME ).to( ParkingPopulationAgentSource.class );
	}

}
