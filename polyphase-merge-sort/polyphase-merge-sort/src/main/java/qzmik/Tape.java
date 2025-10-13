package qzmik;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.javatuples.Pair;

// class responsible for giving access to a disk file for sorting and
// distribution purposes
public class Tape {

    private RandomAccessFile fileHook;

    public Tape(int tapeID) throws IOException {
        fileHook = new RandomAccessFile(String.format("../output/tape%1$d", tapeID), "rw");
    }

    // initial record file can be used as a tape after initial distribution
    public Tape(int tapeID, String initialRecordFileName) throws FileNotFoundException {
        fileHook = new RandomAccessFile(initialRecordFileName, "rw");
    }

    public Pair<Double, Double> readRecord() throws IOException, EOFException {
        Pair<Double, Double> recordData = new Pair<Double, Double>(fileHook.readDouble(), fileHook.readDouble());
        return recordData;
    }

    public void writeRecord(Record record) throws IOException {
        fileHook.writeDouble(record.getVoltage());
        fileHook.writeDouble(record.getCurrent());
    }

    public void writeRecordInReadableFormat(Record record) throws IOException {
        fileHook.writeChars(
                String.format("%1$.7f %2$.7f %3$.7f\n", record.getVoltage(), record.getCurrent(), record.getPower()));
    }

    public int readBlockOfRecordsToBuffer(byte[] buffer) throws IOException {
        return fileHook.read(buffer);
    }

    public void writeBlockOfRecords(byte[] blockOfRecords) throws IOException {
        fileHook.write(blockOfRecords);
    }

    public void resetTape() throws IOException {
        fileHook.seek(0);
    }

    public void setPosition(long pos) throws IOException {
        fileHook.seek(pos);
    }

    public long getPosition() throws IOException {
        return fileHook.getFilePointer();
    }

    public void clearTape() throws IOException {
        fileHook.setLength(0);
    }

    public void closeTape() throws IOException {
        clearTape();
        fileHook.close();
    }

}
