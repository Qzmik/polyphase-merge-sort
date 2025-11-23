package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        TapeManager tapeManager = new TapeManager(String.format("output/records%1$s", "3000"), 3000);
        Sorter sorter = new Sorter(tapeManager);
        sorter.getAndDistributeRecordsFromInputTape();
        tapeManager.archiveTapesPostInitialDistribution(0);
        sorter.skipDummyRuns();
        sorter.beginMerging();

    }
}