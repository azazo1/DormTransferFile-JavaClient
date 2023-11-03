package com.azazo1.dormtransferfile;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.Scanner;

public class FileTransferClient implements Closeable {
    public final InetSocketAddress address;
    private final Socket socket;
    private final InputStream in;

    public FileTransferClient(InetSocketAddress address) throws IOException {
        this.address = address;
        socket = new Socket();
        socket.connect(address);
        in = socket.getInputStream();
    }

    public long receiveFileSize() throws IOException {
        long rst = 0;
        byte[] data = in.readNBytes(8);
        for (int i = 0; i < 8; i++) {
            rst = (rst << 8) + data[i];
        }
        return rst;
    }

    public String receiveFilename() throws IOException {
        int nameSize = Integer.parseInt(new String(in.readNBytes(3), StandardCharsets.UTF_8));
        return new String(in.readNBytes(nameSize), StandardCharsets.UTF_8);
    }

    public void receiveFileData(@NotNull File fileToStore, long fileSize, CallbackOfTransferProgress callback) throws IOException {
        // 检查文件可用性
        boolean created = fileToStore.createNewFile();
        if (!created) { // 如果本来就存在
            if (!fileToStore.isFile()) {
                throw new FileAlreadyExistsException("fileToStore must be a file, not a dir or else.");
            }
        }
        if (!fileToStore.canWrite()) {
            throw new FileCantAccessException("fileToStore must be writeable.");
        }
        // 接收输入并存到文件
        try (FileOutputStream fos = new FileOutputStream(fileToStore)) {
            byte[] buffer = new byte[FileTransferSenderServer.BUFFER_SIZE];
            int read;
            long now = 0;
            while ((read = in.read(buffer, 0, FileTransferSenderServer.BUFFER_SIZE)) >= 0) {
                fos.write(buffer, 0, read);
                now += read;
                callback.callback(now, fileSize);
            }
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignore) {
        }
        try {
            in.close();
        } catch (IOException ignore) {
        }
    }

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 1024) {
            return "%d Bytes".formatted(sizeInBytes);
        } else if (sizeInBytes <= 1024 * 1024) {
            return "%.2f KB".formatted(sizeInBytes * 1.0 / 1024);
        } else if (sizeInBytes <= 1024 * 1024 * 1024) {
            return "%.2f MB".formatted(sizeInBytes * 1.0 / 1024 / 1024);
        } else {
            return "%.2f GB".formatted(sizeInBytes * 1.0 / 1024 / 1024 / 1024);
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        try (SCMConnector scmConnector = new SCMConnector()) {
            String input = "";
            while (!input.equals("Q")) {
                System.out.print("Input your command(Type \"H\" to get help):");
                System.out.flush();
                input = scanner.nextLine();
                if (input.equals("H")) {
                    System.out.println("""
                            Commands                Function
                            F                       Get all available sender servers
                            C <ConnectionCode>      Connect to a sender and receive file, code length is 4
                            Q                       Quit
                            H                       Get help
                            """);
                } else if (input.equals("F")) {
                    scmConnector.fetchAvailableSenders();
                    scmConnector.readResponseCode();
                    scmConnector.readMsgTypeCode();
                    var senderList = MsgType.FetchAvailableSenders.parseMsg(scmConnector.in);
                    if (senderList.isEmpty()) {
                        System.out.println("No sender available");
                    }
                    for (int connCode : senderList.keySet()) {
                        System.out.printf("ConnectionCode: %04d, File: %s\n", connCode, senderList.get(connCode));
                    }
                } else if (input.startsWith("C")) {
                    int connCode = Integer.parseInt(input.substring(2, 6));
                    System.out.println("Connecting to sender:" + connCode);
                    scmConnector.querySenderServerAddress(connCode);
                    scmConnector.readResponseCode();
                    scmConnector.readMsgTypeCode();
                    var address = MsgType.QuerySenderServerAddress.parseMsg(scmConnector.in);
                    if (address != null) {
                        System.out.printf("Get sender server address: %s:%d%n", address.first, address.second);
                        try (var client = new FileTransferClient(new InetSocketAddress(address.first, address.second))) {
                            long fileSize = client.receiveFileSize();
                            String filename = client.receiveFilename();
                            System.out.printf("Target file name: %s, size: %s%n", filename, formatFileSize(fileSize));

                            File file = new File(filename);
                            System.out.printf("Output file path: %s%n", file.getAbsolutePath());
                            long transferStartTime = System.currentTimeMillis();
                            client.receiveFileData(file, fileSize, ((now, total) -> {
                                double progress = 1.0 * now / total;
                                int blockLength = 10;
                                int reached = (int) (blockLength * progress);
                                int unreached = blockLength - reached;
                                long speed = (int) (now / (System.currentTimeMillis() - transferStartTime) * 1000);
                                String progressString = "Transfer Progress: [" + "■".repeat(Math.max(0, reached)) +
                                        "□".repeat(Math.max(0, unreached)) + "] " + formatFileSize(speed) + "/s"
                                        + " Remains: " + formatFileSize(total - now);
                                System.out.print("\r" + progressString);
                            }));
                            System.out.println();
                        }
                    } else {
                        System.out.println("This sender is not available");
                    }
                }
            }
        }

    }
}
