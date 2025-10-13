package qzmik;

import java.nio.ByteBuffer;

public class Converter {

    public static double[] convertByteArrayToDoubleArray(byte[] byteArray, int bufferLength) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);

        double[] doubleArray = new double[bufferLength / 8];

        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = byteBuffer.getDouble();
        }

        return doubleArray;
    }

}
