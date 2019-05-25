import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.scene.text.*;
import javafx.scene.image.*;

import java.net.InetAddress;


public class Main extends Application {
	private int max = 6; // change max to allow more players
    private Text playersA = new Text();
    private Stage welcomeStage, playStage, challengeStage;
    private Text welcomeText, portInputText, ipInputText;
    private Button systemsButton, playButton, closeButton,accept,again;
    private Button Rock, Paper, Scissors, Lizard, Spock;
    private TextField portRequest, ipRequest;
    //    private Text selfScoreText, theirScoreText;
    private TextField challengePlayer = new TextField("Input player ID and enter to challenge");
    private Text gameState = new Text("");
    private Text currentPlayers = new Text("Current # Players:");
    private Text myPlayerID = new Text("You are player:");
    private int portNumber;
    private InetAddress ipNumber;
    private Text winner = new Text("Winner: ");
    private Text lonePlayer = new Text("Someone has left. Close client.");
    private Button playAgainButton = new Button("Play again");
    private Text players = new Text("Num Players:");

    private Image rock = new Image("rock.jpeg",75,75,true, true);
    private Image paper = new Image("paper.jpeg", 75, 75, true, true);
    private Image scissors = new Image("scissors.jpeg", 50, 50, true, true);
    private Image lizard = new Image("lizard.jpeg", 75, 75, true, true);
    private Image spock = new Image("spock.jpeg", 75, 75, true, true);

    private Client thisClient = createClient();

    @Override
    public void start(Stage primaryStage) throws Exception{

        //////////////////          welcome page            //////////////////

        welcomeStage = primaryStage;
        welcomeStage.setTitle("This is the client.");
        BorderPane welcomePane = new BorderPane();
        Scene welcomeScene = new Scene(welcomePane, 400,600);

        welcomeText = new Text("Client - RSPLS");
        welcomeText.setFont(Font.font("Verdana", 20));

        portRequest = new TextField("Input desired port here");
        ipRequest = new TextField("Input desired IP here");
        portRequest.setMaxWidth(200);
        ipRequest.setMaxWidth(200);

        portInputText = new Text("Port: ");
        ipInputText = new Text("IP: ");

        systemsButton = new Button("Confirm settings");
        playButton = new Button("Let's play!");
        closeButton = new Button("End client");
        accept = new Button("accept");
        again = new Button("return");
        accept.setVisible(false);
        again.setVisible(false);
        
        VBox Cproperties = new VBox(welcomeText, portRequest,ipRequest, systemsButton, playButton, portInputText, ipInputText, closeButton);
        Cproperties.setSpacing(10);
        Cproperties.setAlignment(Pos.CENTER);

        welcomePane.setTop(Cproperties);
        welcomeStage.setScene(welcomeScene);

        //////////////////          play page            //////////////////
        playStage = new Stage();
        playStage.setTitle("Game in progress...");
        BorderPane playPane = new BorderPane();
        Scene playScene = new Scene(playPane, 400,600);

        Rock = new Button("Rock");
        Paper = new Button("Paper");
        Scissors = new Button("Scissors");
        Lizard = new Button("Lizard");
        Spock = new Button("Spock");

        Rock.setGraphic(new ImageView(rock));
        Paper.setGraphic(new ImageView(paper));
        Scissors.setGraphic(new ImageView(scissors));
        Lizard.setGraphic(new ImageView(lizard));
        Spock.setGraphic(new ImageView(spock));


//        VBox moves = new VBox(Rock,Paper,Scissors,Lizard,Spock, theirMove, selfScoreText,theirScoreText, winner, closeButton, playAgainButton);
        VBox moves = new VBox(Rock,Paper,Scissors,Lizard,Spock, gameState, winner, closeButton,again, playAgainButton);
        moves.setSpacing(10);
        moves.setAlignment(Pos.CENTER);

        playPane.setCenter(moves);
        playAgainButton.setVisible(false);

        //////////////////          challenge page            ///////////////////
        challengeStage = new Stage();
        challengeStage.setTitle("Choose your challenger");
        BorderPane challengePane = new BorderPane();
        Scene challengeScene = new Scene(challengePane,400,600);
        challengePlayer.setMaxWidth(300);

        VBox challenger = new VBox(myPlayerID, currentPlayers, playersA,accept, challengePlayer, closeButton);
        challenger.setSpacing(20);
        challenger.setAlignment(Pos.CENTER);

        challengePane.setCenter(challenger);

        //sets the buttons for the client
        accept.setOnAction( event->{
        	accept.setVisible(false);
            thisClient.sendInfo("accept");
        	primaryStage.setScene(playScene);
        });
        again.setOnAction(event->{
        	thisClient.returnThisString = "";
        	gameState.setText("");
            thisClient.sendInfo("return");
        	again.setVisible(false);
        	primaryStage.setScene(challengeScene);
        });
        
        systemsButton.setOnAction(event -> {
            try {
                portNumber = Integer.parseInt(portRequest.getText());
                ipNumber = InetAddress.getByName(ipRequest.getText());

                portInputText.setText("Port: " + portNumber);
                ipInputText.setText("IP: " + ipNumber);

                thisClient.setPort(portNumber);
                thisClient.setIP(ipNumber);


                thisClient.setValid(true);
                System.out.println("Port: " + thisClient.getPort() + " IP: " + thisClient.getIP());
            }
            catch (Exception e) {
                portInputText.setText("Port: " + " needs to be a number");
                ipInputText.setText("IP: " + "needs to be an address");
            }
        });

        playButton.setOnAction(event -> {
            if(thisClient.isValid()) {
                try {
                    thisClient.startConnection();
                    thisClient.connected = true;
                    primaryStage.setScene(challengeScene);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Issue starting the connection. Valid settings.");
                }
            }
        });

        closeButton.setOnAction(event -> {
            try {
                thisClient.stopConnection();
                primaryStage.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Issue with closing the connection of the client.");
            }
        });


        challengePlayer.setOnAction(e -> {
            try {
                Integer i = Integer.parseInt(challengePlayer.getText());
            	System.out.println("1");
            	
                if( (boolean)thisClient.activePlayers.get(i) ==true) {
                	System.out.println("3");
                	challengePlayer.appendText("Player"+i+" alreay in a game");
                }
                else {
                thisClient.sendInfo("challenge");
                thisClient.sendInfo(i);
                primaryStage.setScene(playScene);
                }
            }
            catch (Exception a){
                a.printStackTrace();
                challengePlayer.setText("Needs to be a number");
            }

        });

        Rock.setOnAction(event -> {
            thisClient.sendInfo("Rock");
        });

        Paper.setOnAction(event -> {
            thisClient.sendInfo("Paper");
        });

        Scissors.setOnAction(event -> {
            thisClient.sendInfo("Scissors");
        });

        Lizard.setOnAction(event -> {
            thisClient.sendInfo("Lizard");
        });

        Spock.setOnAction(event -> {
            thisClient.sendInfo("Spock");
        });

        playAgainButton.setOnAction(event -> {
            thisClient.sendInfo("Play");

            winner.setText("Winner: ");
            primaryStage.setScene(challengeScene);

            playAgainButton.setVisible(false);
        });


        primaryStage.show();
    }

