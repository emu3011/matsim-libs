package org.matsim.contrib.parking.parkingsearch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem.PGSConfigurator;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * An example how to run a Parking Guidance System parking search in MATSim.
 * Note that some stuff has to be set in the PGSConfigurator class.
 *
 * @author Emanuel Skodinis (emanuesk@ethz.ch) 
 */

public class RunPGSParkingSearchExample {

	public static void main(String[] args) {
		// step 1: load config file
		Config config = ConfigUtils.loadConfig(PGSConfigurator.configXMLFilePath, new ParkingSearchConfigGroup());

		// step 2: get parking search config group and set parking search strategy to ParkingGuidance
		ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.ParkingGuidance);

        // step 3: set number of iterations
        config.controller().setLastIteration(PGSConfigurator.numIterations - 1);

        // step 4: run the PGS parking search example
		new RunPGSParkingSearchExample().run(config);
	}

	public void run(Config config) {
        // step 1: load the scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config);

        // step 2: create controler from scenario and set the snapshot style
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

        // step 3: install parking modules to controler
		SetupParking.installParkingModules(controler);

        // step 4: run the simulation
		controler.run();
	}

}
