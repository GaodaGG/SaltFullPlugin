package com.gg.SaltFullPlugin;

import com.sun.jna.platform.win32.WinDef;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MainPlugin extends Plugin {
    private final Map<WinDef.HWND, WindowState> windowSizes = new HashMap<>();
    private final Map<WinDef.HWND, Boolean> windowStates = new HashMap<>();
    ComponentAdapter adjustListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            putWindowSize((Window) e.getSource());
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            putWindowSize((Window) e.getSource());
        }
    };
    private int maxWidth = Integer.MAX_VALUE;
    private int maxHeight = Integer.MAX_VALUE;
    AWTEventListener eventListener = event -> {
        if (event.getID() != WindowEvent.WINDOW_OPENED && event.getID() != WindowEvent.WINDOW_STATE_CHANGED) {
            return;
        }

        Window window = (Window) event.getSource();
        if (!(window instanceof JFrame frame)) {
            return;
        }

        System.out.println("Window state changed: " + frame.getTitle() + " - " + event.getID());

        if (event.getID() == WindowEvent.WINDOW_OPENED) {
            putWindowSize(window);

            frame.addComponentListener(adjustListener);
            return;
        }

        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
            GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            maxWidth = bounds.width;
            maxHeight = bounds.height;

            SwingUtilities.invokeLater(() -> {
                WinDef.HWND hwnd = WindowZOrderSetter.getWindowHandle(frame);
                System.out.println(frame.getTitle() + " state " + (windowStates.get(hwnd) == null || !windowStates.get(hwnd)));
                if (windowStates.get(hwnd) == null || !windowStates.get(hwnd)) {
                    setFullScreen(frame, bounds);
                    return;
                }

                restoreWindow(frame);
            });
        }
    };

    @Override
    public void start() {
        super.start();
        System.out.println("MainPlugin started");
//        ConsoleWindow.showConsole();
        makeAllWindowsFullscreen();
    }

    @Override
    public void stop() {
        super.stop();

        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
        for (Window window : Window.getWindows()) {
            window.removeComponentListener(adjustListener);
        }
    }

    private void putWindowSize(Window window) {
        if (!(window instanceof JFrame frame)) {
            return;
        }

        WinDef.HWND hwnd = WindowZOrderSetter.getWindowHandle(window);
        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH || Boolean.TRUE.equals(windowStates.get(hwnd))) {
            return;
        }

        windowSizes.put(hwnd, new WindowState(frame.getWidth(), frame.getHeight(),
                frame.getX(), frame.getY()));
    }

    private void makeAllWindowsFullscreen() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WindowZOrderSetter.allowSleep();
            System.out.println("已恢复系统睡眠功能");
        }));

        for (Window window : Window.getWindows()) {
            if (!(window instanceof JFrame frame)) {
                return;
            }

            putWindowSize(window);
            frame.addComponentListener(adjustListener);
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.WINDOW_EVENT_MASK);
    }

    private void setFullScreen(JFrame frame, Rectangle bounds) {
        WinDef.HWND hWnd = WindowZOrderSetter.getWindowHandle(frame);
        windowStates.put(hWnd, true);

        frame.setSize(maxWidth, maxHeight);
        frame.setLocation(bounds.x, bounds.y);
        frame.setAlwaysOnTop(true);
        frame.setAlwaysOnTop(false);


        WindowZOrderSetter.disableWindowsBorder(hWnd);
        WindowZOrderSetter.disableRoundedCorners(hWnd);
        WindowZOrderSetter.preventSleep(true);

        System.out.println("Made window " + frame.getTitle() + " fullscreen.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }

    private void restoreWindow(JFrame frame) {
        WinDef.HWND hWnd = WindowZOrderSetter.getWindowHandle(frame);

        frame.setAlwaysOnTop(false);
        frame.setSize(windowSizes.get(hWnd).width(),
                windowSizes.get(hWnd).height());
        frame.setLocation(windowSizes.get(hWnd).x(),
                windowSizes.get(hWnd).y());

        WindowZOrderSetter.enableWindowsBorder(hWnd);
        WindowZOrderSetter.enableRoundedCorners(hWnd);

        windowStates.put(hWnd, false);

        WindowZOrderSetter.allowSleep();

        System.out.println("Restored window " + frame.getTitle() + " to its original size.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }


    record WindowState(int width, int height, int x, int y) {
        @NotNull
        @Override
        public String toString() {
            return "WindowState{" +
                    "width=" + width +
                    ", height=" + height +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}