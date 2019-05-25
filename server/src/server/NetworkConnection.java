package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public abstract class NetworkConnection {

    abstract boolean isServer();
    abstract String getIP();
    abstract int getPort();

    //set up the connection and create the consumer for callback
    connection_thread connection = new connection_thread();
    Consumer<Serializable> callback;

    //default constructor
    public NetworkConnection(Consumer<Serializable> callback) {
        this.callback = callback;
    }

    //start the connection of the connection thread
    public void startConnection() throws Exception {
        connection.start();
    }

    //send information to the output stream
    public void send(Serializable data) throws Exception {
        connection.output.writeObject(data);
    }

    //close the connection
    public void closeConnection() throws Exception {
        connection.socket.close();
    }

    //the thread class for the connection
    class connection_thread extends Thread {
        ServerSocket server;
        Socket socket;
        ObjectOutputStream output;
        ObjectInputStream input;

        //needs to be implemented when extending off thread
        public void run() {
            //if it is a server, then make a server socket. else, do nothing on run
            if (isServer()) {
                try {
                    ServerSocket server = new ServerSocket(getPort());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Issue with server generation");
                }
            }
//            try {
//               //if server, make new server socket. else, do nothing
//               ServerSocket server = isServer() ? new ServerSocket(getPort()) : null;
//               //if server, look for connection. else, make new socket
//               Socket socket = isServer() ? server.accept() : new Socket(getIP(), getPort());
//
//               //
//               ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//               ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//
//               this.server = server;
//               this.socket = socket;
//               this.output = out;
//               this.input = in;
//
//               //disable nagle's algorithm so we don't wait before sending data
//                socket.setTcpNoDelay(true);
//
//                //this will take the object, serialize it and then put into callback
//                while (true) {
//                    Serializable data = (Serializable) in.readObject();
//                    callback.accept(data);
//                }
//
//            }//end of try
//
//            catch (Exception e){
//                e.printStackTrace();
//                callback.accept("Connection closed.");
//            }//end of catch
        }
    }
}

