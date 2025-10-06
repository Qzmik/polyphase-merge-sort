package qzmik;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TapeManager {

    public static final int NUMBER_OF_RECORDS_IN_A_BLOCK = 64;

    private Tape[] sortingTapes; // tapes used for polyphase merge sort
    private Tape displayTape; // tape that will be used to display the state of one of the sorting tapes

    public TapeManager(String inputTapeFileName) throws FileNotFoundException, IOException {
        sortingTapes = new Tape[3];
        sortingTapes[0] = new Tape(0);
        sortingTapes[1] = new Tape(1);
        sortingTapes[2] = new Tape(2, inputTapeFileName);
        displayTape = new Tape(3);
    }

    public void saveSortingTapeStateToDisplayTape(int tapeID) throws IOException, EOFException {

        Double[] tapeStateInDoubleFormat = createDoubleFormatDataFromBytesFromTape(tapeID);
        displayTape.clearTape();

        for (int i = 0; i < tapeStateInDoubleFormat.length; i += 2) {
            displayTape.writeRecordInReadableFormat(
                    new Record(tapeStateInDoubleFormat[i], tapeStateInDoubleFormat[i + 1]));
        }

    }

    private Double[] createDoubleFormatDataFromBytesFromTape(int tapeID) throws IOException, EOFException {
        byte[] recordBuffer = new byte[NUMBER_OF_RECORDS_IN_A_BLOCK * Record.RECORD_SIZE_ON_DISK];
        int numberOfBytesRead = sortingTapes[tapeID].readBlockOfRecordsToBuffer(recordBuffer);
        if (numberOfBytesRead < 0) {
            throw new EOFException("Chosen data has no data on it");
        }
        Double[] tapeStateInDoubleFormat = Converter.convertByteArrayToDoubleArray(recordBuffer, numberOfBytesRead);
        return tapeStateInDoubleFormat;
    }

}
