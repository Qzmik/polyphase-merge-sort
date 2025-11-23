package qzmik;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PMSDemonstration {

    public static void main(String[] args) throws IOException {

        File archiveDir = new File("archive");

        archiveDir.mkdir();

        for (File file : archiveDir.listFiles()) {
            file.delete();
        }

        File tapesDir = new File("tapes");

        tapesDir.mkdir();

        for (File file : tapesDir.listFiles()) {
            file.delete();
        }

        System.out.printf("Polyphase fibonacci merge\n");
        System.out.printf("Mikolaj Kuzmicz 198291\n");

        System.out.printf("Press e to run an experiment\n");
        System.out.printf("Press r to run a single sort with random records\n");
        System.out.printf("Press m to run a single sort with manually given records\n");
        System.out.printf("Press l to run a single sort with records from file\n");
        System.out.printf("Press q to quit\n");

        Scanner scanner = new Scanner(System.in);

        char scannedOption = 'z';
        while (scannedOption != 'q') {
            scannedOption = scanner.next().charAt(0);
            switch (scannedOption) {
                case 'r':
                    randomRecordsMerge();
                    scannedOption = 'q';
                    break;
                case 'm':
                    manualRecordsMerge();
                    scannedOption = 'q';
                    break;
                case 'e':
                    // TODO: run an experiment with plotting data and returning to a file
                    Experiment exp = new Experiment();
                    exp.runExperiment();
                    scannedOption = 'q';
                    break;
                case 'l':
                    loadingFromFileMerge();
                    scannedOption = 'q';
                    break;
            }
        }

        scanner.close();
    }

    private static void randomRecordsMerge() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Type in the number of records: ");
        int numberOfRecords = scanner.nextInt();
        TapeManager tapeManager = new TapeManager(
                String.format("tapes/records%1$s", Integer.toString(numberOfRecords)), numberOfRecords);
        Sorter sorter = new Sorter(tapeManager, 1);
        sorter.mergeSort();
        scanner.close();
        return;
    }

    private static void loadingFromFileMerge() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Type in the name of the file: ");
        String fileName = scanner.next();
        TapeManager tapeManager;
        try {
            tapeManager = new TapeManager(fileName);
        } catch (Exception e) {
            System.out.printf("INVALID FILE, ABORTING\n");
            System.out.println(e.getMessage());
            scanner.close();
            return;
        }
        Sorter sorter = new Sorter(tapeManager, 1);
        sorter.mergeSort();
        scanner.close();
        return;
    }

    private static void manualRecordsMerge() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Type in the number of records to load ");
        int numberOfRecords = scanner.nextInt();
        System.out.printf(
                "Type in the records you want to sort \n for example: 2.5 0.4 4.5 1.0 - that's 2 following records\n");
        System.out.printf("Both fields are limited from 0 to 100 (0 excluded, 100 included)\n");
        System.out.printf("A record consists of 2 double values, representing voltage and current\n");
        TapeManager tapeManager;

        File manualRecordsFile = new File("archive/manualRecords");
        FileWriter fw = new FileWriter(manualRecordsFile);
        for (int i = 0; i < numberOfRecords; i++) {
            Double voltage = scanner.nextDouble();
            Double current = scanner.nextDouble();
            fw.write(String.format("%1$.7f %2$.7f\n", voltage, current));
        }
        fw.close();
        scanner.close();

        tapeManager = new TapeManager("archive/manualRecords");

        Sorter sorter = new Sorter(tapeManager, 1);
        sorter.mergeSort();
        return;
    }
}