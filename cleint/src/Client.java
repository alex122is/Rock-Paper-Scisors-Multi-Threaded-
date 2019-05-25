import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.ArrayList;


public class Client {
	private int max = 6; // change max to allow more players
    private int port;
    private InetAddress IP;
    Connection connection = new Connection();
    private boolean validSettings = false;
    private Consumer<String> callback;
    boolean connected = false;
    
    boolean challenge = false;
    boolean endGame = false; 
    
    int p1Score, p2Score, p3Score;
    int numPlayers;
    int myPlayerID;
    String p1Move, p2Move, p3Move;
    String returnThisString ="";
    ArrayList activePlayers = new ArrayList();

    //default constructor
    public Client(Consumer<String> callback) {
        this.callback = callback;
    }

    //get and set of port
    public int getPort() {
        return port;
    }
    public void setPort(int newPort) {
        this.port = newPort;
    }

    //get and set of IP
    public InetAddress getIP() {
        return IP;
    }
    public void setIP(InetAddress newIP) {
        this.IP = newIP;
    }

    //get and set of validsettings
    public boolean isValid() {
        return validSettings;
    }
    public void setValid(boolean valid) {
        this.validSettings = valid;
    }

    //send data
    public void sendInfo(Serializable data) {
        try {
            connection.output.writeObject(data);
        }
        catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Issue writing out of client");
        }
    }

    //start connection
    public void startConnection() throws Exception {
        connection.start();
    }

    //stop connection
    public void stopConnection() throws Exception {
        this.connected = false;
        if (connection.s!= null) {
            if (connection.input != null) {
                connection.input.close();
            }
            if (connection.output != null) {
                connection.output.close();
            }
            connection.s.close();
        }
    }




    //inner class
    class Connection extends Thread {
        Socket s;
        ObjectInputStream input;
        ObjectOutputStream output;

        public void run() {
            try {
                s = new Socket(getIP(),getPort());

                output = new ObjectOutputStream(s.getOutputStream());
                input = new ObjectInputStream(s.getInputStream());
                s.setTcpNoDelay(true);

                System.out.println("New connection client created.");

                //take in input
                while(connected) {

                	// match msg with the keywords: numPlayer,challenge,gameInformation to perform different code.
                	String msg =  (String) input.readObject(); 
                	
                	// update numPlayer,and players who is available
                    if( msg.compareTo("numPlayer") == 0)
                    {
                        activePlayers.clear();

                    	Serializable players = (Serializable) input.readObject();
                    	numPlayers = (Integer) players;

                    	for (int i=0; i < max; i++) {
                    		Serializable areTheyPlaying = (Serializable) input.readObject();
                    		activePlayers.add(areTheyPlaying);
                    	}

                    	Serializable myID = (Serializable) input.readObject();
                    	myPlayerID = (Integer) myID;
                    	callback.accept("Changes made");
                    }
                    
                    //receive a challenge
                    else if(msg.compareTo("challenge") ==0)
                    {
                    	String msg2 = (String) input.readObject();
                    	challenge = true;
                    	callback.accept(msg2);
                    }
                    
                    //a round ends, receive result from server
                    else if(msg.compareTo("gameInformation") ==0)
                    {   
                        Serializable playerInfo = (Serializable) input.readObject();
                        returnThisString = (String) playerInfo;
                        endGame = true;
                    	callback.accept(returnThisString);
                    }
                    //activePlayers.clear(); //call activePlayer.clear() in callback
                }
            }
            catch (Exception e) {
                if (s !=null) {
                    System.out.println("Client was closed.");
                }
                else {
//                    e.printStackTrace();
                    System.out.println("Client could not find server and was closed.");
                }

            }//end of try/catch


        }//end of run

    }//end of connection class
}