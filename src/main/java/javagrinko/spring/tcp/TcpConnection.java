package javagrinko.spring.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TcpConnection implements Connection {
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private List<Listener> listeners = new ArrayList<>();
    private static Log logger = LogFactory.getLog(TcpConnection.class);

    public TcpConnection(Socket socket) {
        this.socket = socket;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
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
        try {
            outputStream.writeObject(objectToSend);
        } catch (IOException e) {
            e.printStackTrace();
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
                    logger.error(e.getMessage());
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
