package server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class Server {
    private int port;
    private ServerSocket server_socket;
    boolean active = false;
    boolean validPort = false;
    int p1Score = 0;
    int p2Score = 0;
    //int p3Score = 0;
    volatile ArrayList<Integer> playerScores;	//new to this version
    volatile ArrayList<String>  playerMoves;	//new to this version
    String p1Move;
    String p2Move;
    //String p3Move;
    Consumer<Serializable> callback;
    boolean gameOver = false;
    String winner;
    int numPlayers = 0;
    int maxPlayers = 6; //// change maxPlayers to allow more players
    int clientIDs = 0;
    //store the active connections
    ArrayList<Connection> connectionList = new ArrayList<Connection>();
    public Server(Consumer<Serializable> callback) {
        this.callback = callback;
        playerScores = new ArrayList<Integer>(); //can now handle any amount of players
        playerMoves = new ArrayList<String>();
        for(int i = 0; i < maxPlayers; i++)
        {
        	playerScores.add(0);
            playerMoves.add("");
        }
   }

    //get the port of the server
    int getPort() {
        return port;
    }

    //set the port of the server
    void setPort(int port) {
        if (!active) {
            this.port = port;
        }
        else {
            System.out.println("Cannot change port because server is already on");
        }
    }

    //get status of server
    boolean getActive() {
        return active;
    }

    //starts looking for connections via Client subclass
    void turnOnServer() {
        this.active = true;
        try {server_socket = new ServerSocket(port); }
        catch (Exception e) { e.printStackTrace(); }
        //creates four players
        for (int i=0; i < maxPlayers; i++) {
            Connection newClient = new Connection();
//            newClient.playerID = clientIDs; // because we do not which thread runs first, so the playerID will be assigned randomly
//            clientIDs++;
            connectionList.add(newClient);
            newClient.start();
        }
    }
   //close all of the existing threads connected to the server
    void turnOffServer() throws Exception {
        for (int i = 0; i < connectionList.size(); i++) {
            if (connectionList.get(i).s != null) {
                if (connectionList.get(i).output !=null && connectionList.get(i).input.read() != -1) {
                    connectionList.get(i).output.close();
                }
                if (connectionList.get(i).input !=null && connectionList.get(i).input.read() != -1) {
                    connectionList.get(i).input.close();
                }
                connectionList.get(i).s.close();
            }
        }
        if (server_socket !=null ) {
            server_socket.close();
        }
        this.active = false;
        System.out.println("All connections closed.");
    }
    //helper method to get a socket;
    private Socket receiveClient() {
        try {
            return server_socket.accept();
        }
        catch (Exception e) {
            System.out.println("Server socket closed before able to accept.");
        }
        return null;
    }

    //inner class for connecting server with threads
    class Connection extends Thread {
        Socket s;
        ObjectInputStream input;
        ObjectOutputStream output;
        String ClientMove;
        String msg = null;
        boolean madeMove = false;
        int playerScore = 0;
        int playerID;
        int currentOpponent = -1;
        volatile boolean alreadyInGame = false;
        
        public void reset() {
        	this.msg = null;
        	madeMove = false;
        	alreadyInGame = false;
        	ClientMove = null;
        	currentOpponent = -1;
        }
        public void run() {
            try {
                //get a socket from server.accept()
                s = receiveClient();
                //update the input and output streams
                if (s != null) {
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    this.output = out;
                    this.input = in;
                    //starts at 0, goes up to numPlayers-1
                    playerID = connectionList.indexOf(this);
                    numPlayers++;
                    updateClients(); // TODO: write a function to only send numPlayers and players are !already in game
                    callback.accept("Found connection");
                     this.msg = null;
                     
                     //match msg with keywords: challenge,accept,return
                    while(true) {
                    	if( this.msg ==null)
                          msg = (String) in.readObject(); //read someone challenging someone else, or accepting someone else's challenge
                        if( msg.compareTo("challenge") == 0) {
                        	Serializable challenge = (Serializable) in.readObject();
                        	if(alreadyInGame == false) //user is challenging someone else            		
                        	{
                        		alreadyInGame = true;
                        		currentOpponent = (int)challenge; //set current opponent the the user that you challenged
                          		Serializable c = (Serializable)("Player"+ Integer.toString(playerID)+ " is challenging player"+ Integer.toString(currentOpponent));
                          	    callback.accept(c);
                        		connectionList.get(currentOpponent).alreadyInGame = true;  //set opponent's current opponent to you
                        		connectionList.get(currentOpponent).currentOpponent = playerID; //after that, get ready to read the user's moves
                        		updateClients(); 
                        		
                        		Connection opp = connectionList.get(currentOpponent);
                        		//notify other user that they're in a game
                        		opp.output.writeObject("challenge");
                        		opp.output.writeObject("Player"+Integer.toString(playerID)+"is challenging you");
                        		
                        		Serializable data = (Serializable) in.readObject();
                        		this.ClientMove = (String) data;
                        		this.madeMove = true;
                        		Serializable m = (Serializable) ("Player"+ this.playerID+" made move "+this.ClientMove);
                        		callback.accept(m);                             
                        		calculateRound(this.playerID, this.currentOpponent); //if you are not versing anyone, currentOpponent is -1, go to calculateRound to see why
                        	}
                        	else //opponent has not accepted or made a move yet
                        	{
                                calculateRound(this.playerID, this.currentOpponent);
                                //if you are not versing anyone, currentOpponent is -1, go to calculateRound to see why
                        	}
                        	
                        }
                        
                        else if(msg.compareTo("accept") == 0) { // accept a challenge
                        	if(madeMove == false) {
                        		Serializable data = (Serializable) in.readObject();
                        		this.ClientMove = (String) data;
                        		this.madeMove = true;
                        		Serializable m = (Serializable) ("Player"+ this.playerID+" made move "+this.ClientMove);
                        		callback.accept(m);
                        	}else {
                        	   calculateRound(playerID, currentOpponent); //if you are not versing anyone, currentOpponent is -1, go to calculateRound to see why
                        	}
                        }
                        
                        else if(msg.compareTo("return") == 0) { // return to challenge page
                        	updateClients();
                        	msg = null;
                        	continue;
                        }
                    }
                } //end of if not null
            } //end of try
            catch (Exception e) {
                System.out.println("A player has disappeared");
                s = null;
                //numPlayers = -1; //holder for if we were to lose a player, then we are going to end game
                numPlayers--; //new to this version
                //BELOW NEEDS TO BE CHANGED
                updateClients();
            } //end of catch
        } //end of run
    } //end of connection method

    //calculates who won the round, and the game
    private synchronized void calculateRound(int player1, int player2) throws IOException { //now takes the ID's of the two players, so it knows who to check
        
    	if (	player2 != -1 //if player2 is -1, then there is not a game currently going on, this if statement shorts out
                && (connectionList.get(player1).playerScore != 3 && connectionList.get(player2).playerScore != 3)
                && numPlayers >1)
    	{
        	int w = -99;
            //if anyone has not made a move yet, do nothing
            if (!connectionList.get(player1).madeMove || !connectionList.get(player2).madeMove) {
                return;
            }
            else if (connectionList.get(player1).ClientMove.equals("Rock")) {
                switch (connectionList.get(player2).ClientMove) {
                    case "Rock":
                    	w= -99;
                        break;
                    case "Paper":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Scissors":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Lizard":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Spock":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                }
            }
            else if (connectionList.get(player1).ClientMove.equals("Paper")) {
                switch (connectionList.get(player2).ClientMove) {
                    case "Rock":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Paper":
                    	w= -99;
                        break;
                    case "Scissors":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Lizard":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Spock":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                }
            }
            else if (connectionList.get(player1).ClientMove.equals("Scissors")) {
                switch (connectionList.get(player2).ClientMove) {
                    case "Rock":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Paper":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Scissors":
                    	w= -99;
                        break;
                    case "Lizard":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Spock":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                }
            }
            else if (connectionList.get(player1).ClientMove.equals("Lizard")) {
                switch (connectionList.get(player2).ClientMove) {
                    case "Rock":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Paper":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Scissors":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Lizard":
                    	w= -99;
                        break;
                    case "Spock":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                }
            }
            else if (connectionList.get(player1).ClientMove.equals("Spock")) {
                switch (connectionList.get(player2).ClientMove) {
                    case "Rock":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Paper":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Scissors":
                    	w= player1;
                        connectionList.get(player1).playerScore++;
                        break;
                    case "Lizard":
                    	w= player2;
                        connectionList.get(player2).playerScore++;
                        break;
                    case "Spock":
                    	w= -99;
                        break;
                }
            }

            //reset so they need to make moves again
            connectionList.get(player1).madeMove = false;
            connectionList.get(player2).madeMove = false;
//
//
//
//            //update the client's version of the scores
//            p1Score = connectionList.get(player1).playerScore;
//            p2Score = connectionList.get(player2).playerScore;
//            p1Move = connectionList.get(player1).ClientMove;
//            p2Move = connectionList.get(player2).ClientMove;
//            callback.accept("Moves have been updated");
//            if (connectionList.get(player1).playerScore == 3) {
//                winner = "Player " + String.valueOf(player1);
//                gameOver = true;
//            }
//            else if (connectionList.get(player2).playerScore == 3) {
//                winner = "Player " + String.valueOf(player2);
//                gameOver = true;
//            }
            
            updatePlayerInformation(player1,player2,w); //server knows which players to send data to this way
        } //end of checking if 6 case statement
    	
        else if (connectionList.get(player1).playerScore == 3) {
            if (connectionList.get(player1).ClientMove.equals("Play")) {
                //when you click play again, you elicit this so everyone is available to play
                for (Connection conn : connectionList) {
                    conn.alreadyInGame = false;
                }
                connectionList.get(player1).playerScore = 0;
                connectionList.get(player2).playerScore = 0;
                connectionList.get(player1).madeMove = false;
                connectionList.get(player2).madeMove = false;
                playerScores.set(player1, 0);
                playerScores.set(player2, 0);
                ///p1Score = 0;
                //p2Score = 0;
                winner = "Playing again";
                callback.accept("replay");
            }
            else {
                System.out.println("Winner is already P1");
            }
        }
        else if (connectionList.get(player2).playerScore == 3) {
            if (connectionList.get(player2).ClientMove.equals("Play")) {
                connectionList.get(player1).playerScore = 0;
                connectionList.get(player2).playerScore = 0;
                connectionList.get(player1).madeMove = false;
                connectionList.get(player2).madeMove = false;
                playerScores.set(player1, 0);
                playerScores.set(player2, 0);
                //p1Score = 0;
                //p2Score = 0;
                winner = "Playing again";
                callback.accept("replay");
            }
            else {
                System.out.println("Winner is already P2");
            }
        }
    } //end of calculateround
    
    // send the gameInformation to the players who are in the game only
    private synchronized void updatePlayerInformation(int p1,int p2, int winner) throws IOException {
    	String win;
    	if(winner < 0)
    		win = "\nDraw";
    	else
    		win = "\n Winner : Player"+Integer.toString(winner);
    	String playerInformation = "";
		//playerInformation +=("Player "+ Integer.toString(p1) + " challanges " + "Player " +Integer.toString(p2));
		playerInformation +=("\nPlayer"+Integer.toString(p1)  +"'s move: " + connectionList.get(p1).ClientMove);
		playerInformation +=("\nPlayer"+Integer.toString(p2) +"'s move: "+connectionList.get(p2).ClientMove);
		playerInformation += win;
		
		//send playerInformation to p1,p2,serverGUI, and reset p1 and p2.
		Serializable msg = (Serializable)playerInformation;
		callback.accept(msg);
		connectionList.get(p1).reset();
		connectionList.get(p2).reset();
		connectionList.get(p1).output.writeObject("gameInformation");
		connectionList.get(p1).output.writeObject(playerInformation);
		connectionList.get(p2).output.writeObject("gameInformation");
		connectionList.get(p2).output.writeObject(playerInformation);
    }

    // update numPlayers
    private synchronized void updateClients() {
        try {          
        	for (Connection conn : connectionList) { //update everyone
        		if (conn != null) {
                    if (conn.s != null) {
                    	conn.output.writeObject("numPlayer");
                        conn.output.writeObject(numPlayers);		//send how many players are connected
                        for(int i = 0; i < maxPlayers; i++)
                        {	
                        	if(connectionList.get(i) == null || connectionList.get(i).s == null)
                        	{
                        		conn.output.writeObject(true);
                        		continue;
                        	}
                            conn.output.writeObject(connectionList.get(i).alreadyInGame); //send all users information regarding whether or not a user is in a game already
                        }
                        conn.output.writeObject(conn.playerID); //write the connection's playerID
                    }
                }
            }
        }

        
        catch (Exception e) {

            e.printStackTrace();

            System.out.println("A client has closed.");

        }

    } //end of updateClients







}