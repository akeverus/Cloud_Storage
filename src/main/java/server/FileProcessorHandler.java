package server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

public class FileProcessorHandler implements Runnable {

    private File serverDir;
    private File clientDir;
    private static final int SIZE = 256;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        serverDir = new File("serverDir");
        clientDir = new File(System.getProperty("user.home"));
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("Received: " + command);
                if (command.equals("#SEND#FILE#")) {
                    String fileName = is.readUTF();
                    long size = is.readLong();
                    System.out.println("Created file: " + fileName);
                    System.out.println("File size: " + size);
                    Path currentPath = serverDir.toPath().resolve(fileName);
                    try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {
                        for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0, read);
                        }
                    }
                    os.writeUTF("File successfully uploaded");
                    os.flush();
                }
                if (command.equals("#RECEIVE#FILE#")) {
                    String fileName = is.readUTF();
                    long size = is.readLong();
                    System.out.println("Created file: " + fileName);
                    System.out.println("File size: " + size);
                    Path currentPath = clientDir.toPath().resolve(fileName);
                    try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {
                        for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0, read);
                        }
                    }
                    os.writeUTF("File successfully uploaded");
                    os.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
