package qzmik;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;

public class Sorter {
    private TapeManager tapeManager;
    private int[] fibonacciBuffer = { 0, 1 };
    private int[] runCounter = { 0, 0, 0 };
    private int[] dummyRuns = { 0, 0 };
    private int[] targetTapes = { 0, 1 };
    private int outputTapeIndex = 2;
    private int suppressingFib = 1;
    DoubleBuffer tapesRecordBlockInDoubleBuffer[] = { DoubleBuffer.allocate(0), DoubleBuffer.allocate(0) };

    public Sorter(TapeManager tapeManagerToSet) {
        tapeManager = tapeManagerToSet;
    }

    /*
     * public void mergeRunsFromTapes() throws IOException, EOFException {
     * 
     * skipDummyRuns();
     * System.out.printf("%d %d\n", dummyRuns[0], dummyRuns[1]);
     * System.out.printf("%d %d %d\n", runCounter[0], runCounter[1], runCounter[2]);
     * 
     * /*
     * while (Arrays.stream(runCounter).sum() > 1) {
     * try {
     * getTapesRecordBlocksIntoBuffers();
     * } catch (EOFException e) {
     * flushBlockBuffersToTapes();
     * }
     * }
     */

    /*
     * private void performPhase() throws IOException {
     * 
     * while (runCounter[targetTapes[0]] != 0 || runCounter[targetTapes[1]] != 0) {
     * 
     * try {
     * getTapesRecordBlocksIntoBuffer(targetTapes[0]);
     * getTapesRecordBlocksIntoBuffer(targetTapes[1]);
     * } catch (EOFException e) {
     * // TODO: dumping the rest of the target into output
     * }
     * 
     * }
     * }
     * 
     * private void skipDummyRuns() throws IOException {
     * tapeManager.resetAllTapes();
     * 
     * while (dummyRuns[0] + dummyRuns[1] != 0) {
     * 
     * // we can ignore EOF (it's not gonna happen) cause we are not gonna run out
     * of
     * // records for dummy runs
     * try {
     * getTapesRecordBlocksIntoBuffer(0);
     * getTapesRecordBlocksIntoBuffer(1);
     * } catch (EOFException e) {
     * 
     * }
     * 
     * while (dummyRuns[0] > 0) {
     * if (!tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
     * break;
     * }
     * runCounter[targetTapes[1]]--;
     * runCounter[outputTapeIndex]++;
     * dummyRuns[0]--;
     * distributeRunFromDoubleBufferToByteBufferForDummyRuns(1,
     * outputTapeIndex);
     * }
     * 
     * while (dummyRuns[1] > 0) {
     * 
     * if (!tapesRecordBlockInDoubleBuffer[0].hasRemaining()) {
     * break;
     * }
     * runCounter[targetTapes[0]]--;
     * runCounter[outputTapeIndex]++;
     * dummyRuns[1]--;
     * distributeRunFromDoubleBufferToByteBufferForDummyRuns(0,
     * outputTapeIndex);
     * }
     * }
     * }
     */

