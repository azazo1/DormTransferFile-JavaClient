import com.azazo1.dormtransferfile.FileTransferClient;
import com.azazo1.dormtransferfile.FileTransferSenderServer;
import com.azazo1.dormtransferfile.MsgType;
import com.azazo1.dormtransferfile.SCMConnector;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileTransferSenderServerTest {

    @Test
    void getFileSize() throws IOException {
        File targetFile = new File("README.md");
        long expectedSize = FileTransferSenderServer.getFileSize(targetFile);
        try (FileInputStream fis = new FileInputStream(targetFile)) {
            long actualSize = fis.readAllBytes().length;
            assertEquals(expectedSize, actualSize);
        }
    }

    @Test
    void convertFileSizeToBytes() {
        System.out.println(Arrays.toString(FileTransferSenderServer.convertFileSizeToBytes(152200)));
    }

    @Test
    void launchSendingServerSide() throws IOException {
        File fileToTransfer = new File("D:\\Azazo1Files\\Downloaded\\Stable Diffusion整合包.zip");
        int senderPort = 45678;
        SCMConnector scm1 = new SCMConnector("192.168.1.200", 1000);
        scm1.registerSender(fileToTransfer.getName(), senderPort);
        scm1.readResponseCode();
        scm1.readMsgTypeCode();
        int connCode = MsgType.RegisterSender.parseMsg(scm1.in);

        // launch server
        try (FileTransferSenderServer server = new FileTransferSenderServer(senderPort, fileToTransfer)) {
            server.launchSending((now, total) -> {
                System.out.printf("Server: sent %d, total %d\n", now, total);
            });
        }
    }

    @Test
    void launchSendingTwoSize() throws IOException {
        File fileToTransfer = new File("D:\\Azazo1Files\\Downloaded\\Stable Diffusion整合包.zip");
        File fileToStore = new File("README_recv.zip");
        int senderPort = 45678;
        SCMConnector scm1 = new SCMConnector("192.168.1.200", 1000);
        scm1.registerSender(fileToTransfer.getName(), senderPort);
        scm1.readResponseCode();
        scm1.readMsgTypeCode();
        int connCode = MsgType.RegisterSender.parseMsg(scm1.in);

        SCMConnector scm2 = new SCMConnector("192.168.1.200", 1000);
        scm2.fetchAvailableSenders();
        scm2.readResponseCode();
        scm2.readMsgTypeCode();
        var senderList = MsgType.FetchAvailableSenders.parseMsg(scm2.in);
        int getConnCode = senderList.keySet().stream().toList().get(0);

        assertEquals(connCode, getConnCode);
        scm2.querySenderServerAddress(getConnCode);
        scm2.readResponseCode();
        scm2.readMsgTypeCode();
        var address = MsgType.QuerySenderServerAddress.parseMsg(scm2.in);
        assertNotNull(address);


        // launch server
        new Thread(() -> {
            try {
                try (FileTransferSenderServer server = new FileTransferSenderServer(senderPort, fileToTransfer)) {
                    server.launchSending((now, total) -> {
                        System.out.printf("Server: sent %d, total %d\n", now, total);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        // launch client
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try (FileTransferClient client = new FileTransferClient(new InetSocketAddress(address.first, address.second))) {
            long fileSize = client.receiveFileSize();
            String filename = client.receiveFilename();
            System.out.println("Client: filename: " + filename);
            System.out.printf("Client: file size %d bytes\n", fileSize);
            client.receiveFileData(fileToStore, fileSize, (now, total) -> {
                System.out.printf("Client: file now %d total %d\n", now, total);
            });
        }

        assertEquals(FileTransferSenderServer.getFileSize(fileToTransfer), FileTransferSenderServer.getFileSize(fileToStore));
    }
}