import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class SCMConnector {
    public static final String SCM_IP = "192.168.1.108";
    public static final int SCM_PORT = 8088;
    private final Socket socket;
    public final InputStream in;
    public final OutputStream out;
    private int msgSeqCursor = 0;

    private int nextMsgSeq() {
        int rst = msgSeqCursor++;
        if (msgSeqCursor > 99999) {
            msgSeqCursor = 0;
        }
        return rst;
    }

    public SCMConnector() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(SCM_IP, SCM_PORT));
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    /**
     * 向连接服务器查询可用的文件发送者
     *
     * @return 消息序号码
     */
    public int querySenderServerAddress(int connCode) throws IOException {
        return sendMsg(MsgType.QUERY_SENDER_SERVER_ADDRESS, String.format("%04d", connCode));
    }

    /**
     * 向连接服务器查询可用的文件发送者
     *
     * @return 消息序号码
     */
    public int fetchAvailableSenders() throws IOException {
        return sendMsg(MsgType.FETCH_AVAILABLE_SENDER);
    }

    /**
     * 向服务器申请注册成为文件发送者
     *
     * @return 消息序号码
     */
    public int registerSender(@NotNull String filename, int port) throws IOException {
        int filenameLength = filename.getBytes(StandardCharsets.UTF_8).length;
        return sendMsg(MsgType.REGISTER_SENDER, String.format("%03d%s%05d", filenameLength, filename, port));
    }

    /**
     * 读取消息序号码, 但不能直接连续调用, 因为消息内容可能未被处理
     */
    public int readResponseCode() throws IOException {
        byte[] codeBytes = in.readNBytes(5);
        return Integer.parseInt(new String(codeBytes, StandardCharsets.UTF_8));
    }

    /**
     * 读取消息类型码, 但不能直接连续调用, 因为消息内容可能未被处理
     */
    public int readMsgTypeCode() throws IOException {
        byte[] codeBytes = in.readNBytes(2);
        return Integer.parseInt(new String(codeBytes, StandardCharsets.UTF_8));
    }

    private int sendMsg(int msgType, String content) throws IOException {
        int seq = nextMsgSeq();
        send_(String.format("%05d%02d%s", seq, msgType, content));
        return seq;
    }

    private int sendMsg(int msgType) throws IOException {
        int seq = nextMsgSeq();
        send_(String.format("%05d%02d", seq, msgType));
        return seq;
    }

    private void send_(@NotNull String message) throws IOException {
        out.write(message.getBytes(StandardCharsets.UTF_8));
        out.flush();
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
        try {
            out.close();
        } catch (IOException ignore) {
        }
    }
}
