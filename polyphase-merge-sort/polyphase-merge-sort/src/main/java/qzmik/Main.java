package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        TapeManager tapeManager = new TapeManager(String.format("../output/records%1$s", args[0]),
                Integer.parseInt(args[0]));
        Sorter sorter = new Sorter(tapeManager);
        sorter.getAndDistributeRecordsFromInputTape();
        tapeManager.saveSortingTapeStateToDisplayTape(0);
        sorter.mergeRunsFromTapes();
    }
}