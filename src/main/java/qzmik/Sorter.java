package qzmik;

import java.io.EOFException;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Scanner;

public class Sorter {
    private TapeManager tapeManager;
    private int[] fibonacciBuffer = { 0, 1 };
    private int[] runCounter = { 0, 0, 0 };
    private int[] dummyRuns = { 0, 0 };
    private int[] targetTapes = { 0, 1 };
    private int outputTapeIndex = 2;
    private int suppressingFib = 1;
    private int finalOutputTape = 0;
    private int mode;
    int phaseCounter = 1;
    DoubleBuffer tapesRecordBlockInDoubleBuffer[] = { DoubleBuffer.allocate(0), DoubleBuffer.allocate(0) };

    public Sorter(TapeManager tapeManagerToSet, int modeToSet) {
        tapeManager = tapeManagerToSet;
        mode = modeToSet;
    }

    public int[] mergeSort() throws IOException {
        getAndDistributeRecordsFromInputTape();
        int runs = runCounter[0] + runCounter[1];
        tapeManager.archiveTapesPostInitialDistribution(0);
        skipDummyRuns();
        beginFurtherPhases();
        tapeManager.closeAllTapes();
        phaseCounter--;
        int blockOpsData[] = tapeManager.getBlockOpsData();
        if (mode == 1) {
            System.out.printf("Block reads: %d\nBlock writes: %d\nNumber of phases: %d\n\n", blockOpsData[0],
                    blockOpsData[1],
                    phaseCounter);
            System.out.printf("Tape containing the output: tape %d\n", finalOutputTape);
        }
        int plotData[] = { blockOpsData[0], blockOpsData[1], phaseCounter, runs };
        return plotData;
    }

    public void beginFurtherPhases() throws IOException {

        Scanner scanner = new Scanner(System.in);
        // each run of a loop corresponds to a post-initial distribution phase
        while (runCounter[0] + runCounter[1] + runCounter[2] > 1) {
            if (mode == 1) {
                scanner.nextLine();
            }
            mergingTapes();
            tapeManager.archiveTapes(phaseCounter, outputTapeIndex);
            int newOutputTapeIndex = runCounter[targetTapes[0]] == 0 ? 0 : 1;
            int temp = outputTapeIndex;
            finalOutputTape = outputTapeIndex;
            outputTapeIndex = targetTapes[newOutputTapeIndex];
            targetTapes[newOutputTapeIndex] = temp;
            phaseCounter++;
            tapeManager.setupForNextPhase(outputTapeIndex, temp);
        }
        scanner.close();
    }

    public void skipDummyRuns() throws IOException, EOFException {
        tapeManager.resetAllTapes();
        Record currRecord = new Record(0, 0);
        Record prevRecord = new Record(0, 0);
        while (dummyRuns[0] + dummyRuns[1] > 0) {

            if (dummyRuns[0] > 0) {
                currRecord = tapeManager.lookupRecord(targetTapes[1]);
                if (currRecord.compareTo(prevRecord) == -1) {
                    runCounter[targetTapes[1]]--;
                    runCounter[outputTapeIndex]++;
                    dummyRuns[0]--;
                }
                if (dummyRuns[0] != 0) {
                    tapeManager.readRecord(targetTapes[1]);
                    tapeManager.writeRecord(outputTapeIndex, currRecord);
                    prevRecord = currRecord;
                }
            }

            if (dummyRuns[1] > 0) {
                currRecord = tapeManager.lookupRecord(targetTapes[0]);
                if (currRecord.compareTo(prevRecord) == -1) {
                    runCounter[targetTapes[0]]--;
                    runCounter[outputTapeIndex]++;
                    dummyRuns[1]--;
                }
                if (dummyRuns[1] != 0) {
                    tapeManager.readRecord(targetTapes[0]);
                    tapeManager.writeRecord(outputTapeIndex, currRecord);
                    prevRecord = currRecord;
                }
            }
        }
        if (mode == 1)
            System.out.printf("Run count: %d %d %d\n", runCounter[0], runCounter[1], runCounter[2]);
    }

