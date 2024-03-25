package com.willswill.qrtunnel.gui;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.willswill.qrtunnel.core.DecodeException;
import lombok.AllArgsConstructor;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Will
 */
public class GetCodeCoordinates {

    private static final Map<DecodeHintType, Object> DECODE_MAP = new HashMap<>();

    static {
        DECODE_MAP.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    public static Layout detect(BufferedImage image) throws ReaderException, DecodeException {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "ISO-8859-1");
        hints.put(DecodeHintType.TRY_HARDER, "true");

        final float scale = 0.2f;

        final Result[] results = new QRCodeMultiReader().decodeMultiple(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))), DECODE_MAP);
        Result result0 = results[0];
        int left = image.getWidth();
        int top = image.getHeight();
        int right = 0;
        int bottom = 0;
        for (Result result : results) {
            int curleft = image.getWidth();
            int curTop = image.getHeight();
            int curRight = 0;
            int curBottom = 0;
            for (ResultPoint point : result.getResultPoints()) {
                if (point instanceof FinderPattern) {
                    FinderPattern fp = (FinderPattern) point;
                    curleft = Math.min(curleft, (int) Math.round(point.getX() - 3.5 * fp.getEstimatedModuleSize()));
                    curTop = Math.min(curTop, (int) Math.round(point.getY() - 3.5 * fp.getEstimatedModuleSize()));
                    curRight = Math.max(curRight, (int) Math.round(point.getX() + 3.5 * fp.getEstimatedModuleSize()));
                    curBottom = Math.max(curBottom, (int) Math.round(point.getY() + 3.5 * fp.getEstimatedModuleSize()));
                }
            }
            left = Math.min(left, curleft - (int) ((curRight - curleft) * scale));
            top = Math.min(top, curTop - (int) ((curBottom - curTop) * scale));
            right = Math.max(right, curRight + (int) ((curRight - curleft) * scale));
            bottom = Math.max(bottom, curBottom + (int) ((curBottom - curTop) * scale));
        }


        int width = right - left;
        int height = bottom - top;

        String[] split = result0.getText().split("/");
        // int num = Integer.parseInt(split[0]);
        String[] split1 = split[1].split("\\*");

        int rows = Integer.parseInt(split1[0]);
        int cols = Integer.parseInt(split1[1]);

        // check
        if (right - left > image.getWidth() || bottom - top > image.getHeight()) {
            throw new DecodeException("Capture rect is out of screen");
        }
        return new Layout(left, top, width, height, rows, cols);
    }

    @AllArgsConstructor
    public static class Layout {
        public int left;
        public int top;
        public int width;
        public int height;
        public int rows;
        public int cols;
    }
}
