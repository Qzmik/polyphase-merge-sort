package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        TapeManager tapeManager = new TapeManager(String.format("../output/records%1$s", "50"), 50);
        Sorter sorter = new Sorter(tapeManager);
        tapeManager.saveSortingTapeStateToArchive(2, "initialRecords");
        sorter.getAndDistributeRecordsFromInputTape();

        // sorter.mergeRunsFromTapes();
    }
}