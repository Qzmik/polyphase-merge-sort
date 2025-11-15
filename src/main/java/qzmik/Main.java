package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        TapeManager tapeManager = new TapeManager(String.format("output/records%1$s", "10"), 10);
        Sorter sorter = new Sorter(tapeManager);
        sorter.getAndDistributeRecordsFromInputTape();
        tapeManager.saveSortingTapeStateToArchive(0, "archive/tape0");
        tapeManager.saveSortingTapeStateToArchive(1, "archive/tape1");
        tapeManager.saveSortingTapeStateToArchive(2, "archive/tape2");
        // sorter.mergeRunsFromTapes();
    }
}