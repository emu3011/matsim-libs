package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

/**
 * Runs the IdModifier with values specified in the PGSConfigurator.
 * 
 * @author Emanuel Skodinis (emanuesk@ethz.ch)
 */
public class RunIdModifier {
    public static void main(String[] args) {
		IdModifier.modifyPersonIdsToStrategy(PGSConfigurator.sourcePopulationFilePath,
											 PGSConfigurator.resultPopulationFilePath,
											 PGSConfigurator.shareOfPGS,
											 PGSConfigurator.shareOfRandom,
											 PGSConfigurator.shareOfBenenson,
											 PGSConfigurator.shareOfDistanceMemory,
											 PGSConfigurator.shareOfNearestParkingSpot);
		
		IdModifier.modifyFacilityIdsToKnowledge(PGSConfigurator.sourceFacilitiesFilePath,
												PGSConfigurator.resultFacilitiesFilePath,
												PGSConfigurator.shareOfFacilitiesWithSensor);
    }
}
