package qzmik;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        RecordFileGenerator.generateRecordFile(Integer.parseInt(args[0]));
    }
}