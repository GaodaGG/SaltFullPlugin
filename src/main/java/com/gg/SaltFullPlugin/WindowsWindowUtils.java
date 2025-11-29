package com.gg.SaltFullPlugin;

import static com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE;
import static com.sun.jna.platform.win32.WinUser.GWL_STYLE;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_SHOWWINDOW;
import static com.sun.jna.platform.win32.WinUser.WS_BORDER;
import static com.sun.jna.platform.win32.WinUser.WS_OVERLAPPED;
import static com.sun.jna.platform.win32.WinUser.WS_THICKFRAME;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

public class WindowsWindowUtils {
    // Windows API 常量
    public static final int ZBID_SYSTEM_TOOLS = 16;
    public static final int ZBID_DESKTOP = 1;
    public static final int WS_EX_TOOLWINDOW = 0x00000080;
    public static final int ES_CONTINUOUS = 0x80000000;
    public static final int ES_SYSTEM_REQUIRED = 0x00000001;
    public static final int ES_DISPLAY_REQUIRED = 0x00000002;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWCP_DONOTROUND = 1;
    private static final int WS_EX_WINDOWEDGE = 256;
    private static final int WS_EX_DLGMODALFRAME = 0x000000001;
    private static WinDef.DWORD currentState = null;

    /**
     * 阻止系统进入睡眠状态
     *
     * @param keepDisplayOn 是否保持显示器开启
     */
    public static void preventSleep(boolean keepDisplayOn) {
        int flags = ES_CONTINUOUS | ES_SYSTEM_REQUIRED;
        if (keepDisplayOn) {
            flags |= ES_DISPLAY_REQUIRED;
        }

        currentState = Kernel32.INSTANCE.SetThreadExecutionState(new WinDef.DWORD(flags));
    }

    /**
     * 恢复系统默认睡眠行为
     */
    public static void allowSleep() {
        if (currentState != null) {
            Kernel32.INSTANCE.SetThreadExecutionState(currentState);
            currentState = null;
        } else {
            // 恢复默认状态
            Kernel32.INSTANCE.SetThreadExecutionState(new WinDef.DWORD(ES_CONTINUOUS));
        }
    }

    /**
     * 临时阻止睡眠（直到下一次系统事件）
     *
     * @param keepDisplayOn 是否保持显示器开启
     */
    public static void preventSleepTemporarily(boolean keepDisplayOn) {
        int flags = ES_SYSTEM_REQUIRED;
        if (keepDisplayOn) {
            flags |= ES_DISPLAY_REQUIRED;
        }

        Kernel32.INSTANCE.SetThreadExecutionState(new WinDef.DWORD(flags));
    }

    /**
     * 获取窗口标题
     *
     * @param hwnd 窗口句柄
     * @return 窗口标题
     */
    public static String getWindowTitle(WinDef.HWND hwnd) {
        char[] buffer = new char[512];
        User32.INSTANCE.GetWindowTextW(hwnd, buffer, 512);
        return Native.toString(buffer);
    }

    // 禁用窗口边框和阴影
    public static void disableWindowsBorder(WinDef.HWND hWnd) {
        try {
            int style = User32.INSTANCE.GetWindowLongA(hWnd, GWL_STYLE);
            int exStyle = User32.INSTANCE.GetWindowLongA(hWnd, GWL_EXSTYLE);

            // 移除边框和阴影样式
            style &= ~(WS_BORDER);
            style &= ~(WS_THICKFRAME);
//            style &= ~(WS_CAPTION);
            style &= ~(WS_OVERLAPPED);
            exStyle &= ~(WS_EX_WINDOWEDGE);
//            exStyle &= ~(WS_EX_COMPOSITED);
//            exStyle &= ~(WS_EX_DLGMODALFRAME);

            User32.INSTANCE.SetWindowLongA(hWnd, GWL_STYLE, style);
            User32.INSTANCE.SetWindowLongA(hWnd, GWL_EXSTYLE, exStyle);

            User32.INSTANCE.SetWindowPos(hWnd, null, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOSIZE | SWP_NOMOVE | SWP_SHOWWINDOW);

        } catch (Exception e) {
            System.err.println("禁用边框失败: " + e.getMessage());
        }
    }

    // 恢复窗口边框和阴影
    public static void enableWindowsBorder(WinDef.HWND hWnd) {
        int style = User32.INSTANCE.GetWindowLongA(hWnd, GWL_STYLE);
        int exStyle = User32.INSTANCE.GetWindowLongA(hWnd, GWL_EXSTYLE);

        style |= WS_BORDER;
        style |= WS_THICKFRAME;
//        style |= WS_CAPTION;
        style |= WS_OVERLAPPED;
        exStyle |= WS_EX_WINDOWEDGE;
//        exStyle |= WS_EX_COMPOSITED;
//        exStyle |= WS_EX_DLGMODALFRAME;

        User32.INSTANCE.SetWindowLongA(hWnd, GWL_STYLE, style);
        User32.INSTANCE.SetWindowLongA(hWnd, GWL_EXSTYLE, exStyle);

        User32.INSTANCE.SetWindowPos(hWnd, null, 0, 0, 0, 0,
                SWP_FRAMECHANGED | SWP_NOSIZE | SWP_NOMOVE | SWP_SHOWWINDOW);

    }

    // 禁用 Windows 11 圆角
    public static void disableRoundedCorners(WinDef.HWND hwnd) {
        try {
            if (isLowerThanWindows11()) return;

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
            if (isLowerThanWindows11()) return;

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

    // 获取窗口句柄 (HWND) - 兼容性更好的版本
    public static WinDef.HWND getWindowHandle(Window window) {
        if (window instanceof Frame frame) {
            Pointer windowPointer;
            try {
                windowPointer = Native.getWindowPointer(frame);
            } catch (Exception e) {
                String title = frame.getTitle();
                windowPointer = foundWindowPointerByTitle(title);
            }
            return new WinDef.HWND(windowPointer);
        } else if (window instanceof Dialog dialog) {
            Pointer windowPointer;
            try {
                windowPointer = Native.getWindowPointer(dialog);
            } catch (Exception e) {
                String title = dialog.getTitle();
                windowPointer = foundWindowPointerByTitle(title);
            }
            return new WinDef.HWND(windowPointer);
        }

        return null;
    }

    public static Pointer foundWindowPointerByTitle(String title) {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindowW(null, new WString(title));
        if (hwnd != null) {
            // 验证找到的窗口是否匹配
            String foundTitle = getWindowTitle(hwnd);
            if (title.equals(foundTitle)) {
                return hwnd.getPointer();
            }
        }
        return null;
    }

    // 检查是否为 Windows 11 或更低版本
    private static boolean isLowerThanWindows11() {
        String os = System.getProperty("os.name", "").toLowerCase();
        System.out.println("当前操作系统: " + os);
        return !"windows 11".equals(os);
    }

    // 定义 Kernel32 接口
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        // 设置线程执行状态
        WinDef.DWORD SetThreadExecutionState(WinDef.DWORD esFlags);
    }

    // 定义 User32 接口
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        int GetWindowLongA(WinDef.HWND hWnd, int nIndex);

        int SetWindowLongA(WinDef.HWND hWnd, int nIndex, int dwNewLong);

        WinDef.HWND FindWindowW(WString lpClassName, WString lpWindowName);

        int GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

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