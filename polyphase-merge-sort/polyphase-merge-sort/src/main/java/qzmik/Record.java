package qzmik;

import java.util.Random;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Record {

    private Double voltage;
    private Double current;
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 100;

    public Record() {
        Random r = new Random();
        voltage = r.nextDouble() * (MAX_RANGE - MIN_RANGE) + MIN_RANGE;
        current = r.nextDouble() * (MAX_RANGE - MIN_RANGE) + MIN_RANGE;
    }

    public Record(double voltageToSet, double currentToSet) {
        voltage = voltageToSet;
        current = currentToSet;
    }

    public Double getPower() {
        return voltage * current;
    }
}
