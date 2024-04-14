package com.willswill.qrtunnel.gui;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.willswill.qrtunnel.core.DecodeException;
import com.willswill.qrtunnel.core.Decoder;
import com.willswill.qrtunnel.core.DecoderCallback;
import com.willswill.qrtunnel.core.FileInfo;
import lombok.extern.slf4j.Slf4j;
import sun.awt.ComponentFactory;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.image.*;
import java.awt.peer.RobotPeer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Will
 */
@Slf4j
public class ReceiverForm {
    private JPanel panel1;
    private JButton startButton;
    private JTextArea logView;
    private JButton stopButton;
    private JButton senderButton;
    private JProgressBar fileProgress;
    private JLabel filenameLabel;
    private JButton configButton;

    private JFrame frame;
    private Decoder decoder;
    GetCodeCoordinates.Layout layout;
    boolean running = false;
    private final RingBuffer<String> logBuf = new RingBuffer<>(200);

    private static final GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final int MAX_ACCEPTED_VERSION = 25;

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final RobotPeer robotPeer;

    private static final Toolkit toolkit = Toolkit.getDefaultToolkit();

    private int totalImages;
    private int imageIndex;

    static {
        try {
            if (getJavaVersion(System.getProperty("java.version")) < 17) {
                MethodType methodType = MethodType.methodType(RobotPeer.class, Robot.class, GraphicsDevice.class);
                MethodHandle methodHandle = lookup.findVirtual(ComponentFactory.class, "createRobot", methodType).bindTo(toolkit);
                robotPeer = (RobotPeer) methodHandle.invokeExact((Robot) null, localGraphicsEnvironment.getDefaultScreenDevice());
            } else {
                MethodType methodType = MethodType.methodType(RobotPeer.class, GraphicsDevice.class);
                MethodHandle methodHandle = lookup.findVirtual(ComponentFactory.class, "createRobot", methodType).bindTo(toolkit);
                robotPeer = (RobotPeer) methodHandle.invokeExact(localGraphicsEnvironment.getDefaultScreenDevice());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static ReceiverForm create() {
        JFrame frame = new JFrame("ReceiverForm");
        ReceiverForm form = new ReceiverForm();
        frame.setContentPane(form.panel1);
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        form.frame = frame;

        form.initComponents();

        frame.pack();
        frame.setVisible(true);
        return form;
    }

    public void show() {
        frame.setVisible(true);
    }

    void initComponents() {
        startButton.addActionListener(e -> {
            try {
                detectCaptureRect();
            } catch (ReaderException ex) {
                log.error("Failed to detect capture rect!", ex);
                JOptionPane.showMessageDialog(panel1, "Failed to detect capture rect!");
                return;
            } catch (DecodeException ex) {
                JOptionPane.showMessageDialog(panel1, ex.getMessage());
                return;
            } catch (Exception ex) {
                log.error("Failed to capture screenshot!", ex);
                JOptionPane.showMessageDialog(panel1, "Failed to capture screenshot!");
                return;
            }

            startCaptureAsync();
        });

        stopButton.addActionListener(e -> running = false);

        senderButton.addActionListener(e -> Launcher.self.showSenderForm());

        configButton.addActionListener(e -> Launcher.self.showConfigsForm());

        DefaultCaret caret = (DefaultCaret) logView.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    void detectCaptureRect() throws ReaderException, DecodeException {
        Rectangle fullRect = getFullVirtualScreenRect();
        BufferedImage image = screenshot(fullRect);
        layout = GetCodeCoordinates.detect(image);
        log.info(layout.toString());
        Launcher.log("Capture rect  is set to " + layout.left + "," + layout.top + " " + layout.width + "*" + layout.height);
    }

    public static Rectangle getFullVirtualScreenRect() {
        GraphicsDevice[] screenDevices = localGraphicsEnvironment.getScreenDevices();
        Rectangle allBounds = new Rectangle();
        for (int i = 0; i < screenDevices.length; i++) {
            GraphicsDevice screenDevice = screenDevices[i];
            GraphicsConfiguration gc = screenDevice.getDefaultConfiguration();
            // 获取 GraphicsConfiguration 的 Bounds，它包含更高分辨率信息
            Rectangle screenBounds = gc.getBounds();
            if (System.getProperty("os.name").startsWith("Windows")) {
                screenBounds.x = screenBounds.x * screenDevice.getDisplayMode().getWidth() / screenBounds.width;
                screenBounds.y = screenBounds.y * screenDevice.getDisplayMode().getHeight() / screenBounds.height;
                screenBounds.height = screenDevice.getDisplayMode().getHeight();
                screenBounds.width = screenDevice.getDisplayMode().getWidth();
                screenBounds.height = screenDevice.getDisplayMode().getHeight();
            }
            allBounds.add(screenBounds);
        }
        return allBounds;
    }

    public static int getJavaVersion(String versionString) throws IllegalArgumentException {
        // trimming
        String str = versionString.trim();
        Map<String, String> trimmingMap = new HashMap<>(); // "substring to detect" to "substring from which to trim"
        trimmingMap.put("Runtime Environment", "(build ");
        trimmingMap.put("OpenJ9", "version ");
        trimmingMap.put("GraalVM", "Java ");
        for (String keyToDetect : trimmingMap.keySet()) {
            if (str.contains(keyToDetect)) {
                int p = str.indexOf(trimmingMap.get(keyToDetect));
                if (p > 0) {
                    str = str.substring(p);
                }
            }
        }

        // partitioning
        java.util.List<String> numbers = new ArrayList<>(), separators = new ArrayList<>();
        int length = str.length(), p = 0;
        boolean number = false;
        while (p < length) {
            int start = p;
            while (p < length && Character.isDigit(str.charAt(p)) == number) {
                p++;
            }
            String part = str.substring(start, p);
            (number ? numbers : separators).add(part);
            number = !number;
        }

        // parsing
        if (!numbers.isEmpty() && !separators.isEmpty()) {
            try {
                int feature = Integer.parseInt(numbers.get(0));

                if (feature >= 5 && feature < MAX_ACCEPTED_VERSION) {
                    // Java 9+; Java 5+ (short format)
                    return feature;
                } else if (feature == 1 && numbers.size() > 1 && separators.size() > 1 && ".".equals(separators.get(1))) {
                    // Java 1.0 .. 1.4; Java 5+ (prefixed format)
                    feature = Integer.parseInt(numbers.get(1));
                    if (feature <= MAX_ACCEPTED_VERSION) {
                        return feature;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        throw new IllegalArgumentException(versionString);
    }

    private static boolean startsWithWord(String s, String word) {
        return s.startsWith(word) && (s.length() == word.length() || !Character.isLetterOrDigit(s.charAt(word.length())));
    }

    public static BufferedImage screenshot(Rectangle allBounds) {

        int[] pixels = robotPeer.getRGBPixels(allBounds);
        DirectColorModel screenCapCM = new DirectColorModel(24,
                /* red mask */ 0x00FF0000,
                /* green mask */ 0x0000FF00,
                /* blue mask */ 0x000000FF);
        DataBufferInt buffer = new DataBufferInt(pixels, pixels.length);
        int[] bandmasks = new int[3];
        bandmasks[0] = screenCapCM.getRedMask();
        bandmasks[1] = screenCapCM.getGreenMask();
        bandmasks[2] = screenCapCM.getBlueMask();

        WritableRaster raster = Raster.createPackedRaster(buffer, allBounds.width,
                allBounds.height, allBounds.width, bandmasks, null);

        return new BufferedImage(screenCapCM, raster, false, null);
    }

    void startCaptureAsync() {
        decoder = new Decoder(Launcher.getAppConfigs(), new DecoderCallback() {
            @Override
            public void imageReceived(int num) {
                imageIndex = num;
                updateProgress();
            }

            @Override
            public void fileBegin(FileInfo fileInfo) {
                totalImages = fileInfo.getChunkCount();
                filenameLabel.setText(fileInfo.getFilename());
                updateProgress();
                Launcher.log("Receiving file " + fileInfo.getFilename());
            }

            @Override
            public void fileEnd(FileInfo fileInfo, boolean crc32Matches) {
                if (crc32Matches) {
                    Launcher.log("Received file " + fileInfo.getFilename() + " with " + fileInfo.getLength() + "bytes");
                } else {
                    Launcher.log("Received file " + fileInfo.getFilename() + " with error: crc32 is not match");
                }
                resetProgress();
            }
        });

        new Thread(() -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            try {
                startCapture();
            } catch (Exception e) {
                log.error("Capture failed", e);
            }
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }).start();
    }

    void startCapture() throws Exception {
        Rectangle captureRect = new Rectangle(layout.left, layout.top, layout.width, layout.height);
        BufferedImage image;
        running = true;
        int[][] nonceArr = new int[layout.rows][layout.cols];
        while (running) {
            image = screenshot(captureRect);
            try {
                // crop image
                for (int i = 0; i < layout.rows; i++) {
                    for (int j = 0; j < layout.cols; j++) {
                        BufferedImage cropped = image.getSubimage(layout.width / layout.cols * j, layout.height / layout.rows * i, layout.width / layout.cols, layout.height / layout.rows);
                        Integer decode = decoder.decode(cropped, nonceArr[i][j]);
                        nonceArr[i][j] = decode != null ? decode : nonceArr[i][j];
                    }
                }
            } catch (NotFoundException | FormatException | ChecksumException ex) {
                log.error("Decode failed!maybe ignore", ex);
            } catch (DecodeException ex) {
                log.error("Decode failed: " + ex.getMessage());
                Launcher.log(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (Exception ex) {
                log.error("Decode failed!", ex);
                Launcher.log(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        decoder.reset();
        resetProgress();
    }

    void updateProgress() {
        fileProgress.setMaximum(totalImages);
        fileProgress.setValue(imageIndex);
    }

    void resetProgress() {
        totalImages = 0;
        imageIndex = 0;
        filenameLabel.setText("");
        updateProgress();
    }

    public void addLog(String s) {
        logBuf.put(s);
        String text = String.join("\n", logBuf.readFully());
        logView.setText(text);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setPreferredSize(new Dimension(400, 200));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.NORTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel2.add(panel3, BorderLayout.WEST);
        startButton = new JButton();
        startButton.setText("Start");
        panel3.add(startButton);
        stopButton = new JButton();
        stopButton.setEnabled(false);
        stopButton.setText("Stop");
        panel3.add(stopButton);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel2.add(panel4, BorderLayout.CENTER);
        fileProgress = new JProgressBar();
        fileProgress.setPreferredSize(new Dimension(146, 6));
        panel4.add(fileProgress, BorderLayout.NORTH);
        filenameLabel = new JLabel();
        filenameLabel.setHorizontalAlignment(0);
        filenameLabel.setPreferredSize(new Dimension(0, 17));
        filenameLabel.setText("");
        panel4.add(filenameLabel, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel2.add(panel5, BorderLayout.EAST);
        senderButton = new JButton();
        senderButton.setText("Sender");
        panel5.add(senderButton);
        configButton = new JButton();
        configButton.setText("Config");
        panel5.add(configButton);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel1.add(scrollPane1, BorderLayout.CENTER);
        logView = new JTextArea();
        logView.setEditable(false);
        logView.setMargin(new Insets(0, 5, 0, 0));
        logView.setRows(10);
        scrollPane1.setViewportView(logView);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
