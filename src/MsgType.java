import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public final class MsgType {
    public static final int FETCH_AVAILABLE_SENDER = 0;
    public static final int REGISTER_SENDER = 1;
    public static final int QUERY_SENDER_SERVER_ADDRESS = 2;

    public static class Pair<T, E> {
        public T first;
        public E second;

        public Pair(T first, E second) {
            this.first = first;
            this.second = second;
        }
    }


    /**
     * "获取可用的文件发送者"消息<br>
     * 消息内容格式(request): 无
     * 消息内容格式(response): 可用文件发送者数量(int, 2) + (文件发送者数量) * (连接号码(int, 4) + 文件名长度(int, 3) + 文件名(str, 文件名长度))
     */
    public static class FetchAvailableSenders {
        /**
         * InputStream 在此方法被调用时, 已经读取了消息类型码, 消息类直接对消息内容进行读取
         *
         * @return 连接号码(int):文件名(str) 对的列表
         */
        public static @NotNull HashMap<Integer, String> parseMsg(@NotNull InputStream is) throws IOException {
            byte[] cntBytes = is.readNBytes(2);
            int cnt = Integer.parseInt(new String(cntBytes));
            HashMap<Integer, String> senders = new HashMap<>();
            for (int i = 0; i < cnt; i++) {
                byte[] connBytes = is.readNBytes(4);
                int conn = Integer.parseInt(new String(connBytes, StandardCharsets.UTF_8));
                byte[] nameLenBytes = is.readNBytes(3);
                int nameLen = Integer.parseInt(new String(nameLenBytes, StandardCharsets.UTF_8));
                byte[] nameBytes = is.readNBytes(nameLen);
                senders.put(conn, new String(nameBytes, StandardCharsets.UTF_8));
            }
            return senders;
        }
    }

    /**
     * "注册成为文件发送者" 消息<br>
     * 此消息获取消息回复后需要即刻启动文件传输服务器, 若申请多次则会覆盖前面的申请<br>
     * 消息内容格式(request): 文件名长度(int, 3) + 文件名(str, 文件名长度) + 端口号(int, 5)
     * 消息内容格式(response): 连接号码(int, 4)
     */
    public static class RegisterSender {
        /**
         * InputStream 在此方法被调用时, 已经读取了消息类型码, 消息类直接对消息内容进行读取
         *
         * @return 连接号码(int)
         */
        public static int parseMsg(@NotNull InputStream is) throws IOException {
            byte[] connCodeBytes = is.readNBytes(4);
            return Integer.parseInt(new String(connCodeBytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * "获取文件发送者服务器地址" 消息<br>
     * 消息内容格式(request): 连接号码(int, 4)<br>
     * 消息内容格式(response): IP长度(int, 2) + IP(str, IP长度) + 端口号(int, 5)<br>
     * 特别地, 当查询不到目标发送者时, 消息内容格式(response): 00
     */
    public static class QuerySenderServerAddress {
        /**
         * InputStream 在此方法被调用时, 已经读取了消息类型码, 消息类直接对消息内容进行读取
         *
         * @return Pair(str, int) | null(查询不到目标发送者)
         */
        public static @Nullable Pair<String, Integer> parseMsg(@NotNull InputStream is) throws IOException {
            byte[] ipLengthBytes = is.readNBytes(2);
            int ipLength = Integer.parseInt(new String(ipLengthBytes, StandardCharsets.UTF_8));
            if (ipLength > 0) {
                String ip = new String(is.readNBytes(ipLength), StandardCharsets.UTF_8);
                int port = Integer.parseInt(new String(is.readNBytes(5), StandardCharsets.UTF_8));
                return new Pair<>(ip, port);
            } else {
                return null;
            }
        }
    }
}
