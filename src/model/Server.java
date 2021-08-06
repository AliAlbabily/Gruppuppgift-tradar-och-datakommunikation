package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private Clients globalOnlineUsers = new Clients(); // lista av anslutna användare (online users)

    public Server(int port) throws IOException {
        new Connection(port).start();
    }

    private class Connection extends Thread {
        private int port;

        public Connection(int port) {
            this.port = port;
        }

        public void run() {
            Socket socket = null;
            System.out.println("Server startad");
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while(true) {
                    try {
                        socket = serverSocket.accept(); // Lyssna efter anslutande klienter
                        new ClientHandler(socket).start();
                    } catch(IOException e) {
                        System.err.println(e);
                        if(socket != null)
                            socket.close();
                    }
                }
            } catch(IOException e) {
                System.err.println(e);
            }
            System.out.println("Server stoppad");
        }
    }

    public class ClientHandler extends Thread {
        private Socket socket;
        ObjectOutputStream oos;
        ObjectInputStream ois;

        User key = null;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            System.out.println("A new client has connected to the server!");
        }

        public void run() {
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());

                while(true) {
                    Object objReceived = ois.readObject();

                    if(objReceived instanceof User) {
                        key = (User)objReceived;
                        globalOnlineUsers.put((User)objReceived, this); // spara en referens av ClientHandler i hashmapen med sin motsvarande User
                        updateOnlineUsersList();
                    }
                    else if(objReceived instanceof Message) {
                        Message messageReceived = (Message)objReceived;
                        sendMessageToReceivers(messageReceived);
                    }

                }
            }
            catch (IOException | ClassNotFoundException e) {
                try {
                    socket.close();
                    System.out.println("Client disconnected!");
                    globalOnlineUsers.getHashMapList().remove(key); // tar bort användaren som avslutar sin anslutning från listan
                    updateOnlineUsersList();

                } catch (Exception e2) {}
            }
        }

        public ObjectOutputStream getOos() {
            return oos;
        }
    }

    public static void main(String[] args) {
        try {
            new Server(2343);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // meddela andra klienter om nuvarande anslutna användare
    private void updateOnlineUsersList() throws IOException {
        ArrayList<User> onlineUsers = new ArrayList<>();

        for ( User key : globalOnlineUsers.getHashMapList().keySet() ) {
            onlineUsers.add(key);
        }

        for ( User key : globalOnlineUsers.getHashMapList().keySet() ) {
            // få tag på alla ClientHandlers i hashmappen
            ClientHandler client = globalOnlineUsers.getHashMapList().get(key);
            // skicka den senaste infon om anslutna användare till alla klienter
            client.getOos().writeObject( onlineUsers );
            client.getOos().flush();
        }
    }

    private void sendMessageToReceivers(Message message) throws IOException {
        User[] receivers = message.getArrayOfReceivers();

        for ( User user : receivers ) {
            ClientHandler client = globalOnlineUsers.getHashMapList().get(user);

            client.getOos().writeObject(message);
            client.getOos().flush();
        }
    }
}