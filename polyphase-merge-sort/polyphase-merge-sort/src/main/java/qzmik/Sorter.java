package qzmik;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;

public class Sorter {
    private TapeManager tapeManager;
    private int[] fibonacciBuffer = { 0, 1 };
    private ByteBuffer[] blockBuffers;
    private int[] runCounter = { 0, 0, 0 };
    private int[] dummyRuns = { 0, 0 };
    private int[] targetTapes = { 0, 1 };
    private int outputTapeIndex = 2;
    private int suppressingFib = 1;
    DoubleBuffer tapesRecordBlockInDoubleBuffer[] = { DoubleBuffer.allocate(0), DoubleBuffer.allocate(0) };

    public Sorter(TapeManager tapeManagerToSet) {
        blockBuffers = new ByteBuffer[3];
        tapeManager = tapeManagerToSet;
        blockBuffers[0] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[1] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[2] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
    }

    public void mergeRunsFromTapes() throws IOException, EOFException {

        fibonacciBuffer[1] -= 1;

        skipDummyRuns();

        while (Arrays.stream(runCounter).sum() > 1) {
            try {
                getTapesRecordBlocksIntoBuffers();
            } catch (EOFException e) {
                flushBlockBuffersToTapes();
            }
        }
    }

    private void getTapesRecordBlocksIntoBuffers() throws IOException, EOFException {
        for (int i = 0; i < targetTapes.length; i++) {
            if (!tapesRecordBlockInDoubleBuffer[i].hasRemaining()) {
                tapesRecordBlockInDoubleBuffer[i] = DoubleBuffer
                        .wrap(tapeManager.readRecordBlockFromTapeInDoubleFormat(targetTapes[i]));
            }
        }
    }

    private void mergeSortRecordsFromTapes() {

    }

    private void skipDummyRuns() throws IOException, EOFException {

        while (true) {

            if (dummyRuns[0] + dummyRuns[1] == 0) {
                break;
            }

            getTapesRecordBlocksIntoBuffers();

            if (dummyRuns[0] > 0) {
                Record currRecord = new Record(tapesRecordBlockInDoubleBuffer[1].get(),
                        tapesRecordBlockInDoubleBuffer[1].get());
                putRecordIntoBlockBuffer(currRecord, outputTapeIndex);
                while (tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
                    putRunFromInputBufferIntoTargetBuffer(tapesRecordBlockInDoubleBuffer[1], outputTapeIndex,
                            currRecord);
                }
                if (!tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
                    runCounter[1]--;
                    dummyRuns[0]--;
                }
            }

            while (dummyRuns[1] > 0) {
                Record currRecord = new Record(tapesRecordBlockInDoubleBuffer[0].get(),
                        tapesRecordBlockInDoubleBuffer[0].get());
                putRecordIntoBlockBuffer(currRecord, outputTapeIndex);
                while (tapesRecordBlockInDoubleBuffer[0].hasRemaining()) {
                    putRunFromInputBufferIntoTargetBuffer(tapesRecordBlockInDoubleBuffer[0], outputTapeIndex,
                            currRecord);
                }
                if (!tapesRecordBlockInDoubleBuffer[0].hasRemaining()) {
                    runCounter[0]--;
                    dummyRuns[1]--;
                }
            }
        }
    }

    public void getAndDistributeRecordsFromInputTape() throws IOException, EOFException {
        int targetBuffer = 0;
        suppressingFib = 1;
        DoubleBuffer inputTapeRecordBlockInDoubleBuffer = DoubleBuffer.allocate(0);
        while (true) {
            if (inputTapeRecordBlockInDoubleBuffer.position() == inputTapeRecordBlockInDoubleBuffer.limit()) {
                try {
                    inputTapeRecordBlockInDoubleBuffer = DoubleBuffer
                            .wrap(tapeManager.readRecordBlockFromInputTapeInDoubleFormat());
                } catch (EOFException e) {
                    dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                    System.out.printf("%d %d\n", runCounter[0], runCounter[1]);
                    flushBlockBuffersToTapes();
                    return;
                }
            }
            increaseFibonacciNumber();
            while (inputTapeRecordBlockInDoubleBuffer.position() != inputTapeRecordBlockInDoubleBuffer.limit()) {
                if (runCounter[targetBuffer] == fibonacciBuffer[1]) {
                    targetBuffer = targetBuffer == 1 ? 0 : 1;
                    break;
                }
                runCounter[targetBuffer]++;
                distributeRunFromDoubleBufferToByteBuffer(inputTapeRecordBlockInDoubleBuffer, targetBuffer);
            }
        }
    }

    private void increaseFibonacciNumber() {
        if (suppressingFib > 0) {
            suppressingFib--;
        } else {
            int temp = fibonacciBuffer[1];
            fibonacciBuffer[1] = fibonacciBuffer[1] + fibonacciBuffer[0];
            fibonacciBuffer[0] = temp;
        }
    }

    private void distributeRunFromDoubleBufferToByteBuffer(DoubleBuffer inputBuffer, int targetBufferIndex)
            throws IOException, EOFException {
        // first record, checking if run is coalesced
        Record currRecord = new Record(inputBuffer.get(), inputBuffer.get());
        if (blockBuffers[targetBufferIndex].position() != 0) {
            double prevCurrent = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 8);
            double prevVoltage = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 16);

            if (prevCurrent * prevVoltage < currRecord.getPower()) {
                runCounter[targetBufferIndex]--;
            }
        }
        putRecordIntoBlockBuffer(currRecord, targetBufferIndex);
        // inputting further records as part of a run
        putRunFromInputBufferIntoTargetBuffer(inputBuffer, targetBufferIndex, currRecord);
    }

    private void putRunFromInputBufferIntoTargetBuffer(DoubleBuffer inputBuffer, int targetBufferIndex,
            Record currRecord) throws IOException, EOFException {
        while (inputBuffer.position() + 2 < inputBuffer.limit()
                && runCounter[targetBufferIndex] < fibonacciBuffer[1]) {
            Record nextRecord = new Record(inputBuffer.get(), inputBuffer.get());
            if (nextRecord.compareTo(currRecord) == -1) {
                inputBuffer.position(inputBuffer.position() - 2);
                break;
            }
            putRecordIntoBlockBuffer(nextRecord, targetBufferIndex);
            currRecord = nextRecord;
        }
    }

    private void putRecordIntoBlockBuffer(Record record, int targetBufferIndex) throws IOException, EOFException {
        blockBuffers[targetBufferIndex].putDouble(record.getVoltage());
        blockBuffers[targetBufferIndex].putDouble(record.getCurrent());
        sendBlockBufferToTapeIfFull(targetBufferIndex);
    }

    private void sendBlockBufferToTapeIfFull(int targetBufferIndex) throws IOException, EOFException {
        if (blockBuffers[targetBufferIndex].position() == blockBuffers[targetBufferIndex].limit()) {
            tapeManager.writeRecordBlockToTapeInBytes(blockBuffers[targetBufferIndex].array(), targetBufferIndex);
            blockBuffers[targetBufferIndex].clear();
        }
    }

    private void flushBlockBuffersToTapes() throws IOException, EOFException {
        for (int i = 0; i < 3; i++) {
            int length = 0;
            blockBuffers[i].position(0);
            while (blockBuffers[i].getDouble() != 0.0f) {
                length += 8;
            }
            tapeManager.writeRecordBlockToTapeInBytes(Arrays.copyOfRange(blockBuffers[i].array(), 0, length), i);
            blockBuffers[i].clear();
        }
    }
}