    public void mergingTapes() throws IOException {
        Record prevRecordLeft = new Record(0, 0);
        Record prevRecordRight = new Record(0, 0);
        while (runCounter[targetTapes[0]] > 0 && runCounter[targetTapes[1]] > 0) {

            // read from both buffers - if one is empty, then theres only 1 run on 1 tape,
            // and program should've ended
            Record currRecordLeft = tapeManager.lookupRecord(targetTapes[0]);
            Record currRecordRight = tapeManager.lookupRecord(targetTapes[1]);
            // 1st step - until both runs are not fully read
            while (prevRecordLeft.compareTo(currRecordLeft) <= 0 && prevRecordRight.compareTo(currRecordRight) <= 0) {
                if (currRecordLeft.compareTo(currRecordRight) <= 0) {
                    tapeManager.writeRecord(outputTapeIndex, currRecordLeft);
                    tapeManager.readRecord(targetTapes[0]);
                    prevRecordLeft = currRecordLeft;
                    try {
                        currRecordLeft = tapeManager.lookupRecord(targetTapes[0]);
                    } catch (EOFException e) {
                        // this effectively cuts off any attempts for further reading
                        prevRecordLeft = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
                    }
                } else {
                    tapeManager.writeRecord(outputTapeIndex, currRecordRight);
                    tapeManager.readRecord(targetTapes[1]);
                    prevRecordRight = currRecordRight;
                    try {
                        currRecordRight = tapeManager.lookupRecord(targetTapes[1]);
                    } catch (EOFException e) {
                        // this effectively cuts off any attempts for further reading
                        prevRecordRight = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
                    }
                }
            }

            // one of the following while loops will run until the run empties naturally, or
            // the tape does
            while (prevRecordLeft.compareTo(currRecordLeft) <= 0) {
                tapeManager.writeRecord(outputTapeIndex, currRecordLeft);
                tapeManager.readRecord(targetTapes[0]);
                prevRecordLeft = currRecordLeft;
                try {
                    currRecordLeft = tapeManager.lookupRecord(targetTapes[0]);
                } catch (EOFException eLeft) {
                    prevRecordLeft = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
                }
            }

            while (prevRecordRight.compareTo(currRecordRight) <= 0) {
                tapeManager.writeRecord(outputTapeIndex, currRecordRight);
                tapeManager.readRecord(targetTapes[1]);
                prevRecordRight = currRecordRight;
                try {
                    currRecordRight = tapeManager.lookupRecord(targetTapes[1]);
                    // System.out.printf("CONSUMING RIGHT: %f \n", currRecordRight.getPower());
                } catch (EOFException eRight) {
                    prevRecordRight = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
                }
            }
            runCounter[targetTapes[0]]--;
            runCounter[targetTapes[1]]--;
            runCounter[outputTapeIndex]++;
            prevRecordLeft = new Record(0, 0);
            prevRecordRight = new Record(0, 0);
        }
        // end state here should be that one of the tapes in empty, one is partially
        // read (with part of it possibly still in buffer) and one fully written to
        tapeManager.flushBlockBufferToTape(outputTapeIndex);
        if (mode == 1)
            System.out.printf("Run count: %d %d %d\n", runCounter[0], runCounter[1], runCounter[2]);
    }

    public void getAndDistributeRecordsFromInputTape() throws IOException, EOFException {
        int targetBuffer = 0;
        suppressingFib = 0;

        Record prevRecordLeft = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
        Record prevRecordRight = new Record(Record.MAX_RANGE + 1, Record.MAX_RANGE + 1);
        Record currRecord;

        while (true) {

            if (runCounter[targetBuffer] == fibonacciBuffer[1]) {
                increaseFibonacciNumber();
                targetBuffer = targetBuffer == 1 ? 0 : 1;
            }

            try {
                currRecord = tapeManager.readRecord(tapeManager.inputTapeID);
            } catch (EOFException e) {

                if (fibonacciBuffer[1] - runCounter[targetBuffer] != fibonacciBuffer[0])
                    dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                tapeManager.flushBlockBufferToTape(0);
                tapeManager.flushBlockBufferToTape(1);
                tapeManager.resetAllTapes();
                if (mode == 1) {
                    System.out.printf("Run count post-initial distribution: %d %d\n", runCounter[0], runCounter[1]);
                    System.out.printf("Required dummy runs: %d %d\n", dummyRuns[0], dummyRuns[1]);
                }
                return;
            }

            if (targetBuffer == 0) {
                if (currRecord.compareTo(prevRecordLeft) == -1) {
                    runCounter[targetBuffer]++;
                }
                tapeManager.writeRecord(targetBuffer, currRecord);
                prevRecordLeft = currRecord;
                while (true) {
                    try {
                        currRecord = tapeManager.readRecord(tapeManager.inputTapeID);
                    } catch (EOFException e) {

                        if (fibonacciBuffer[1] - runCounter[targetBuffer] != fibonacciBuffer[0])
                            dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                        tapeManager.flushBlockBufferToTape(0);
                        tapeManager.flushBlockBufferToTape(1);
                        if (mode == 1) {
                            System.out.printf("Run count post-initial distribution: %d %d\n", runCounter[0],
                                    runCounter[1]);
                            System.out.printf("Required dummy runs: %d %d\n", dummyRuns[0], dummyRuns[1]);
                        }
                        return;
                    }
                    if (currRecord.compareTo(prevRecordLeft) == -1) {
                        tapeManager.undoRecordRead(tapeManager.inputTapeID);
                        break;
                    }
                    tapeManager.writeRecord(targetBuffer, currRecord);
                    prevRecordLeft = currRecord;
                }
            }

            if (targetBuffer == 1) {
                if (currRecord.compareTo(prevRecordRight) == -1) {
                    runCounter[targetBuffer]++;
                }
                tapeManager.writeRecord(targetBuffer, currRecord);
                prevRecordRight = currRecord;
                while (true) {
                    try {
                        currRecord = tapeManager.readRecord(tapeManager.inputTapeID);
                    } catch (EOFException e) {

                        if (fibonacciBuffer[1] - runCounter[targetBuffer] != fibonacciBuffer[0])
                            dummyRuns[targetBuffer] = fibonacciBuffer[1] - runCounter[targetBuffer];
                        tapeManager.flushBlockBufferToTape(0);
                        tapeManager.flushBlockBufferToTape(1);
                        if (mode == 1) {
                            System.out.printf("Run count post-initial distribution: %d %d\n", runCounter[0],
                                    runCounter[1]);
                            System.out.printf("Required dummy runs: %d %d\n", dummyRuns[0], dummyRuns[1]);
                        }
                        return;
                    }
                    if (currRecord.compareTo(prevRecordRight) == -1) {
                        tapeManager.undoRecordRead(tapeManager.inputTapeID);
                        break;
                    }
                    tapeManager.writeRecord(targetBuffer, currRecord);
                    prevRecordRight = currRecord;
                }
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

}