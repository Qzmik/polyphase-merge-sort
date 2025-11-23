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

        blockBuffers[0] = ByteBuffer.allocate(0);
        blockBuffers[1] = ByteBuffer.allocate(0);
        blockBuffers[2] = ByteBuffer.allocate(0);
        clearAllTapes();
        RecordFileGenerator.generateRecordFile(recordAmount);
        saveSortingTapeStateToArchive(2, "archive/initialRecords", false);
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

    public Record lookupRecord(int targetBufferIndex) throws IOException, EOFException {
        if (!blockBuffers[targetBufferIndex].hasRemaining()) {
            readTapeBlockIntoBuffer(targetBufferIndex);
        }
        Record record = new Record(blockBuffers[targetBufferIndex].getDouble(),
                blockBuffers[targetBufferIndex].getDouble());
        blockBuffers[targetBufferIndex].position(blockBuffers[targetBufferIndex].position() - 16);
        return record;
    }

    public boolean hasRemaining(int targetBufferIndex) {
        return blockBuffers[targetBufferIndex].hasRemaining();
    }

    public void undoRecordRead(int targetBufferIndex) {
        blockBuffers[targetBufferIndex].position(blockBuffers[targetBufferIndex].position() - 16);
    }

    public void writeRecord(int targetBufferIndex, Record record) throws IOException {
        if (!blockBuffers[targetBufferIndex].hasRemaining()) {
            writeBufferToTape(targetBufferIndex);
        }

        blockBuffers[targetBufferIndex].putDouble(record.getVoltage());
        blockBuffers[targetBufferIndex].putDouble(record.getCurrent());

    }

    public void setupForNextPhase(int newOutputTapeIndex, int prevOutputTapeIndex) throws IOException {
        sortingTapes[prevOutputTapeIndex].resetTape();
        blockBuffers[prevOutputTapeIndex] = ByteBuffer.allocate(0);
        sortingTapes[newOutputTapeIndex].clearTape();
        blockBuffers[newOutputTapeIndex] = ByteBuffer.allocate(0);
    }

    public void saveSortingTapeStateToArchive(int tapeID, String archiveName, boolean isOutput)
            throws IOException, EOFException {

        long prevPosition = sortingTapes[tapeID].getPosition();

        if (isOutput)
            sortingTapes[tapeID].resetTape();
        Tape archiveTape = new Tape(3, archiveName);

        blockBuffers[tapeID].mark();

        try {
            if (!isOutput) {
                while (blockBuffers[tapeID].hasRemaining()) {
                    if (blockBuffers[tapeID].getDouble(blockBuffers[tapeID].position()) == 0.0f) {
                        break;
                    }
                    Record record = new Record(blockBuffers[tapeID].getDouble(), blockBuffers[tapeID].getDouble());
                    archiveTape.writeRecordInReadableFormat(record);
                }
                blockBuffers[tapeID].reset();
            }
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

    public void flushBlockBuffersToTapes() throws IOException {

        for (int i = 0; i < 3; i++) {
            int length = 0;
            blockBuffers[i].position(0);
            while (blockBuffers[i].hasRemaining() && blockBuffers[i].getDouble() != 0.0f) {
                length += 8;
            }
            sortingTapes[i].writeBlockOfRecords(Arrays.copyOfRange(blockBuffers[i].array(), 0, length));
            blockBuffers[i].clear();
        }
    }

    public void flushBlockBufferToTape(int targetTapeIndex) throws IOException {
        int length = 0;
        blockBuffers[targetTapeIndex].position(0);
        while (blockBuffers[targetTapeIndex].hasRemaining() && blockBuffers[targetTapeIndex].getDouble() != 0.0f) {
            length += 8;
        }
        sortingTapes[targetTapeIndex]
                .writeBlockOfRecords(Arrays.copyOfRange(blockBuffers[targetTapeIndex].array(), 0, length));
        blockBuffers[targetTapeIndex].clear();
    }

    public void archiveTapes(int phase, int outputTapeIndex) throws IOException, EOFException {
        for (int i = 0; i < 3; i++) {
            if (i == outputTapeIndex) {
                saveSortingTapeStateToArchive(outputTapeIndex, String.format("archive/phase%1$d_tape%2$d", phase, i),
                        true);
            } else
                saveSortingTapeStateToArchive(i, String.format("archive/phase%1$d_tape%2$d", phase, i), false);
        }
    }

    public void archiveTapesPostInitialDistribution(int phase) throws IOException, EOFException {
        for (int i = 0; i < 2; i++) {
            saveSortingTapeStateToArchive(i, String.format("archive/phase%1$d_tape%2$d", phase, i),
                    true);
        }
        saveSortingTapeStateToArchive(2, String.format("archive/phase%1$d_tape%2$d", phase, 2),
                false);
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
        for (int i = 0; i < 3; i++) {
            sortingTapes[i].resetTape();
            blockBuffers[i] = ByteBuffer.allocate(0);
        }
    }

}
