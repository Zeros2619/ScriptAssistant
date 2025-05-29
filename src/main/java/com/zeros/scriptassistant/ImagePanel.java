package com.zeros.scriptassistant;

import cn.hutool.core.util.NumberUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Set;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private Rectangle rect;
    private int[] line;

    private int x;
    private int y;
    private int width;
    private int height;

    private double scaleX;
    private double scaleY;
    private Set<Rectangle> allNodeRect;
    private ImagePanelListener listener;

    public ImagePanel() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (image == null || listener == null) {
                    return;
                }
                Point imagePoint = scaleToImagePoint(e);
                if(imagePoint == null){
                    listener.onMousePositionChange(-1, -1);
                }else{
                    listener.onMousePositionChange(imagePoint.x, imagePoint.y);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            private int pressX, pressY;
            private int releaseX, releaseY;
            private long pressTime;
            private boolean rightBtn;
            long lastClickedTime = 0;
            int lastX = 0;
            int lastY = 0;

            @Override
            public void mousePressed(MouseEvent e) {
                pressX = e.getX();
                pressY = e.getY();
                pressTime = System.currentTimeMillis();
                rightBtn = SwingUtilities.isRightMouseButton(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (image == null || listener == null) {
                    return;
                }
                releaseX = e.getX();
                releaseY = e.getY();
                if (Math.abs(pressX - e.getX()) > 10 || Math.abs(pressY - e.getY()) > 10) {
                    Point start = scaleToImagePoint(pressX, pressY);
                    Point end = scaleToImagePoint(releaseX, releaseY);
                    if(start == null || end == null){
                        return;
                    }
                    // 画线
                    line = new int[]{pressX, pressY, releaseX, releaseY};
                    rect = null;
                    double duration = NumberUtil.div(System.currentTimeMillis() - pressTime, 1000, 2);
                    if(rightBtn) {
                        listener.onSwipe(start.x, start.y, end.x, end.y, duration);
                    }else{
                        double percentStartX = NumberUtil.div(start.x, image.getWidth(), 2);
                        double percentStartY = NumberUtil.div(start.y, image.getHeight(), 2);
                        double percentEndX = NumberUtil.div(end.x, image.getWidth(), 2);
                        double percentEndY = NumberUtil.div(end.y, image.getHeight(), 2);
                        listener.onSwipe(percentStartX, percentStartY, percentEndX, percentEndY, duration);
                    }
                }else{
                    line = null;
                }
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (image == null || listener == null) {
                    return;
                }
                Point imagePoint = scaleToImagePoint(e);
                if(imagePoint == null){
                    return;
                }
                int imageX = imagePoint.x;
                int imageY = imagePoint.y;
                long now = System.currentTimeMillis();
                if (now - lastClickedTime < 300) {
                    if (Math.abs(lastX - e.getX()) < 10 && Math.abs(lastY - e.getY()) < 10) {
                        // 双击
                        listener.onLeftDoubleClicked(imageX, imageY);
                        return;
                    }
                }
                lastX = e.getX();
                lastY = e.getY();
                lastClickedTime = now;
                listener.onLeftClicked(imageX, imageY);
                //根据点击位置，找到所属控件，控件树选中对应控件
                if (e.getButton() == MouseEvent.BUTTON3) {
                    //右键要生成代码
                    listener.onRightClicked(imageX, imageY);
                }
            }
        });
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (image == null || listener == null) {
                    return;
                }
                listener.onMouseWheel();
                paintRect(null);
            }
        });
    }

    private Point scaleToImagePoint(MouseEvent e){
        return scaleToImagePoint(e.getX(), e.getY());
    }

    private Point scaleToImagePoint(int originX, int originY){
        int imageX = (int) ((originX - x) * scaleX);
        int imageY = (int) ((originY - y) * scaleY);
        if (imageX < 0 || imageY < 0 || imageX >= image.getWidth() || imageY >= image.getHeight()) {
            return null;
        }
        return new Point(imageX, imageY);
    }

    public void setListener(ImagePanelListener listener) {
        this.listener = listener;
    }

    public void setAllNodeRect(Set<Rectangle> allNodeRect){
        this.allNodeRect = allNodeRect;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void paintRect(NodeInfo info) {
        if (info == null) {
            rect = null;
        } else {
            rect = new Rectangle(info.bounds.x, info.bounds.y, info.bounds.width, info.bounds.height);
        }
        line = null;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            drawImage(g2d);
            drawAllNodeRect(g2d);
            drawSelectedRect(g2d);
            drawSwipeLine(g2d);
        }
    }

    private void drawImage(Graphics2D g2d) {
        double aspectRatio = (double) image.getWidth(null) / (double) image.getHeight(null);
        if (aspectRatio >= (double) getWidth() / (double) getHeight()) {
            width = getWidth();
            height = (int) (width / aspectRatio);
            //y = (getHeight() - height) / 2;
        } else {
            height = getHeight();
            width = (int) (height * aspectRatio);
            //x = (getWidth() - width) / 2;
        }
        g2d.drawImage(image, x, y, width, height, this);
        // 计算缩放比
        scaleX = (double) image.getWidth() / (double) width;
        scaleY = (double) image.getHeight() / (double) height;
    }

    private void drawSelectedRect(Graphics2D g2d) {
        if (rect != null) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            Rectangle scaleRect = scaleRect(rect);
            g2d.drawRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height);
        }
    }

    private void drawAllNodeRect(Graphics2D g2d) {
        if(allNodeRect == null){
            return;
        }
        float[] dashPattern = {5, 5}; // 虚线的模式，10像素的实线后跟10像素的空白
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));
        for (Rectangle rectangle : allNodeRect) {
            if (rectangle.width > 10000) {
                continue;
            }
            // 绘制矩形虚线框
            Rectangle scaleRect = scaleRect(rectangle);
            g2d.drawRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height);
        }
    }

    private void drawSwipeLine(Graphics2D g2d) {
        if (line != null) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(line[0], line[1], line[2], line[3]);
        }
    }

    private Rectangle scaleRect(Rectangle rectangle) {
        int x1 = (int) (rectangle.x / scaleX) + x;
        int y1 = (int) (rectangle.y / scaleY) + y;
        int width = (int) (rectangle.width / scaleX);
        int height = (int) (rectangle.height / scaleY);
        return new Rectangle(x1, y1, width, height);
    }

    public Rectangle getRect() {
        return rect;
    }

    public static class ImagePanelListener {
        public void onMousePositionChange(int imageX, int imageY) {
        }

        public void onLeftDoubleClicked(int imageX, int imageY) {
        }

        public void onLeftClicked(int imageX, int imageY) {
        }

        public void onRightClicked(int imageX, int imageY) {
        }

        public void onMouseWheel() {
        }

        public void onSwipe(double startX, double startY, double endX, double endY, double duration){
        }
    }
}