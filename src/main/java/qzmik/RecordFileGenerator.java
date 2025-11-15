package qzmik;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.javatuples.Pair;

@Getter
@Setter
public class RecordFileGenerator {

    public static void generateRecordFile(int amountOfRecordsToGenerate) throws IOException {
        RandomAccessFile fileHook = createRecordFile(amountOfRecordsToGenerate);
        populateRecordFile(amountOfRecordsToGenerate, fileHook);
        closeRecordFile(fileHook);
    }

    private static RandomAccessFile createRecordFile(int amountOfRecordsToGenerate) {
        try {
            return new RandomAccessFile(
                    String.format("../output/records%1$d", amountOfRecordsToGenerate), "rw");
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
