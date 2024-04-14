package com.willswill.qrtunnel;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.willswill.qrtunnel.core.AppConfigs;
import com.willswill.qrtunnel.core.Decoder;
import com.willswill.qrtunnel.core.Encoder;
import com.willswill.qrtunnel.core.EncoderCallback;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Will
 */
public class Test {
    public static void main(String[] args) {
        test2();
    }

    public static void test2() {
        AppConfigs appConfigs = new AppConfigs();
        appConfigs.setSaveDir("Received");
        Decoder decoder = new Decoder(appConfigs, null);

        Encoder encoder = new Encoder(appConfigs, new EncoderCallback() {
            @Override
            public void imageCreated(int num, BitMatrix matrix) {
                try {
                    BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
                    decoder.decode(image, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        File file = new File("D:\\Temp\\1.png");
        try {
            encoder.encode(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
