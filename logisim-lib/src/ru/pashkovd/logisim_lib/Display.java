/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package ru.pashkovd.logisim_lib;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.util.Arrays;

public class Display extends InstanceFactory {
    public static final Color[] color_map = new Color[]{
            new Color(0, 0, 0),
            new Color(128, 0, 0),
            new Color(0, 128, 0),
            new Color(128, 128, 0),
            new Color(0, 0, 128),
            new Color(128, 0, 128),
            new Color(0, 128, 128),
            new Color(192, 192, 192),
            new Color(128, 128, 128),
            new Color(255, 0, 0),
            new Color(0, 255, 0),
            new Color(255, 255, 0),
            new Color(0, 0, 255),
            new Color(255, 0, 255),
            new Color(0, 255, 255),
            new Color(255, 255, 255)
    };

    static final AttributeOption SHAPE_CIRCLE = new AttributeOption("circle", () -> "ioShapeCircle");
    static final AttributeOption SHAPE_SQUARE = new AttributeOption("square", () -> "ioShapeSquare");
    static final AttributeOption SHAPE_3DSQUARE = new AttributeOption("square", () -> "ioShape3DSquare");

    static final Attribute<Integer> ATTR_MATRIX_COLS = Attributes.forIntegerRange("matrixcols",
            () -> "ioMatrixCols", 2, Value.MAX_WIDTH);
    static final Attribute<Integer> ATTR_MATRIX_ROWS = Attributes.forIntegerRange("matrixrows",
            () -> "ioMatrixRows", 2, Value.MAX_WIDTH);
    static final Attribute<AttributeOption> ATTR_DOT_SHAPE = Attributes.forOption("dotshape", () -> "ioMatrixShape",
            new AttributeOption[]{SHAPE_CIRCLE, SHAPE_SQUARE, SHAPE_3DSQUARE});

    public Display() {
        super("DotMatrix", () -> "dotMatrixComponent");
        setAttributes(new Attribute<?>[]{
                ATTR_MATRIX_COLS, ATTR_MATRIX_ROWS, ATTR_DOT_SHAPE
        }, new Object[]{
                5, 7, SHAPE_SQUARE
        });
        setIconName("dotmat.gif");
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        int cols = attrs.getValue(ATTR_MATRIX_COLS);
        int rows = attrs.getValue(ATTR_MATRIX_ROWS);
        return Bounds.create(0, -5 * rows + 5, 10 * cols, 10 * rows);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr != ATTR_MATRIX_ROWS && attr != ATTR_MATRIX_COLS) {
            return;
        }
        instance.recomputeBounds();
        updatePorts(instance);
    }

    private void updatePorts(Instance instance) {
        int rows = instance.getAttributeValue(ATTR_MATRIX_ROWS);
        int cols = instance.getAttributeValue(ATTR_MATRIX_COLS);

        Port[] ps = new Port[]{
                new Port(0, -20, Port.INPUT, 1),
                new Port(0, 0, Port.INPUT, 32 - Integer.numberOfLeadingZeros(cols - 1)),
                new Port(0, 10, Port.INPUT, 32 - Integer.numberOfLeadingZeros(rows - 1)),
                new Port(0, -10, Port.INPUT, 4),
        };


        instance.setPorts(ps);
    }

    private State getState(InstanceState state) {
        int rows = state.getAttributeValue(ATTR_MATRIX_ROWS);
        int cols = state.getAttributeValue(ATTR_MATRIX_COLS);

        State data = (State) state.getData();
        if (data == null) {
            data = new State(rows, cols);
            state.setData(data);
        } else {
            data.updateSize(rows, cols);
        }
        return data;
    }

    @Override
    public void propagate(InstanceState state) {
        State data = getState(state);

        data.setSelect(state.getPort(2), state.getPort(1), state.getPort(0), state.getPort(3));

    }

    @Override
    public void paintInstance(InstancePainter painter) {
        AttributeOption figure = painter.getAttributeValue(ATTR_DOT_SHAPE);

        State data = getState(painter);
        Bounds bds = painter.getBounds();
        boolean showState = painter.getShowState();
        Graphics g = painter.getGraphics();
        int rows = data.rows;
        int cols = data.cols;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                int x = bds.getX() + 10 * i;
                int y = bds.getY() + 10 * j;
                if (!showState) { // for prints on paper
                    g.setColor(Color.GRAY);
                    g.fillOval(x + 1, y + 1, 8, 8);
                    continue;
                }
                Color c = data.get(j, i);
                g.setColor(c);
                if (figure == SHAPE_CIRCLE) {
                    g.fillOval(x + 1, y + 1, 8, 8);
                } else if (figure == SHAPE_3DSQUARE) {
                    Color temp = g.getColor();
                    g.setColor(temp.darker());
                    g.fillRect(x, y, 10, 10);
                    g.setColor(temp);
                    g.fillRect(x + 2, y + 2, 6, 6);
                } else {
                    g.fillRect(x, y, 10, 10);
                }

            }
        }
        g.setColor(Color.BLACK);
        GraphicsUtil.switchToWidth(g, 2);
        g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        GraphicsUtil.switchToWidth(g, 1);
        painter.drawPorts();
    }

    private static class State implements InstanceData, Cloneable {
        private int rows;
        private int cols;
        private int[] grid;
        private Value lastClock;

        public State(int rows, int cols) {
            this.rows = -1;
            this.cols = -1;
            updateSize(rows, cols);
        }

        @Override
        public State clone() {
            try {
                State ret = (State) super.clone();
                ret.grid = this.grid.clone();
                return ret;
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        private void updateSize(int rows, int cols) {
            if (this.rows == rows && this.cols == cols) {
                return;
            }
            this.rows = rows;
            this.cols = cols;
            int length = rows * cols;
            grid = new int[length];
            Arrays.fill(grid, 0);
        }

        private Color get(int row, int col) {
            int index = row * cols + col;
            int ret = grid[index];
            return color_map[ret];
        }

        public boolean updateClock(Value value) {
            Value old = lastClock;
            lastClock = value;
            return old == Value.FALSE && value == Value.TRUE;
        }


        private void setSelect(Value rowVector, Value colVector, Value clock, Value data) {
            if (!updateClock(clock)) return;
            int row = rowVector.toIntValue();
            int col = colVector.toIntValue();
            if (row == -1 || col == -1)
                return;
            if (row >= rows || col >= cols)
                return;
            if (data.toIntValue() == -1) {
                return;
            }
            int index = row * cols + col;
            grid[index] = data.toIntValue();

        }
    }
}
