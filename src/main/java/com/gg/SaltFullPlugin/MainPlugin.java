package com.gg.SaltFullPlugin;

import org.pf4j.Plugin;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainPlugin extends Plugin {
    private Map<String, WindowState> windowSizes = new HashMap<>();
    private Map<String, Boolean> windowStates = new HashMap<>();

    private int maxWidth = Integer.MAX_VALUE;
    private int maxHeight = Integer.MAX_VALUE;

    @Override
    public void start() {
        super.start();
        System.out.println("MainPlugin started");
//        ConsoleWindow.showConsole();
        new Thread(this::hideLyricBarFromTaskBar).start();

        new Thread(this::makeAllWindowsFullscreen).start();
    }

    private void hideLyricBarFromTaskBar() {
        Window lyricBar = null;

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Window[] windows = Window.getWindows();
            if (lyricBar != null && lyricBar.isActive()) {
                continue;
            } else {
                lyricBar = null;
            }

            for (Window window : windows) {
                if (window instanceof JFrame) {
                    continue;
                }
                lyricBar = window;

                WindowZOrderSetter.hideFromTaskbar(window);
            }
        }
    }

    private void makeAllWindowsFullscreen() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WindowZOrderSetter.allowSleep();
            System.out.println("已恢复系统睡眠功能");
        }));

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Window[] windows = Window.getWindows();

            for (Window window : windows) {
                if (!(window instanceof JFrame frame)) {
                    continue;
                }

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
                        setFullScreen(frame, bounds);
                        return;
                    }

                    restoreWindow(frame);
                });
            }
        }
    }

    private void setFullScreen(JFrame frame, Rectangle bounds) {
        windowStates.put(frame.getTitle(), true);

        frame.setSize(maxWidth, maxHeight);
        frame.setLocation(bounds.x, bounds.y);
        frame.setAlwaysOnTop(true);
        frame.setAlwaysOnTop(false);

        WindowZOrderSetter.disableRoundedCorners(WindowZOrderSetter.getWindowHandle(frame));
        WindowZOrderSetter.preventSleep(true);

        System.out.println("Made window " + frame.getTitle() + " fullscreen.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }

    private void restoreWindow(JFrame frame) {
        frame.setAlwaysOnTop(false);
        frame.setSize(windowSizes.get(frame.getTitle()).getWidth(),
                windowSizes.get(frame.getTitle()).getHeight());
        frame.setLocation(windowSizes.get(frame.getTitle()).getX(),
                windowSizes.get(frame.getTitle()).getY());

        WindowZOrderSetter.enableRoundedCorners(WindowZOrderSetter.getWindowHandle(frame));

        windowStates.put(frame.getTitle(), false);

        WindowZOrderSetter.allowSleep();

        System.out.println("Restored window " + frame.getTitle() + " to its original size.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
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