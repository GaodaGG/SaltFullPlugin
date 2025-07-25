package com.gg.SaltFullPlugin;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.*;

import static com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE;

public class WindowZOrderSetter {
    // Windows API 常量
    public static final int ZBID_SYSTEM_TOOLS = 16;
    public static final int ZBID_DESKTOP = 1;
    public static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWCP_DONOTROUND = 1;

    // 设置窗口到特定 Z 序级别
    public static boolean setWindowZBand(Window window, int zBandId) {
        try {
            // 获取窗口句柄
            WinDef.HWND hwnd = getWindowHandle(window);
            if (hwnd == null) {
                System.err.println("无法获取窗口句柄");
                return false;
            }

            // 禁用圆角效果（可选，但推荐）
            disableRoundedCorners(hwnd);

            // 设置 Z 序
            return setZBandWithSetWindowPos(hwnd, zBandId);
        } catch (Exception e) {
            System.err.println("设置 Z 序失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 使用 SetWindowPos 设置 Z 序
    private static boolean setZBandWithSetWindowPos(WinDef.HWND hwnd, int zBandId) {
        // 特殊句柄值对应不同的 Z 序级别
        WinDef.HWND hWndInsertAfter;

        switch (zBandId) {
            case ZBID_DESKTOP:
                hWndInsertAfter = new WinDef.HWND(Pointer.createConstant(1));
                break;
            case ZBID_SYSTEM_TOOLS:
                // 获取桌面窗口 (Progman)
                WinDef.HWND hwndProgman = User32.INSTANCE.FindWindowW(new WString("Progman"), new WString("Program Manager"));
                if (hwndProgman == null) {
                    System.err.println("找不到桌面窗口 (Progman)");
                    return false;
                }
                hWndInsertAfter = hwndProgman;
                break;
            default:
                hWndInsertAfter = new WinDef.HWND(Pointer.createConstant(0));
        }

        // 设置窗口位置标志 (不改变大小/位置)
        final int SWP_NOSIZE = 0x0001;
        final int SWP_NOMOVE = 0x0002;
        final int SWP_NOACTIVATE = 0x0010;
        final int SWP_FLAGS = SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE;

        return User32.INSTANCE.SetWindowPos(
                hwnd,
                hWndInsertAfter,
                0, 0, 0, 0,
                SWP_FLAGS
        );
    }

    // 禁用 Windows 11 圆角
    public static void disableRoundedCorners(WinDef.HWND hwnd) {
        try {
            if (!isWindows11OrHigher()) return;

            IntByReference preference = new IntByReference(DWMWCP_DONOTROUND);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(
                    hwnd,
                    DWMWA_WINDOW_CORNER_PREFERENCE,
                    preference.getPointer(),
                    Native.POINTER_SIZE
            );
        } catch (Exception e) {
            System.err.println("禁用圆角失败: " + e.getMessage());
        }
    }

    // 恢复 Windows 11 圆角
    public static void enableRoundedCorners(WinDef.HWND hwnd) {
        try {
            if (!isWindows11OrHigher()) return;

            IntByReference preference = new IntByReference(0); // 恢复默认圆角
            Dwmapi.INSTANCE.DwmSetWindowAttribute(
                    hwnd,
                    DWMWA_WINDOW_CORNER_PREFERENCE,
                    preference.getPointer(),
                    Native.POINTER_SIZE
            );
        } catch (Exception e) {
            System.err.println("恢复圆角失败: " + e.getMessage());
        }
    }

    // 隐藏窗口的任务栏条目
    public static void hideFromTaskbar(Window window) {
        try {
            // 获取窗口句柄
            WinDef.HWND hwnd = getWindowHandle(window);
            if (hwnd == null) return;

            // 获取当前扩展样式
            int exStyle = User32.INSTANCE.GetWindowLongA(hwnd, GWL_EXSTYLE);

            // 添加工具窗口样式
            exStyle |= WS_EX_TOOLWINDOW;

            // 设置新的扩展样式
            User32.INSTANCE.SetWindowLongA(hwnd, GWL_EXSTYLE, exStyle);
        } catch (Exception e) {
            System.err.println("隐藏任务栏条目失败: " + e.getMessage());
        }
    }

    // 获取窗口句柄 (HWND) - 兼容性更好的版本
    public static WinDef.HWND getWindowHandle(Window window) {
        try {
            if (window instanceof Frame frame) {
                return new WinDef.HWND(Native.getWindowPointer(frame));
            } else if (window instanceof Dialog dialog) {
                return new WinDef.HWND(Native.getWindowPointer(dialog));
            }

            return null;
        } catch (Exception e) {
            System.err.println("获取窗口句柄失败: " + e.getMessage());
            return null;
        }
    }

    // 检查是否为 Windows 11 或更高版本
    private static boolean isWindows11OrHigher() {
        String os = System.getProperty("os.name", "").toLowerCase();
        System.out.println("当前操作系统: " + os);
        return "windows 11".equals(os);
    }

    // 定义 User32 接口
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        int GetWindowLongA(WinDef.HWND hWnd, int nIndex);

        int SetWindowLongA(WinDef.HWND hWnd, int nIndex, int dwNewLong);

        WinDef.HWND FindWindowW(WString lpClassName, WString lpWindowName);

        boolean SetWindowPos(
                WinDef.HWND hWnd,
                WinDef.HWND hWndInsertAfter,
                int X, int Y, int cx, int cy,
                int uFlags
        );
    }


    // 定义 Dwmapi 接口
    public interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                int dwAttribute,
                Pointer pvAttribute,
                int cbAttribute
        );
    }
}