package qzmik;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.javatuples.Pair;

@Getter
@Setter
public class RecordFileGenerator {

    public static void generateRandomRecordFile(int amountOfRecordsToGenerate) throws IOException {
        RandomAccessFile fileHook = createRecordFile(amountOfRecordsToGenerate);
        populateRecordFile(amountOfRecordsToGenerate, fileHook);
        closeRecordFile(fileHook);
    }

    public static void populateRecordFileFromFile(String inputFile) throws FileNotFoundException, IOException {
        RandomAccessFile fileHook = new RandomAccessFile("archive/recordsFromFile", "rw");
        File inputFileHook = new File(inputFile);
        Scanner scanner = new Scanner(inputFileHook);
        int doublesRead = 0;
        while (true) {
            try {
                Double voltage = scanner.nextDouble();
                fileHook.writeDouble(voltage);
                doublesRead++;
                Double current = scanner.nextDouble();
                fileHook.writeDouble(current);
                doublesRead++;
            } catch (NoSuchElementException e) {
                scanner.close();
                fileHook.close();
                if (doublesRead % 2 == 1) {
                    throw new IllegalArgumentException("File with unfinished or malformed records");
                }
                break;
            }
        }
    }

    private static RandomAccessFile createRecordFile(int amountOfRecordsToGenerate) {
        try {
            return new RandomAccessFile(
                    String.format("tapes/records%1$d", amountOfRecordsToGenerate), "rw");
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
    }

    private static void populateRecordFile(int amountOfRecordsToGenerate, RandomAccessFile fileHook)
            throws IOException {
        for (int i = 0; i < amountOfRecordsToGenerate; i++) {
            Pair<Double, Double> randomRecordValues = Record.generateRandomRecordValues();
            fileHook.writeDouble(randomRecordValues.getValue0());
            fileHook.writeDouble(randomRecordValues.getValue1());
        }

        closeRecordFile(fileHook);
    }

    private static void closeRecordFile(RandomAccessFile fileHook) throws IOException {
        fileHook.close();
    }

}
