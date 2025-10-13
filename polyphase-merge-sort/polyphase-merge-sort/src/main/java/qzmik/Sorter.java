package qzmik;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

public class Sorter {
    private TapeManager tapeManager;

    private int[] fibonacciBuffer = { 0, 1 };
    private ByteBuffer[] blockBuffers;
    private int[] runCounter = { 0, 0 };
    private int[] dummyRuns = { 0, 0 };

    public Sorter(TapeManager tapeManagerToSet) {
        blockBuffers = new ByteBuffer[3];
        tapeManager = tapeManagerToSet;
        blockBuffers[0] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[1] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
        blockBuffers[2] = ByteBuffer.allocate(Record.RECORD_SIZE_ON_DISK * TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
    }

    /*
     * private void initialDistribution() {
     * try {
     * 
     * } catch (EOFException e) {
     * return;
     * }
     * }
     */

    public void getAndDistributeRecordsFromInputTape() throws IOException, EOFException {
        int targetBuffer = 0;
        int suppressingFib = 1;
        DoubleBuffer inputTapeRecordBlockInDoubleBuffer = DoubleBuffer.allocate(0);
        while (true) {
            if (inputTapeRecordBlockInDoubleBuffer.position() == inputTapeRecordBlockInDoubleBuffer.limit()) {
                try {
                    inputTapeRecordBlockInDoubleBuffer = DoubleBuffer
                            .wrap(tapeManager.readRecordBlockFromInputTapeInDoubleFormat());
                } catch (EOFException e) {
                    System.out.printf("%d %d\n", runCounter[0], runCounter[1]);
                    dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                    flushBlockBuffersToTapes();
                    return;
                }
            }
            if (suppressingFib > 0) {
                suppressingFib--;
            } else {
                int temp = fibonacciBuffer[1];
                fibonacciBuffer[1] = fibonacciBuffer[1] + fibonacciBuffer[0];
                fibonacciBuffer[0] = temp;
            }
            while (inputTapeRecordBlockInDoubleBuffer.position() != inputTapeRecordBlockInDoubleBuffer.limit()) {
                if (runCounter[targetBuffer] == fibonacciBuffer[1]) {
                    targetBuffer = targetBuffer == 1 ? 0 : 1;
                    System.out.printf("%d %d\n", runCounter[0], runCounter[1]);
                    break;
                }
                runCounter[targetBuffer]++;
                distributeRunFromDoubleBufferToByteBuffer(inputTapeRecordBlockInDoubleBuffer, targetBuffer);
            }
        }
    }

    private void distributeRunFromDoubleBufferToByteBuffer(DoubleBuffer inputBuffer, int targetBufferIndex)
            throws IOException, EOFException {
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
        while (inputBuffer.position() + 2 < inputBuffer.limit()
                && runCounter[targetBufferIndex] < fibonacciBuffer[1]) {
            Record nextRecord = new Record(inputBuffer.get(), inputBuffer.get());
            if (nextRecord.getPower() < currRecord.getPower()) {
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
            tapeManager.writeRecordBlockToTapeInBytes(blockBuffers[i].array(), i);
            blockBuffers[i].clear();
        }
    }
}