    public void getAndDistributeRecordsFromInputTape() throws IOException, EOFException {
        int targetBuffer = 0;
        suppressingFib = 0;

        // if program throws exception here, there are no record on input tape
        Record prevRecord = tapeManager.readRecord(tapeManager.inputTapeID);
        tapeManager.writeRecord(targetBuffer, prevRecord);
        runCounter[targetBuffer]++;
        Record currRecord = prevRecord;

        while (true) {
            System.out.printf("%d %d %d\n", runCounter[0], runCounter[1], fibonacciBuffer[1]);

            try {
                currRecord = tapeManager.readRecord(tapeManager.inputTapeID);
                System.out.printf("%f %f %f\n", currRecord.getVoltage(), currRecord.getCurrent(),
                        currRecord.getPower());
            } catch (EOFException e) {
                if (fibonacciBuffer[1] - runCounter[targetBuffer] != fibonacciBuffer[0])
                    dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                tapeManager.flushBlockBuffersToTapes();
                System.out.printf("%d %d\n", runCounter[0], runCounter[1]);
                System.out.printf("%d %d\n", dummyRuns[0], dummyRuns[1]);
                return;
            }

            if (currRecord.compareTo(prevRecord) == -1) {
                if (runCounter[targetBuffer] == fibonacciBuffer[1]) {

                    increaseFibonacciNumber();
                    targetBuffer = targetBuffer == 1 ? 0 : 1;
                    System.out.printf("Swapping to targetBuffer :%d\n", targetBuffer);
                }
                runCounter[targetBuffer]++;
            }

            tapeManager.writeRecord(targetBuffer, currRecord);
            prevRecord = currRecord;
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

    /*
     * private void distributeRunFromDoubleBufferToByteBufferForDummyRuns(int
     * inputBufferIndex,
     * int targetBufferIndex)
     * throws IOException, EOFException {
     * 
     * Record currRecord = new
     * Record(tapesRecordBlockInDoubleBuffer[inputBufferIndex].get(),
     * tapesRecordBlockInDoubleBuffer[inputBufferIndex].get());
     * 
     * // checking record is a continuation of previous run
     * if (blockBuffers[targetBufferIndex].position() != 0) {
     * double prevCurrent = blockBuffers[targetBufferIndex]
     * .getDouble(blockBuffers[targetBufferIndex].position() - 8);
     * double prevVoltage = blockBuffers[targetBufferIndex]
     * .getDouble(blockBuffers[targetBufferIndex].position() - 16);
     * 
     * if (prevCurrent * prevVoltage < currRecord.getPower()) {
     * dummyRuns[inputBufferIndex == 0 ? 1 : 0]++;
     * runCounter[targetTapes[inputBufferIndex]]++;
     * runCounter[outputTapeIndex]--;
     * }
     * }
     * 
     * putRecordIntoBlockBuffer(currRecord, targetBufferIndex);
     * // inputting further records as part of a run
     * putRunFromInputBufferIntoTargetBuffer(tapesRecordBlockInDoubleBuffer[
     * inputBufferIndex], targetBufferIndex,
     * currRecord);
     * }
     *
     * 
     * private void distributeRunsFromDoubleBuffersToByteBufferForPhases(int
     * targetBufferIndex) throws IOException {
     * 
     * Record leftRecord = new Record(tapesRecordBlockInDoubleBuffer[0].get(),
     * tapesRecordBlockInDoubleBuffer[0].get());
     * Record rightRecord = new Record(tapesRecordBlockInDoubleBuffer[1].get(),
     * tapesRecordBlockInDoubleBuffer[1].get());
     * 
     * if (blockBuffers[targetBufferIndex].position() != 0) {
     * double prevCurrent = blockBuffers[targetBufferIndex]
     * .getDouble(blockBuffers[targetBufferIndex].position() - 8);
     * double prevVoltage = blockBuffers[targetBufferIndex]
     * .getDouble(blockBuffers[targetBufferIndex].position() - 16);
     * 
     * if (prevCurrent * prevVoltage < leftRecord.getPower()
     * || prevCurrent * prevVoltage < rightRecord.getPower()) {
     * runCounter[targetBufferIndex]--;
     * }
     * }
     * 
     * if (leftRecord.compareTo(rightRecord) == -1) {
     * putRecordIntoBlockBuffer(leftRecord, targetBufferIndex);
     * putRecordIntoBlockBuffer(rightRecord, targetBufferIndex);
     * } else {
     * putRecordIntoBlockBuffer(rightRecord, targetBufferIndex);
     * putRecordIntoBlockBuffer(leftRecord, targetBufferIndex);
     * }
     * 
     * while (tapesRecordBlockInDoubleBuffer[0].hasRemaining() &&
     * tapesRecordBlockInDoubleBuffer[1].hasRemaining()) {
     * Record nextLeftRecord = new Record(tapesRecordBlockInDoubleBuffer[0].get(),
     * tapesRecordBlockInDoubleBuffer[0].get());
     * Record nextRightRecord = new Record(tapesRecordBlockInDoubleBuffer[1].get(),
     * tapesRecordBlockInDoubleBuffer[1].get());
     * 
     * if (nextLeftRecord.compareTo(leftRecord) == -1) {
     * tapesRecordBlockInDoubleBuffer[0].position(tapesRecordBlockInDoubleBuffer[0].
     * position() - 2);
     * // TODO: dump the other block
     * }
     * if (nextRightRecord.compareTo(rightRecord) == -1) {
     * tapesRecordBlockInDoubleBuffer[0].position(tapesRecordBlockInDoubleBuffer[0].
     * position() - 2);
     * // TODO: dump the other block
     * }
     * if (nextLeftRecord.compareTo(nextRightRecord) == -1) {
     * putRecordIntoBlockBuffer(nextLeftRecord, targetBufferIndex);
     * putRecordIntoBlockBuffer(nextRightRecord, targetBufferIndex);
     * } else {
     * putRecordIntoBlockBuffer(nextRightRecord, targetBufferIndex);
     * putRecordIntoBlockBuffer(nextLeftRecord, targetBufferIndex);
     * }
     * leftRecord = nextLeftRecord;
     * rightRecord = nextRightRecord;
     * }
     * }
     */

}