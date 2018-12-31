package javagrinko.spring.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TcpConnection implements Connection {
    private ObjectInputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;
    private List<Listener> listeners = new ArrayList<>();

    public TcpConnection(Socket socket) {
        this.socket = socket;
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    @Override
    public void send(Object objectToSend) {
        if (objectToSend instanceof byte[]) {
            byte[] data = (byte[]) objectToSend;
            try {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void start() {
        new Thread(() -> {
            while (true) {
                byte buf[] = new byte[64 * 1024];
                try {
                    Object obj = inputStream.readObject();
                    if (obj != null) {
                        for (Listener listener : listeners) {
                            listener.messageReceived(this, obj);
                        }
                    } else {
                        socket.close();
                        for (Listener listener : listeners) {
                            listener.disconnected(this);
                        }
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    for (Listener listener : listeners) {
                        listener.disconnected(this);
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
