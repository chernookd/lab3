package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Main extends JFrame {
    private JPanel canvas;
    private int scale = 20;
    private List<JTextField> coordFields = new ArrayList<>();
    private List<JLabel> coordLabels = new ArrayList<>();
    private JSlider scaleSlider;
    private List<JRadioButton> algoButtons = new ArrayList<>();
    private List<Consumer<Graphics>> algos = new ArrayList<>();
    private JTextArea output;
    private int centerX;
    private int centerY;
    private boolean doLogging = false;

    public Main() {
        super("Rasterization");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initCanvas();
        initControlPanel();
        initAlgoFunctions();

        JScrollPane scrollPane = new JScrollPane(output);
        scrollPane.setPreferredSize(new Dimension(600, 100));

        add(canvas, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);
        add(scrollPane, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void initCanvas() {
        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGrid(g);
                drawAxis(g);
                rasterizeLine(g);
            }
        };
        canvas.setPreferredSize(new Dimension(400, 400));
        canvas.setDoubleBuffered(true);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridLayout(11, 2));

        ButtonGroup algoGroup = new ButtonGroup();
        for (JRadioButton button : algoButtons) {
            controlPanel.add(button);
            algoGroup.add(button);
        }

        for (int i = 0; i < coordLabels.size(); i++) {
            controlPanel.add(coordLabels.get(i));
            controlPanel.add(coordFields.get(i));
        }

        controlPanel.add(new JLabel("Scale:"));
        controlPanel.add(scaleSlider);

        JButton drawButton = new JButton("Draw");
        drawButton.addActionListener(e -> {
            try {
                scale = scaleSlider.getValue();
                canvas.repaint();
            } catch (NumberFormatException ex) {
                output.setText("Invalid cell size!");
            }
        });
        controlPanel.add(drawButton);

        JCheckBox loggingCheckBox = new JCheckBox("Logging");
        loggingCheckBox.addItemListener(e -> doLogging = e.getStateChange() == ItemEvent.SELECTED);
        controlPanel.add(loggingCheckBox);

        return controlPanel;
    }

    private void initControlPanel() {
        for (int i = 0; i < 4; i++) {
            coordFields.add(new JTextField("0"));
            coordLabels.add(new JLabel((i % 2 == 0 ? "x" : "y") + (i / 2)));
        }

        scaleSlider = new JSlider(10, 100, scale);
        scaleSlider.setMajorTickSpacing(18);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);

        algoButtons.add(new JRadioButton("Step Algorithm", true));
        algoButtons.add(new JRadioButton("DDA Algorithm"));
        algoButtons.add(new JRadioButton("Bresenham Algorithm Line"));
        JRadioButton circleButton = new JRadioButton("Bresenham Algorithm Circle");
        circleButton.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            coordFields.get(3).setVisible(!selected);
            coordLabels.get(2).setText(selected ? "R" : "x1");
            coordLabels.get(3).setVisible(!selected);
        });
        algoButtons.add(circleButton);

        output = new JTextArea();
        output.setEditable(false);
    }




    private void log(String message) {
        if (doLogging) {
            output.append(message + "\n");
        }
    }



    private void initAlgoFunctions() {
        algos.add(this::stepByStepAlgorithm);
        algos.add(this::ddaAlgorithm);
        algos.add(this::bresenhamAlgorithmLine);
        algos.add(this::bresenhamAlgorithmCircle);
    }



    private void drawGrid(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < canvas.getWidth(); i += scale) {
            g.drawLine(i, 0, i, canvas.getHeight());
        }
        for (int i = 0; i < canvas.getHeight(); i += scale) {
            g.drawLine(0, i, canvas.getWidth(), i);
        }
    }

    private void drawAxis(Graphics g) {
        g.setColor(Color.BLACK);
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        centerX = width / 2 / scale * scale;
        centerY = height / 2 / scale * scale;

        g.drawLine(centerX, 0, centerX, height);
        g.drawLine(0, centerY, width, centerY);
        g.drawString("X", width - 20, centerY - 10);
        g.drawString("Y", centerX + 10, 20);

        for (int i = 0; i <= width / 2 - 2 * scale; i += scale) {
            if (i != 0) {
                g.drawLine(centerX + i, centerY - 5, centerX + i, centerY + 5);
                g.drawString(String.valueOf(i / scale), centerX + i - 5, centerY + 20);

                g.drawLine(centerX - i, centerY - 5, centerX - i, centerY + 5);
                g.drawString(String.valueOf(-i / scale), centerX - i - 10, centerY + 20);
            }
        }
    }

    private void rasterizeLine(Graphics g) {
        int id = -1;
        for (int i = 0; i < algoButtons.size(); i++) {
            if (algoButtons.get(i).isSelected()) {
                id = i;
                break;
            }
        }

        if (id == -1) return;

        output.setText(algoButtons.get(id).getText() + ":\n");
        long startTime = System.currentTimeMillis();
        algos.get(id).accept(g);
        long elapsedTime = System.currentTimeMillis() - startTime;

        output.append("Time: " + elapsedTime + " ms\n");
    }


    private void stepByStepAlgorithm(Graphics g) {
        int[] params = getParams();
        int x0 = params[0];
        int y0 = params[1];
        int x1 = params[2];
        int y1 = params[3];

        if (x0 > x1) {
            int temp = x0;
            x0 = x1;
            x1 = temp;
        }
        if (y0 > y1) {
            int temp = y0;
            y0 = y1;
            y1 = temp;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;

        if (dx > dy) {
            double slope = (double) dy / dx;
            for (int x = x0; x <= x1; x++) {
                double y = y0 + slope * (x - x0);
                drawPixel(g, x, (int) Math.floor(y));
            }
        } else {
            double slope = (double) dx / dy;
            for (int y = y0; y <= y1; y++) {
                double x = x0 + slope * (y - y0);
                drawPixel(g, (int) Math.floor(x), y);
            }
        }
    }
    private void ddaAlgorithm(Graphics g) {
        int[] params = getParams(); // Assuming getParams() returns an array of integers
        int x0 = params[0];
        int y0 = params[1];
        int x1 = params[2];
        int y1 = params[3];

        int dx = x1 - x0;
        int dy = y1 - y0;

        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        double xInc = dx / (double) steps;
        double yInc = dy / (double) steps;

        double x = x0;
        double y = y0;

        for (int i = 0; i <= steps; i++) {
            drawPixel(g, (int) Math.round(x), (int) Math.round(y));
            x += xInc;
            y += yInc;
        }
    }


    private void bresenhamAlgorithmLine(Graphics g) {
        int[] params = getParams();
        int x0 = params[0];
        int y0 = params[1];
        int x1 = params[2];
        int y1 = params[3];

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;

        int err = dx - dy;

        while (true) {
            drawPixel(g, x0, y0);
            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }


    private void bresenhamAlgorithmCircle(Graphics g) {
        int[] params = getParams();
        int xc = params[0];
        int yc = params[1];
        int r = params[2];

        int x = 0;
        int y = r;
        int d = 3 - 2 * r;

        while (x <= y) {
            drawCirclePixels(g, xc, yc, x, y);
            x++;
            if (d > 0) {
                d += 4 * (x - y) + 10;
                y--;
            } else {
                d += 4 * x + 6;
            }
        }
    }


    private int[] getParams() {
        try {
            int x0 = Integer.parseInt(coordFields.get(0).getText());
            int y0 = Integer.parseInt(coordFields.get(1).getText());
            int x1 = Integer.parseInt(coordFields.get(2).getText());
            int y1 = coordFields.get(3).isVisible() ? Integer.parseInt(coordFields.get(3).getText()) : 0;
            return new int[]{x0, y0, x1, y1};
        } catch (NumberFormatException ex) {
            output.append("Invalid input in coordinates!\n");
            return new int[]{0, 0, 0, 0};
        }
    }


    private void drawPixel(Graphics g, int x, int y) {
        int screenX = centerX + x * scale;
        int screenY = centerY - y * scale;
        g.fillRect(screenX - scale / 2, screenY - scale / 2, scale, scale);
    }



    private void drawCirclePixels(Graphics g, int xc, int yc, int x, int y) {
        drawPixel(g, xc + x, yc + y);
        drawPixel(g, xc - x, yc + y);
        drawPixel(g, xc + x, yc - y);
        drawPixel(g, xc - x, yc - y);
        drawPixel(g, xc + y, yc + x);
        drawPixel(g, xc - y, yc + x);
        drawPixel(g, xc + y, yc - x);
        drawPixel(g, xc - y, yc - x);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
