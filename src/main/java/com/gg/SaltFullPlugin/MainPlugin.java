package com.gg.SaltFullPlugin;

import com.sun.jna.platform.win32.WinDef;
import org.pf4j.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MainPlugin extends Plugin {
    private final Map<String, WindowState> windowSizes = new HashMap<>();
    private final Map<String, Boolean> windowStates = new HashMap<>();

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
            windowSizes.put(frame.getTitle(), new WindowState(frame.getWidth(), frame.getHeight(),
                    frame.getX(), frame.getY()));
            return;
        }

        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
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
    }

    private void makeAllWindowsFullscreen() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WindowZOrderSetter.allowSleep();
            System.out.println("已恢复系统睡眠功能");
        }));

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.WINDOW_EVENT_MASK);
    }

    private void setFullScreen(JFrame frame, Rectangle bounds) {
        windowStates.put(frame.getTitle(), true);

        frame.setSize(maxWidth, maxHeight);
        frame.setLocation(bounds.x, bounds.y);
        frame.setAlwaysOnTop(true);
        frame.setAlwaysOnTop(false);

        WinDef.HWND hWnd = WindowZOrderSetter.getWindowHandle(frame);
        WindowZOrderSetter.disableWindowsBorder(hWnd);
        WindowZOrderSetter.disableRoundedCorners(hWnd);
        WindowZOrderSetter.preventSleep(true);

        System.out.println("Made window " + frame.getTitle() + " fullscreen.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }

    private void restoreWindow(JFrame frame) {
        frame.setAlwaysOnTop(false);
        frame.setSize(windowSizes.get(frame.getTitle()).width(),
                windowSizes.get(frame.getTitle()).height());
        frame.setLocation(windowSizes.get(frame.getTitle()).x(),
                windowSizes.get(frame.getTitle()).y());

        WinDef.HWND hWnd = WindowZOrderSetter.getWindowHandle(frame);
        WindowZOrderSetter.enableWindowsBorder(hWnd);
        WindowZOrderSetter.enableRoundedCorners(hWnd);

        windowStates.put(frame.getTitle(), false);

        WindowZOrderSetter.allowSleep();

        System.out.println("Restored window " + frame.getTitle() + " to its original size.");
        System.out.println("Window size: " + frame.getWidth() + "x" + frame.getHeight());
    }


    record WindowState(int width, int height, int x, int y) {
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