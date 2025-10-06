package qzmik;

import java.nio.ByteBuffer;

public class Converter {

    public static Double[] convertByteArrayToDoubleArray(byte[] byteArray, int bufferLength) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);

        Double[] doubleArray = new Double[bufferLength / 8];

        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = byteBuffer.getDouble();
        }

        return doubleArray;
    }

}
