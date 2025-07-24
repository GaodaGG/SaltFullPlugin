package com.gg.saltPlayerPlugin;

import org.pf4j.Plugin;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainPlugin extends Plugin {
    Map<String, WindowState> windowSizes = new HashMap<>();
    Map<String, Boolean> windowStates = new HashMap<>();
    Shape shape = null;

    int maxWidth = Integer.MAX_VALUE;
    int maxHeight = Integer.MAX_VALUE;

    @Override
    public void start() {
        super.start();
        System.out.println("MainPlugin started");
        ConsoleWindow.showConsole();

//        new Thread(() -> {
//            try {
//                while (true) {
//                    Thread.sleep(3000);
//                    Window[] windows = Window.getWindows();
//                    for (Window window : windows) {
//                        if (window instanceof Frame) {
//                            continue;
//                        }
//
////                        window.setAlwaysOnTop(true);
////                        WindowZOrderSetter.setWindowZBand(window, WindowZOrderSetter.ZBID_SYSTEM_TOOLS);
//                        System.out.println("Window: " + window.getName() + " is set to always on top.");
//                    }
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();

        new Thread(this::makeAllWindowsFullscreen).start();
    }

    private void makeAllWindowsFullscreen() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Window[] windows = Window.getWindows();

            for (Window window : windows) {
                if (!(window instanceof JFrame)) {
                    continue;
                }
                JFrame frame = (JFrame) window;

                //判断是否全屏
                if (frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {
                    if (maxWidth > frame.getWidth() && maxHeight > frame.getHeight()) {
                        windowSizes.put(frame.getTitle(), new WindowState(frame.getWidth(), frame.getHeight(),
                                frame.getX(), frame.getY()));
                    }
                    continue;
                }

                GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                maxWidth = bounds.width;
                maxHeight = bounds.height;

                SwingUtilities.invokeLater(() -> {
                    System.out.println(frame.getTitle() + " state " + (windowStates.get(frame.getTitle()) == null || !windowStates.get(frame.getTitle())));
                    if (windowStates.get(frame.getTitle()) == null || !windowStates.get(frame.getTitle())) {
                        shape = frame.getShape();
                        windowStates.put(frame.getTitle(), true);

                        frame.setSize(maxWidth, maxHeight);
                        frame.setLocation(bounds.x, bounds.y);
                        frame.setAlwaysOnTop(true);

                        WindowZOrderSetter.disableRoundedCorners(WindowZOrderSetter.getWindowHandle(frame));

                        System.out.println("Made window " + frame.getTitle() + " fullscreen.");
                        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
                    } else {
                        frame.setAlwaysOnTop(false);
                        frame.setSize(windowSizes.get(frame.getTitle()).getWidth(),
                                windowSizes.get(frame.getTitle()).getHeight());
                        frame.setLocation(windowSizes.get(frame.getTitle()).getX(),
                                windowSizes.get(frame.getTitle()).getY());

//                        frame.setShape(shape); // 恢复原来的形状
                        WindowZOrderSetter.enableRoundedCorners(WindowZOrderSetter.getWindowHandle(frame));

                        windowStates.put(frame.getTitle(), false);
                        System.out.println("Restored window " + frame.getTitle() + " to its original size.");
                        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
                    }

                });
            }
        }
    }

    class WindowState {
        private int width;
        private int height;
        private int x;
        private int y;

        public WindowState(int width, int height, int x, int y) {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}