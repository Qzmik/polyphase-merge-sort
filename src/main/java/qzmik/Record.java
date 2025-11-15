package qzmik;

import java.util.Random;

import org.javatuples.Pair;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Record {

    private Double voltage;
    private Double current;
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 100;
    public static final int RECORD_SIZE_ON_DISK = 16;

    public Record() {
        Pair<Double, Double> randomRecordValues = generateRandomRecordValues();
        voltage = randomRecordValues.getValue0();
        current = randomRecordValues.getValue1();
    }

    public static Pair<Double, Double> generateRandomRecordValues() {
        Random r = new Random();
        return new Pair<Double, Double>(r.nextDouble() * (MAX_RANGE - MIN_RANGE) + MIN_RANGE,
                r.nextDouble() * (MAX_RANGE - MIN_RANGE) + MIN_RANGE);
    }

    public Record(double voltageToSet, double currentToSet) {
        voltage = voltageToSet;
        current = currentToSet;
    }

    public static Record getZeroRecord() {
        return new Record(0.0f, 0.0f);
    }

    public Double getPower() {
        return voltage * current;
    }

    public int compareTo(Record other) {
        if (getPower() < other.getPower())
            return -1;
        if (getPower() == other.getPower())
            return 0;
        else
            return 1;
    }
}
