package qzmik;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TapeManager {

    public static final int NUMBER_OF_RECORDS_IN_A_BLOCK = 64;

    private Tape[] sortingTapes; // tapes used for polyphase merge sort
    private Tape displayTape; // tape that will be used to display the state of one of the sorting tapes
    private int inputTapeID = 2;

    public TapeManager(String inputTapeFileName, int recordAmount) throws FileNotFoundException, IOException {
        sortingTapes = new Tape[3];
        sortingTapes[0] = new Tape(0);
        sortingTapes[1] = new Tape(1);
        sortingTapes[2] = new Tape(inputTapeID, inputTapeFileName);
        displayTape = new Tape(3);
        clearAllTapes();
        RecordFileGenerator.generateRecordFile(recordAmount);
    }

    public void saveSortingTapeStateToDisplayTape(int tapeID) throws IOException, EOFException {

        displayTape.clearTape();
        long prevPosition = sortingTapes[tapeID].getPosition();
        sortingTapes[tapeID].setPosition(0);

        try {
            while (true) {
                double[] tapeStateInDoubleFormat = readRecordBlockFromTapeInDoubleFormat(tapeID);

                for (int i = 0; i < tapeStateInDoubleFormat.length; i += 2) {
                    displayTape.writeRecordInReadableFormat(
                            new Record(tapeStateInDoubleFormat[i], tapeStateInDoubleFormat[i + 1]));
                }
            }
        } catch (EOFException e) {
            sortingTapes[tapeID].setPosition(prevPosition);
            return;
        }

    }

    public double[] readRecordBlockFromInputTapeInDoubleFormat() throws IOException, EOFException {
        return readRecordBlockFromTapeInDoubleFormat(inputTapeID);
    }

    public byte[] readRecordBlockFromInputTapeInBytes() throws IOException, EOFException {
        return readRecordBlockFromTapeInBytes(inputTapeID);
    }

    public byte[] readRecordBlockFromTapeInBytes(int tapeID) throws IOException, EOFException {
        byte[] recordBuffer = new byte[NUMBER_OF_RECORDS_IN_A_BLOCK * Record.RECORD_SIZE_ON_DISK];
        int numberOfBytesRead = sortingTapes[tapeID].readBlockOfRecordsToBuffer(recordBuffer);
        if (numberOfBytesRead < 0) {
            throw new EOFException("Chosen tape has no more data on it");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(recordBuffer, 0, numberOfBytesRead);
        return byteBuffer.array();
    }

    public double[] readRecordBlockFromTapeInDoubleFormat(int tapeID) throws IOException, EOFException {

        byte[] recordBuffer = new byte[NUMBER_OF_RECORDS_IN_A_BLOCK * Record.RECORD_SIZE_ON_DISK];
        int numberOfBytesRead = sortingTapes[tapeID].readBlockOfRecordsToBuffer(recordBuffer);
        if (numberOfBytesRead < 0) {
            throw new EOFException("Chosen tape has no more data on it");
        }
        double[] tapeStateInDoubleFormat = Converter.convertByteArrayToDoubleArray(recordBuffer, numberOfBytesRead);
        return tapeStateInDoubleFormat;
    }

    public void writeRecordBlockToTapeInBytes(byte[] recordBlock, int tapeID) throws IOException, EOFException {
        sortingTapes[tapeID].writeBlockOfRecords(recordBlock);
    }

    public void closeAllTapes() throws IOException {
        for (Tape tape : sortingTapes) {
            tape.closeTape();
        }
        displayTape.closeTape();
    }

    public void clearAllTapes() throws IOException {
        for (Tape tape : sortingTapes) {
            tape.clearTape();
        }
        displayTape.clearTape();
    }

}
