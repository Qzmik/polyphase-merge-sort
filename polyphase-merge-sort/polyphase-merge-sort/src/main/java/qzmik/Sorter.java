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

        skipDummyRuns();
        System.out.printf("%d %d\n", dummyRuns[0], dummyRuns[1]);
        System.out.printf("%d %d %d\n", runCounter[0], runCounter[1], runCounter[2]);

        /*
         * while (Arrays.stream(runCounter).sum() > 1) {
         * try {
         * getTapesRecordBlocksIntoBuffers();
         * } catch (EOFException e) {
         * flushBlockBuffersToTapes();
         * }
         * }
         */
    }

    private void performPhase() throws IOException {

        while (runCounter[targetTapes[0]] != 0 || runCounter[targetTapes[1]] != 0) {

            try {
                getTapesRecordBlocksIntoBuffer(targetTapes[0]);
                getTapesRecordBlocksIntoBuffer(targetTapes[1]);
            } catch (EOFException e) {
                // TODO: dumping the rest of the target into output
            }

        }
    }

    private void getTapesRecordBlocksIntoBuffer(int targetTapeIndex) throws IOException, EOFException {
        if (!tapesRecordBlockInDoubleBuffer[targetTapeIndex].hasRemaining()) {
            tapesRecordBlockInDoubleBuffer[targetTapeIndex] = DoubleBuffer
                    .wrap(tapeManager.readRecordBlockFromTapeInDoubleFormat(targetTapes[targetTapeIndex]));
        }
    }

    private void skipDummyRuns() throws IOException {
        tapeManager.resetAllTapes();

        while (dummyRuns[0] + dummyRuns[1] != 0) {

            // we can ignore EOF (it's not gonna happen) cause we are not gonna run out of
            // records for dummy runs
            try {
                getTapesRecordBlocksIntoBuffer(0);
                getTapesRecordBlocksIntoBuffer(1);
            } catch (EOFException e) {

            }

            while (dummyRuns[0] > 0) {
                if (!tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
                    break;
                }
                runCounter[targetTapes[1]]--;
                runCounter[outputTapeIndex]++;
                dummyRuns[0]--;
                distributeRunFromDoubleBufferToByteBufferForDummyRuns(1,
                        outputTapeIndex);
            }

            while (dummyRuns[1] > 0) {

                if (!tapesRecordBlockInDoubleBuffer[0].hasRemaining()) {
                    break;
                }
                runCounter[targetTapes[0]]--;
                runCounter[outputTapeIndex]++;
                dummyRuns[1]--;
                distributeRunFromDoubleBufferToByteBufferForDummyRuns(0,
                        outputTapeIndex);
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
                    if (fibonacciBuffer[1] - runCounter[targetBuffer] != fibonacciBuffer[0])
                        dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                    System.out.printf("%d %d\n", runCounter[0], runCounter[1]);
                    System.out.printf("%d %d\n", dummyRuns[0], dummyRuns[1]);
                    flushBlockBuffersToTapes();
                    return;
                }
            }

            while (inputTapeRecordBlockInDoubleBuffer.position() != inputTapeRecordBlockInDoubleBuffer.limit()) {
                if (runCounter[targetBuffer] == fibonacciBuffer[1]) {
                    increaseFibonacciNumber();
                    targetBuffer = targetBuffer == 1 ? 0 : 1;
                    break;
                }
                runCounter[targetBuffer]++;
                distributeRunFromDoubleBufferToByteBufferForInitialDistribution(inputTapeRecordBlockInDoubleBuffer,
                        targetBuffer);
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

    private void distributeRunFromDoubleBufferToByteBufferForInitialDistribution(DoubleBuffer inputBuffer,
            int targetBufferIndex)
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

    private void distributeRunFromDoubleBufferToByteBufferForDummyRuns(int inputBufferIndex,
            int targetBufferIndex)
            throws IOException, EOFException {

        Record currRecord = new Record(tapesRecordBlockInDoubleBuffer[inputBufferIndex].get(),
                tapesRecordBlockInDoubleBuffer[inputBufferIndex].get());

        // checking record is a continuation of previous run
        if (blockBuffers[targetBufferIndex].position() != 0) {
            double prevCurrent = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 8);
            double prevVoltage = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 16);

            if (prevCurrent * prevVoltage < currRecord.getPower()) {
                dummyRuns[inputBufferIndex == 0 ? 1 : 0]++;
                runCounter[targetTapes[inputBufferIndex]]++;
                runCounter[outputTapeIndex]--;
            }
        }

        putRecordIntoBlockBuffer(currRecord, targetBufferIndex);
        // inputting further records as part of a run
        putRunFromInputBufferIntoTargetBuffer(tapesRecordBlockInDoubleBuffer[inputBufferIndex], targetBufferIndex,
                currRecord);
    }

    private void distributeRunsFromDoubleBuffersToByteBufferForPhases(int targetBufferIndex) throws IOException {

        Record leftRecord = new Record(tapesRecordBlockInDoubleBuffer[0].get(),
                tapesRecordBlockInDoubleBuffer[0].get());
        Record rightRecord = new Record(tapesRecordBlockInDoubleBuffer[1].get(),
                tapesRecordBlockInDoubleBuffer[1].get());

        if (blockBuffers[targetBufferIndex].position() != 0) {
            double prevCurrent = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 8);
            double prevVoltage = blockBuffers[targetBufferIndex]
                    .getDouble(blockBuffers[targetBufferIndex].position() - 16);

            if (prevCurrent * prevVoltage < leftRecord.getPower()
                    || prevCurrent * prevVoltage < rightRecord.getPower()) {
                runCounter[targetBufferIndex]--;
            }
        }

        if (leftRecord.compareTo(rightRecord) == -1) {
            putRecordIntoBlockBuffer(leftRecord, targetBufferIndex);
            putRecordIntoBlockBuffer(rightRecord, targetBufferIndex);
        } else {
            putRecordIntoBlockBuffer(rightRecord, targetBufferIndex);
            putRecordIntoBlockBuffer(leftRecord, targetBufferIndex);
        }

        while (tapesRecordBlockInDoubleBuffer[0].hasRemaining() && tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
            Record nextLeftRecord = new Record(tapesRecordBlockInDoubleBuffer[0].get(),
                    tapesRecordBlockInDoubleBuffer[0].get());
            Record nextRightRecord = new Record(tapesRecordBlockInDoubleBuffer[1].get(),
                    tapesRecordBlockInDoubleBuffer[1].get());

            if (nextLeftRecord.compareTo(leftRecord) == -1) {
                tapesRecordBlockInDoubleBuffer[0].position(tapesRecordBlockInDoubleBuffer[0].position() - 2);
                // TODO: dump the other block
            }
            if (nextRightRecord.compareTo(rightRecord) == -1) {
                tapesRecordBlockInDoubleBuffer[0].position(tapesRecordBlockInDoubleBuffer[0].position() - 2);
                // TODO: dump the other block
            }
            if (nextLeftRecord.compareTo(nextRightRecord) == -1) {
                putRecordIntoBlockBuffer(nextLeftRecord, targetBufferIndex);
                putRecordIntoBlockBuffer(nextRightRecord, targetBufferIndex);
            } else {
                putRecordIntoBlockBuffer(nextRightRecord, targetBufferIndex);
                putRecordIntoBlockBuffer(nextLeftRecord, targetBufferIndex);
            }
            leftRecord = nextLeftRecord;
            rightRecord = nextRightRecord;
        }
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
            while (blockBuffers[i].hasRemaining() && blockBuffers[i].getDouble() != 0.0f) {
                length += 8;
            }
            tapeManager.writeRecordBlockToTapeInBytes(Arrays.copyOfRange(blockBuffers[i].array(), 0, length), i);
            blockBuffers[i].clear();
        }
    }
}
