package qzmik;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Experiment {

    public void runExperiment() throws FileNotFoundException, IOException {
        int testingValues[] = { 64, 100, 500, 1000, 5000, 10000, 25000, 50000, 100000 };

        File experimentDir = new File("experiment");

        experimentDir.mkdir();

        XYSeries empiricBlockOpsValues = new XYSeries("Wartości zaobserwowane");
        XYSeries calculatedBlockOpsValues = new XYSeries("Wartości obliczone");
        XYSeries empiricPhaseCountValues = new XYSeries("Wartości zaobserwowane");
        XYSeries calculatedPhaseCountValues = new XYSeries("Wartości obliczone");

        for (int value : testingValues) {

            int totalBlockOps = 0;
            int totalRuns = 0;
            int totalPhases = 0;

            for (int i = 0; i < 5; i++) {

                File archiveDir = new File("archive");

                for (File file : archiveDir.listFiles()) {
                    file.delete();
                }

                File outputDir = new File("tapes");

                for (File file : outputDir.listFiles()) {
                    file.delete();
                }

                TapeManager tapeManager = new TapeManager(
                        String.format("tapes/records%1$s", Integer.toString(value)), value);
                Sorter sorter = new Sorter(tapeManager, 0);
                int blockOpsData[] = sorter.mergeSort();
                totalBlockOps += blockOpsData[0] + blockOpsData[1];
                totalRuns += blockOpsData[3];
                totalPhases += blockOpsData[2];
            }
            empiricBlockOpsValues.add(value, totalBlockOps / 5.0);
            calculatedBlockOpsValues.add(value, 2 * value * 1.04 * ((Math.log(totalRuns / 5.0) / Math.log(2)) + 1)
                    / TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
            empiricPhaseCountValues.add(value, totalPhases / 5.0);
            calculatedPhaseCountValues.add(value, 1.45 * Math.log(totalRuns / 5.0) / Math.log(2));

            System.out.printf("Record count: %d\n", value);
            System.out.printf("Observed mean count of block operations: %f\n", totalBlockOps / 5.0);
            System.out.printf("Calculated mean count of block operations: %f\n",
                    2 * value * 1.04 * ((Math.log(totalRuns / 5.0) / Math.log(2)) + 1)
                            / TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK);
            System.out.printf("Observed mean count of phases: %f\n", totalPhases / 5.0);
            System.out.printf("Calculated mean count of phases: %f\n", 1.45 * Math.log(totalRuns / 5.0) / Math.log(2));
        }

        XYSeriesCollection datasetBlockOps = new XYSeriesCollection();
        datasetBlockOps.addSeries(calculatedBlockOpsValues);
        datasetBlockOps.addSeries(empiricBlockOpsValues);

        XYSeriesCollection datasetPhaseCount = new XYSeriesCollection();
        datasetPhaseCount.addSeries(calculatedPhaseCountValues);
        datasetPhaseCount.addSeries(empiricPhaseCountValues);

        JFreeChart blockOpsChart = ChartFactory.createXYLineChart(
                String.format("Liczba operacji dyskowych w zależności od liczby rekordów sortowanych przy b = %d",
                        TapeManager.NUMBER_OF_RECORDS_IN_A_BLOCK),
                "Liczba rekordów",
                "Liczba operacji dyskowych",
                datasetBlockOps);

        int chartWidth = 800;
        int chartHeight = 640;

        blockOpsChart.getXYPlot().setDomainAxis(new LogarithmicAxis("Liczba rekordów - logarytmicznie"));
        blockOpsChart.getXYPlot().setRangeAxis(new LogarithmicAxis("Liczba operacji dyskowych - logarytmicznie"));

        File file1 = new File("experiment/blockOpsChart.png");

        ChartUtils.saveChartAsPNG(file1, blockOpsChart, chartWidth, chartHeight);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(blockOpsChart));
        frame.pack();
        frame.setVisible(true);

        JFreeChart phasesCountChart = ChartFactory.createXYLineChart(
                "Liczba faz sortowania w zależności od liczby rekordów sortowanych",
                "Liczba rekordów",
                "Liczba faz sortowania",

                datasetPhaseCount);

        File file2 = new File("experiment/phasesChart.png");
        phasesCountChart.getXYPlot().setDomainAxis(new LogarithmicAxis("Liczba rekordów - logarytmicznie"));
        phasesCountChart.getXYPlot().setRangeAxis(new LogarithmicAxis("Liczba faz sortowania - logarytmicznie"));

        ChartUtils.saveChartAsPNG(file2, phasesCountChart, chartWidth, chartHeight);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(phasesCountChart));
        frame.pack();
        frame.setVisible(true);

    }
}
