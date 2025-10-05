package qzmik;

import java.io.EOFException;
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

    public Pair<Double, Double> readRecord() throws IOException, EOFException {
        Pair<Double, Double> recordData = new Pair<Double, Double>(fileHook.readDouble(), fileHook.readDouble());
        return recordData;
    }

    public void writeRecord(Record record) throws IOException {
        fileHook.writeDouble(record.getVoltage());
        fileHook.writeDouble(record.getCurrent());
    }

    public byte[] readBlockOfRecords(int numberOfRecordsToRead) throws IOException, EOFException {
        byte[] buffer = new byte[numberOfRecordsToRead * Record.RECORD_SIZE_ON_DISK];
        fileHook.read(buffer);
        return buffer;
    }

    public void writeBlockOfRecords(byte[] blockOfRecords) throws IOException {
        fileHook.write(blockOfRecords);
    }

    public void clearTape() throws IOException {
        fileHook.setLength(0);
    }

    public void closeTape() throws IOException {
        fileHook.close();
    }

}
