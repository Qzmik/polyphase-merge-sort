package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        RecordFileGenerator.generateRecordFile(Integer.parseInt(args[0]));
        TapeManager tapeManager = new TapeManager("../output/records100");
        tapeManager.saveSortingTapeStateToDisplayTape(2);
    }
}