package com.gg.saltPlayerPlugin;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 控制台窗口类，用于显示插件的输出信息。
 * 该类使用 Swing 创建一个简单的文本区域来显示控制台输出。
 * 仅是在SPW没有log方法的临时替代品
 * 感谢 Deepseek (AI 真jb好用)
 */
public class ConsoleWindow {
    private static JFrame consoleFrame;
    private static JTextArea textArea;
    private static boolean isShown = false;

    public static void showConsole() {
        if (isShown) return;
        isShown = true;

        // 设置系统编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");

        SwingUtilities.invokeLater(() -> {
            consoleFrame = new JFrame("插件控制台");
            consoleFrame.setName("test");
            textArea = new JTextArea(25, 80);
            textArea.setEditable(false);
            textArea.setBackground(Color.BLACK);
            textArea.setForeground(Color.GREEN);
            // 设置支持中文的字体
            textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(textArea);
            consoleFrame.add(scrollPane);
            consoleFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            consoleFrame.setSize(800, 600);
            consoleFrame.setVisible(true);

            redirectSystemStreams();
        });
    }

    private static void redirectSystemStreams() {
        try {
            // 使用UTF-8编码的输出流
            OutputStream out = new OutputStream() {
                private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                @Override
                public void write(int b) throws IOException {
                    if (b == '\n') {
                        String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                        SwingUtilities.invokeLater(() -> {
                            textArea.append(text + "\n");
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        });
                        buffer.reset();
                    } else {
                        buffer.write(b);
                    }
                }
            };

            System.setOut(new PrintStream(out, true, "UTF-8"));
            System.setErr(new PrintStream(out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}