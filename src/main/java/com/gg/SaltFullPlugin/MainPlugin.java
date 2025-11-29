package com.gg.SaltFullPlugin;

import com.sun.jna.platform.win32.WinDef;

import org.jetbrains.annotations.NotNull;
import org.pf4j.Plugin;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("unused")
public class MainPlugin extends Plugin {
    private final Map<WinDef.HWND, WindowState> windowSizes = new HashMap<>();
    private final Map<WinDef.HWND, Boolean> windowStates = new HashMap<>();

    private int maxWidth = Integer.MAX_VALUE;
    private int maxHeight = Integer.MAX_VALUE;

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

    AWTEventListener eventListener = event -> {
        if (event.getID() != WindowEvent.WINDOW_OPENED && event.getID() != WindowEvent.WINDOW_STATE_CHANGED) {
            return;
        }

        Window window = (Window) event.getSource();
        if (!(window instanceof JFrame frame)) {
            return;
        }

        System.out.println("Window state changed: " + frame.getTitle() + " - " + event.getID());

        putWindowSize(window);

        if (event.getID() == WindowEvent.WINDOW_OPENED) {
            frame.addComponentListener(adjustListener);
            return;
        }

        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
            GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            maxWidth = bounds.width;
            maxHeight = bounds.height;

            SwingUtilities.invokeLater(() -> {
                WinDef.HWND hwnd = WindowsWindowUtils.getWindowHandle(frame);
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
        makeAllWindowsFullscreen();
    }

    @Override
    public void stop() {
        super.stop();

        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
        for (Window window : Window.getWindows()) {
            window.removeComponentListener(adjustListener);
        }

        windowSizes.clear();
        windowStates.clear();
    }

    private void putWindowSize(Window window) {
        if (!(window instanceof JFrame frame)) {
            return;
        }

        WinDef.HWND hwnd = WindowsWindowUtils.getWindowHandle(window);
        if (hwnd == null) {
            System.err.println("putWindowSize: HWND is null for window: " + frame.getTitle());
            return;
        }

        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH || Boolean.TRUE.equals(windowStates.get(hwnd))) {
            System.err.println("putWindowSize: Window is maximized or fullscreen, skipping size update for window: " + frame.getTitle());
            return;
        }

        windowSizes.put(hwnd, new WindowState(frame.getWidth(), frame.getHeight(),
                frame.getX(), frame.getY()));
    }

    private void makeAllWindowsFullscreen() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WindowsWindowUtils.allowSleep();
            System.out.println("已恢复系统睡眠功能");
        }));

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.WINDOW_EVENT_MASK);

        for (Window window : Window.getWindows()) {
            if (!(window instanceof JFrame frame)) {
                break;
            }

            putWindowSize(window);
            frame.addComponentListener(adjustListener);
        }
    }

    private void setFullScreen(JFrame frame, Rectangle bounds) {
        WinDef.HWND hWnd = WindowsWindowUtils.getWindowHandle(frame);
        windowStates.put(hWnd, true);

        frame.setSize(maxWidth, maxHeight);
        frame.setLocation(bounds.x, bounds.y);
        frame.setAlwaysOnTop(true);
        frame.setAlwaysOnTop(false);


        WindowsWindowUtils.disableWindowsBorder(hWnd);
        WindowsWindowUtils.disableRoundedCorners(hWnd);
        WindowsWindowUtils.preventSleep(true);

        System.out.println("Made window " + frame.getTitle() + " fullscreen.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }

    private void restoreWindow(JFrame frame) {
        WinDef.HWND hWnd = WindowsWindowUtils.getWindowHandle(frame);
        AtomicReference<WindowState> windowStateAtomicReference = new AtomicReference<>(windowSizes.get(hWnd));
        if (windowStateAtomicReference.get() == null) {
            windowSizes.forEach((hwnd, state) -> {
                if (!WindowsWindowUtils.getWindowTitle(hwnd).equals("Untitled")) {
                    windowStateAtomicReference.set(state);
                    putWindowSize(frame);
                }
            });
        }
        WindowState windowState = windowStateAtomicReference.get();

        frame.setAlwaysOnTop(false);
        frame.setSize(windowState.width(),
                windowState.height());
        frame.setLocation(windowState.x(),
                windowState.y());

        WindowsWindowUtils.enableWindowsBorder(hWnd);
        WindowsWindowUtils.enableRoundedCorners(hWnd);

        windowStates.put(hWnd, false);

        WindowsWindowUtils.allowSleep();

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