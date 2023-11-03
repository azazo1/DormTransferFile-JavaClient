package com.azazo1.dormtransferfile;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileTransferSenderServer implements Closeable {
    public static final int BUFFER_SIZE = 8192;
    private final ServerSocket server;
    public final int port;
    public final File targetFile;


    public FileTransferSenderServer(int port, File fileToTransfer) throws IOException {
        server = new ServerSocket(port);
        this.port = port;
        targetFile = fileToTransfer;
    }

    public void launchSending(CallbackOfTransferProgress callback) throws IOException {
        try (Socket client = server.accept(); OutputStream os = client.getOutputStream()) {
            long fileSize = getFileSize(targetFile);
            os.write(convertFileSizeToBytes(fileSize));

            callback.callback(0, fileSize); // 用于通知连接建立

            byte[] fileNameData = targetFile.getName().getBytes(StandardCharsets.UTF_8);
            byte[] fileNameSizeData = "%03d".formatted(fileNameData.length).getBytes(StandardCharsets.UTF_8);
            os.write(fileNameSizeData);
            os.write(fileNameData);

            try (FileInputStream fis = new FileInputStream(targetFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                int now = 0;
                while ((read = fis.read(buffer, 0, BUFFER_SIZE)) >= 0) {
                    os.write(buffer, 0, read);
                    now += read;
                    callback.callback(now, fileSize);
                }
            }
        }
    }

    /**
     * 将一个文件长度转换为64比特位的数据(即转换成二进制表示, 有符号, 权重高的位优先)
     *
     * @return 返回长度为8的字节数组
     */
    @Contract(pure = true)
    public static byte @NotNull [] convertFileSizeToBytes(long size) {
        byte[] data = new byte[8];
        for (int i = 0; i < 8; i++) {
            data[7 - i] = (byte) ((size >> 8 * i) & 0xff);
        }
        return data;
    }

    public static long getFileSize(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            FileChannel fcn = fis.getChannel();
            return fcn.size();
        }
    }

    @Override
    public void close() {
        try {
            server.close();
        } catch (IOException ignore) {
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String input = null;
        File file;
        int senderPort = 49496;
        do {
            if (input != null) {
                System.out.println("Invalid file");
            }
            System.out.print("Input the file to transfer:");
            System.out.flush();
            input = scanner.nextLine();
            file = new File(input);
        } while (!file.isFile() || !file.canRead());
        try (SCMConnector scmConnector = new SCMConnector()) {
            scmConnector.registerSender(file.getName(), senderPort);
            scmConnector.readResponseCode();
            scmConnector.readMsgTypeCode();
            var connCode = MsgType.RegisterSender.parseMsg(scmConnector.in);
            System.out.printf("Registered at code: %04d%n", connCode);
            try (var server = new FileTransferSenderServer(senderPort, file)) {
                long transferStartTime = System.currentTimeMillis();
                server.launchSending(((now, total) -> {
                    if (now == 0) {
                        System.out.println("Client Connected");
                        scmConnector.close();
                    } else {
                        double progress = 1.0 * now / total;
                        int blockLength = 10;
                        int reached = (int) (blockLength * progress);
                        int unreached = blockLength - reached;
                        long speed = (int) (now / (System.currentTimeMillis() - transferStartTime) * 1000);
                        String progressString = "Transfer Progress: [" + "■".repeat(Math.max(0, reached)) +
                                "□".repeat(Math.max(0, unreached)) + "] " + FileTransferClient.formatFileSize(speed) + "/s"
                                + " Remains: " + FileTransferClient.formatFileSize(total - now);
                        System.out.print("\r" + progressString);
                    }
                }));
                System.out.println();
            }
        }
        System.out.println("Transfer over");
    }
}
