package qzmik;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.javatuples.Pair;

@Getter
@Setter
public class RecordFileGenerator {

    public static void generateRecordFile(int amountOfRecordsToGenerate) {
        BufferedWriter writer = createRecordFile(amountOfRecordsToGenerate);
        populateRecordFile(amountOfRecordsToGenerate, writer);
        closeRecordFile(writer);
    }

    private static BufferedWriter createRecordFile(int amountOfRecordsToGenerate) {
        try {
            return new BufferedWriter(
                    new FileWriter(String.format("../output/records%1$d", amountOfRecordsToGenerate)));
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
    }

    private static void populateRecordFile(int amountOfRecordsToGenerate, BufferedWriter writer) {
        try {
            for (int i = 0; i < amountOfRecordsToGenerate; i++) {
                Pair<Double, Double> randomRecordValues = Record.generateRandomRecordValues();
                writer.write(
                        String.format("%1$f %2$f\n", randomRecordValues.getValue0(), randomRecordValues.getValue1()));
            }
        } catch (IOException e) {
            closeRecordFile(writer);
            throw new Error(e.getMessage());
        }
    }

    private static void closeRecordFile(BufferedWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
    }

}
