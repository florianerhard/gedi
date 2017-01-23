package gedi.core.region;

public interface MissingInformationIntronInformation {

	GenomicRegion[] getInformationGenomicRegions();
	// between part 0 and 1 is intronIndex 0!
	boolean isMissingInformationIntron(int intronIndex);
	
	boolean isLeftPartMissing();
	boolean isRightPartMissing();
	default boolean isPartMissing() {
		return isLeftPartMissing()||isRightPartMissing();
	}
	
}
