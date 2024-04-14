package com.willswill.qrtunnel.gui;

import com.google.zxing.common.BitMatrix;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Will
 */
public class ImageView extends JPanel {
    @Getter
    private final int margin;
    @Getter
    private final int imageWidth;
    @Getter
    private final int imageHeight;
    // private BufferedImage image;
    private BitMatrix matrix;

    public ImageView() {
        this(null, 500, 500, 0);
    }

    public ImageView(BitMatrix matrix, int imageWidth, int imageHeight, int margin) {
        this.matrix = matrix;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.margin = margin;
        setLayout(null);
        setPreferredSize(new Dimension(imageWidth + margin * 2, imageHeight + margin * 2));
    }

    public void setImage(BitMatrix matrix) {
        this.matrix = matrix;
        this.repaint();
    }

    @Override
    public void paintComponent(Graphics g1) {
        if (this.matrix == null) {
            return;
        }
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, matrix.getWidth(), matrix.getHeight());

        g1.setColor(Color.BLACK);
        for (int i = 0; i < matrix.getWidth(); i++) {
            for (int j = 0; j < matrix.getHeight(); j++) {
                if (matrix.get(i, j)) {
                    g1.fillRect(i, j, 1, 1);
                }
            }
        }
        // g1.drawImage(image, margin, margin, image.getWidth(), image.getHeight(), null);
    }
}