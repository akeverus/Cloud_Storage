package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioEchoServer {

    /**
     * Сделать терминал, которые умеет обрабатывать команды:
     * ls - список файлов в директории
     * cd dir_name - переместиться в директорию
     * cat file_name - распечатать содержание файла на экран
     * mkdir dir_name - создать директорию в текущей
     * touch file_name - создать пустой файл в текущей директории
     */

    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final ByteBuffer buf;

    public NioEchoServer() throws IOException {
        buf = ByteBuffer.allocate(5);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(8190));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started...");
        while (serverChannel.isOpen()) {
            selector.select(); // block
            System.out.println("Keys selected...");
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder s = new StringBuilder();
        int read = 0;
        buf.clear();
        while (true) {
            read = channel.read(buf);
            if (read == 0) {
                break;
            }
            if (read < 0) {
                channel.close();
                return;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear();
        }
        process(s.toString(), channel);
        System.out.println("Received: " + s);
        channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    private void process(String s, SocketChannel channel) throws IOException {
        Path workingDir = Paths.get("serverDir");
        if (s.equals("ls")) {
            List<String> filesWorkingDir = Files.readAllLines(workingDir);
            for (String str : filesWorkingDir) {
                channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.US_ASCII)));
            }
        } else if (s.startsWith("cd")) {
            String[] str = s.split(" ");
            Path path = Paths.get(workingDir + "//" + str[1]);
            if (Files.exists(path)) {
                workingDir = path;
            }
        } else if (s.startsWith("cat")) {
            String[] str = s.split(" ");
            Path path = Paths.get(workingDir + "//" + str[1]);
            channel.write(ByteBuffer.wrap(Files.readAllBytes(path)));
        } else if (s.startsWith("mkdir")) {
            String[] str = s.split(" ");
            Path newDir = Paths.get(workingDir + "//" + str[1]);
            if (!Files.exists(newDir)) {
                Files.createDirectory(workingDir);
            }
        } else if (s.startsWith("touch")) {
            String[] str = s.split(" ");
            Path path = Paths.get(workingDir + "//" + str[1]);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap(
                "Hello user. Welcome to our terminal\n\r".getBytes(StandardCharsets.US_ASCII)
        ));
        System.out.println("Client accepted...");
    }

    public static void main(String[] args) throws IOException {
        new NioEchoServer();
    }
}
