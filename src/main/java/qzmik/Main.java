package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        TapeManager tapeManager = new TapeManager(String.format("output/records%1$s", "20"), 20);
        Sorter sorter = new Sorter(tapeManager);
        sorter.getAndDistributeRecordsFromInputTape();
        tapeManager.archiveTapes(0);
        // sorter.mergeRunsFromTapes();
        sorter.skipDummyRuns();
        sorter.mergingTapes();
        tapeManager.archiveTapes(1);
    }
}