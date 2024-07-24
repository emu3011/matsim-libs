package org.matsim.contrib.parking.parkingsearch.manager.parkingGuidanceSystem;

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
