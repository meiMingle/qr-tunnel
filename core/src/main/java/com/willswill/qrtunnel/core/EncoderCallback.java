package com.willswill.qrtunnel.core;

import com.google.zxing.common.BitMatrix;

import java.awt.image.BufferedImage;

/**
 * @author Will
 */
public interface EncoderCallback {
    default void imageCreated(int num, BitMatrix matrix) {
    }

    default void fileBegin(FileInfo fileInfo) {
    }

    default void fileEnd(FileInfo fileInfo) {
    }
}
