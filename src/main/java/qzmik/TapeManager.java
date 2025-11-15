package qzmik;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TapeManager {

    public static final int NUMBER_OF_RECORDS_IN_A_BLOCK = 64;

    private Tape[] sortingTapes; // tapes used for polyphase merge sort
    public int inputTapeID = 2;
    private ByteBuffer[] blockBuffers;

    public TapeManager(String inputTapeFileName, int recordAmount) throws FileNotFoundException, IOException {

        sortingTapes = new Tape[3];
        sortingTapes[0] = new Tape(0);
        sortingTapes[1] = new Tape(1);
        sortingTapes[2] = new Tape(inputTapeID, inputTapeFileName);

        blockBuffers = new ByteBuffer[3];

        blockBuffers[0] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[1] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[2] = ByteBuffer.allocate(0);
        clearAllTapes();
        RecordFileGenerator.generateRecordFile(recordAmount);
        saveSortingTapeStateToArchive(2, "archive/initialRecords");
    }

    private void readTapeBlockIntoBuffer(int targetTapeIndex) throws IOException, EOFException {
        byte[] recordArray = new byte[NUMBER_OF_RECORDS_IN_A_BLOCK * Record.RECORD_SIZE_ON_DISK];
        int numberOfBytesRead = sortingTapes[targetTapeIndex].readBlockOfRecordsToBuffer(recordArray);
        if (numberOfBytesRead < 0) {
            sortingTapes[targetTapeIndex].clearTape();
            throw new EOFException("Chosen tape has no more data on it");
        }
        blockBuffers[targetTapeIndex] = ByteBuffer.wrap(recordArray, 0, numberOfBytesRead);
    }

    private void writeBufferToTape(int targetTapeIndex) throws IOException {
        sortingTapes[targetTapeIndex].writeBlockOfRecords(blockBuffers[targetTapeIndex].array());
        blockBuffers[targetTapeIndex] = ByteBuffer
                .allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
    }

    public Record readRecord(int targetBufferIndex) throws IOException, EOFException {
        if (!blockBuffers[targetBufferIndex].hasRemaining()) {
            readTapeBlockIntoBuffer(targetBufferIndex);
        }

        return new Record(blockBuffers[targetBufferIndex].getDouble(), blockBuffers[targetBufferIndex].getDouble());
    }

    public void writeRecord(int targetBufferIndex, Record record) throws IOException {
        if (!blockBuffers[targetBufferIndex].hasRemaining()) {
            writeBufferToTape(targetBufferIndex);
        }

        blockBuffers[targetBufferIndex].putDouble(record.getVoltage());
        blockBuffers[targetBufferIndex].putDouble(record.getCurrent());

    }

    public void saveSortingTapeStateToArchive(int tapeID, String archiveName) throws IOException, EOFException {

        long prevPosition = sortingTapes[tapeID].getPosition();
        sortingTapes[tapeID].resetTape();
        Tape archiveTape = new Tape(3, archiveName);
        try {
            while (true) {
                double[] tapeStateInDoubleFormat = readRecordBlockFromTapeInDoubleFormat(tapeID);

                for (int i = 0; i < tapeStateInDoubleFormat.length; i += 2) {
                    Record record = new Record(tapeStateInDoubleFormat[i], tapeStateInDoubleFormat[i + 1]);
                    archiveTape.writeRecordInReadableFormat(record);
                }
            }
        } catch (EOFException e) {
            sortingTapes[tapeID].setPosition(prevPosition);
            archiveTape.closeTape();
            return;
        }

    }

    private double[] readRecordBlockFromTapeInDoubleFormat(int tapeID) throws IOException, EOFException {

        byte[] recordBuffer = new byte[NUMBER_OF_RECORDS_IN_A_BLOCK * Record.RECORD_SIZE_ON_DISK];
        int numberOfBytesRead = sortingTapes[tapeID].readBlockOfRecordsToBuffer(recordBuffer);
        if (numberOfBytesRead < 0) {
            throw new EOFException("Chosen tape has no more data on it");
        }
        double[] tapeStateInDoubleFormat = Converter.convertByteArrayToDoubleArray(recordBuffer, numberOfBytesRead);
        return tapeStateInDoubleFormat;
    }

    public void flushBlockBuffersToTapes() throws IOException, EOFException {

        for (int i = 0; i < 2; i++) {
            int length = 0;
            blockBuffers[i].position(0);
            while (blockBuffers[i].getDouble() != 0.0f) {
                length += 8;
            }
            sortingTapes[i].writeBlockOfRecords(Arrays.copyOfRange(blockBuffers[i].array(), 0, length));
            blockBuffers[i].clear();
        }
    }

    public void closeAllTapes() throws IOException {
        for (Tape tape : sortingTapes) {
            tape.closeTape();
        }
    }

    public void clearAllTapes() throws IOException {
        for (Tape tape : sortingTapes) {
            tape.clearTape();
        }
    }

    public void resetAllTapes() throws IOException {
        for (Tape tape : sortingTapes) {
            tape.resetTape();
        }
    }

}