    private Client createClient() {
        return new Client(data -> {
            Platform.runLater(() -> {
            	
                //returns the whole string of players' scores and their moves
                gameState.setText(thisClient.returnThisString);
                //return the number of players
                currentPlayers.setText("Current # Players:" + thisClient.numPlayers);

                //return your ID so you cannot challenge yourself
                myPlayerID.setText("You are player: " + thisClient.myPlayerID);
                
                playersA.setText("");
                int x=0;
                String str= "Players Available to challenge: \n";
                for( int i = 0;i < max ;i++) {
                		if( i >= thisClient.activePlayers.size())
                			break;
                		if((boolean)thisClient.activePlayers.get(i) == false) {
                			if(thisClient.myPlayerID == i) {//dont challenge yourself

                			}
                			else {
                			str+= (Integer.toString(i)+", ");
                			}
                	}	
                }
                
                //receive a challenge
                if(thisClient.challenge == true) 
                {
                   // playersA.setText("You are Being challenged Press Accept");
                	accept.setVisible(true);
                	str+=("\n"+data);
                	thisClient.challenge = false;
                }
                
                //  game ends
                if(thisClient.endGame == true) 
                {
                	again.setVisible(true);
                	thisClient.endGame = false;
                }
//                if (x ==0) {
//                    playersA.setText("You are Being challenged Press Accept");
//                }
//                else {
                playersA.setText(str);
                
//                thisClient.activePlayers.clear();
                
                if (thisClient.numPlayers == -1) {
                    winner.setText("A player left. Please close client.");
                }
     

//                if (selfScore == 3) {
//                    winner.setText("Winner: Me!!");
//                    playAgainButton.setVisible(true);
//                }
//                else if (theirScore == 3) {
//                    winner.setText("Winner: Other guy...");
//                    playAgainButton.setVisible(true);
//                }
            });
        });
    }


    public static void main(String[] args) {
        launch(args);
    }


}