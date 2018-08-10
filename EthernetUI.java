import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EthernetUI extends Application {

    private static EthernetServer server;
    private static List<Byte> bytes = new ArrayList<>();
    private static boolean on = true;
    private static TextArea console;

    public static void main(String... args) {
        Thread reciever = new Thread(() -> {
            while(on){
                System.out.print("");
                if(server != null) {
                    byte[] packet = server.recievePacket();
                    //System.out.println("We Recieved A Packet!!!");
                    //We recieved a packet!
                    boolean isEmpty = true;
                    for (byte by : packet) {
                        if (by != 0) {
                            isEmpty = false;
                            break;
                        }
                    }
                    //Break packet up...
                    if(!isEmpty)
                    dissolvePacket(packet);

                }
            }
        });
        reciever.start();
        launch();
    }

    private static void dissolvePacket(byte[] packet) {
        System.out.println("Dissolving");
        if(console != null) {
            byte firstByte = packet[0];
            //Break into 5, 1, and 2.
            String log = "";
            if(!console.getText().isEmpty()){
                log += "\n";
            }
            log += "Recieved {\n";
            log += "RT: " + ((firstByte & 0xf8) >> 3) + "\n";
            log += "T/R: " + ((firstByte & 0x04) >> 2) + "\n";

            byte secondByte = packet[1];

            log += "SA: " + (((firstByte & 0x03) << 3) | ((secondByte & 0xe0) >> 5)) + "\n";
            int size;
            log += "WC: " + (size = (secondByte & 0x1f)) + "\n";
            log += "DATA {\n";
            for (int i = 2; i < size * 2 + 2; i+=4) {
                log += "HIGH: " + ((((short)packet[i]) << 8)  | packet[i+1]) + "\n";
                log += "LOW: " + ((((short)packet[i+2]) << 8)  | packet[i+3]) + "\n";
            }
            log += "}\n";

            byte thirdByte = packet[2 + size * 2];

            log += "RT: " + ((thirdByte & 0xf8) >> 3) + "\n";
            log += "ME: " + ((thirdByte & 0x04) >> 2) + "\n";
            log += "I: " + ((thirdByte & 0x02) >> 1) + "\n";
            log += "SR: " + (thirdByte & 0x01)+ "\n";

            byte fourthByte = packet[3 + size * 2];

            log += "R: " + ((fourthByte & 0xe0) >> 5) + "\n";
            log += "BCR: " + ((fourthByte & 0x10) >> 4) + "\n";
            log += "Busy: " + ((fourthByte & 0x08) >> 3) + "\n";
            log += "SF: " + ((fourthByte & 0x04) >> 2) + "\n";
            log += "DBCA: " + ((fourthByte & 0x02) >> 1) + "\n";
            log += "TF: " + (fourthByte & 0x01) + "\n";
            log += "}\n";

            String finalLog = log;
            Platform.runLater(() -> console.setText(console.getText() + finalLog));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        VBox container = new VBox();
        container.setMaxWidth(640);
        container.setAlignment(Pos.CENTER);

        HBox commandBox = new HBox();
        commandBox.setAlignment(Pos.CENTER);

        TextField remoteField = new TextField(); //5 bits
        remoteField.setPromptText("RT");
        remoteField.setMaxWidth(200);

        TextField trField = new TextField(); //1 bit
        trField.setPromptText("T/R");
        trField.setMaxWidth(40);

        TextField subAddressField = new TextField(); //5 bits
        subAddressField.setPromptText("Subaddress");
        subAddressField.setMaxWidth(200);

        TextField wordCountField = new TextField(); //5 bits (0 represents 32)
        wordCountField.setPromptText("Word Count");
        wordCountField.setMaxWidth(200);

        commandBox.getChildren().addAll(remoteField, trField, subAddressField, wordCountField);

        TextArea dataArea = new TextArea();
        dataArea.setPromptText("Data");

        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER);

        TextField remoteFieldS = new TextField(); //5 bits
        remoteFieldS.setPromptText("RT");
        remoteFieldS.setMaxWidth(200);

        TextField errorField = new TextField(); //1 bit
        errorField.setPromptText("ME");
        errorField.setMaxWidth(40);

        TextField instrumField = new TextField(); //1 bit
        instrumField.setPromptText("I");
        instrumField.setMaxWidth(40);

        TextField serviceField = new TextField(); //1 bit
        serviceField.setPromptText("SR");
        serviceField.setMaxWidth(40);

        TextField reservedField = new TextField(); //3 bits
        reservedField.setPromptText("Reserved");
        reservedField.setMaxWidth(120);

        TextField broadcastField = new TextField(); //1 bit
        broadcastField.setPromptText("BCR");
        broadcastField.setMaxWidth(40);

        TextField busyField = new TextField(); //1 bit
        busyField.setPromptText("Busy");
        busyField.setMaxWidth(40);

        TextField subSystemField = new TextField(); //1 bit
        subSystemField.setPromptText("SF");
        subSystemField.setMaxWidth(40);

        TextField busControlField = new TextField(); //1 bit
        busControlField.setPromptText("DBCA");
        busControlField.setMaxWidth(40);

        TextField terminalField = new TextField(); //1 bit
        terminalField.setPromptText("TF");
        terminalField.setMaxWidth(40);

        statusBox.getChildren().addAll(remoteFieldS, errorField, instrumField, serviceField, reservedField, broadcastField
        , busyField, subSystemField, busControlField, terminalField);


        container.getChildren().addAll(commandBox, dataArea, statusBox);

        HBox vboxContainer = new HBox();
        vboxContainer.setAlignment(Pos.CENTER);

        VBox sendRecievedBox = new VBox();
        sendRecievedBox.setAlignment(Pos.CENTER);
        sendRecievedBox.setMaxWidth(400);

        console = new TextArea();
        console.setPromptText("Output:");
        console.setEditable(false);

        Button sendButton = new Button("->");
        server = new EthernetServer();
        sendButton.setOnAction(event -> {
            //Send data...
            bytes.clear();
            //getBytesFromNodes(container);
            String text = remoteField.getText();
            byte b1 = Byte.parseByte(text);

            text = trField.getText();
            byte b2 = Byte.parseByte(text);

            text = subAddressField.getText();
            byte b3 = Byte.parseByte(text);

            int comb = ((b1 & 0x1f) << 3) | ((b2 & 0x01) << 2) | ((b3 & 0x18) >> 3);
            byte firstByte = (byte) comb; //The first 8 bits...
            bytes.add(firstByte);

            b1 = b3;

            text = wordCountField.getText();
            b2 = Byte.parseByte(text);

            comb = ((b1 & 0x07) << 5) | (b2 & 0x1f);
            byte secondByte = (byte) comb; //The second 8 bits...
            bytes.add(secondByte);

            String[] texts = dataArea.getText().split("\\n");
            for (String str : texts) {
                try {
                    short sho = Short.parseShort(str);
                    byte[] byteArr = shortToByteArray(sho);
                    b1 = byteArr[0];
                    b2 = byteArr[1];
                    bytes.add(b1);
                    bytes.add(b2);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }

            text = remoteFieldS.getText();
            b1 = Byte.parseByte(text);

            text = errorField.getText();
            b2 = Byte.parseByte(text);

            text = instrumField.getText();
            b3 = Byte.parseByte(text);

            text = serviceField.getText();
            byte b4 = Byte.parseByte(text);

            comb = ((b1 & 0x1f) << 3) | ((b2 & 0x01) << 2) | ((b3 & 0x01) << 1) | (b4 & 0x01);
            byte thirdByte = (byte) comb;
            bytes.add(thirdByte);

            text = reservedField.getText();
            b1 = Byte.parseByte(text);

            text = broadcastField.getText();
            b2 = Byte.parseByte(text);

            text = busyField.getText();
            b3 = Byte.parseByte(text);

            text = subSystemField.getText();
            b4 = Byte.parseByte(text);

            text = busControlField.getText();
            byte b5 = Byte.parseByte(text);

            text = terminalField.getText();
            byte b6 = Byte.parseByte(text);

            comb = ((b1 & 0x07) << 5) | ((b2 & 0x01) << 4)
                    | ((b3 & 0x01) << 3) | ((b4 & 0x01) << 2)
                    | ((b5 & 0x01) << 1) | (b6 & 0x01);
            byte fourthByte = (byte) comb;
            bytes.add(fourthByte);



            bytes.forEach(aByte -> {
                String s1 = String.format("%8s", Integer.toBinaryString(aByte & 0xFF)).replace(' ', '0');
                System.out.println(s1);
            });
            String log;
            if(console.getText() != null && !console.getText().isEmpty()) {
                log = console.getText() + "\nSent {\n";
            } else {
                log = "Sent {\n";
            }
            for(int i = 0; i < bytes.size(); i++) {
                log += String.format("%8s", Integer.toBinaryString(bytes.get(i) & 0xFF)).replace(' ', '0');
                if(++i < bytes.size()) {
                    log += " " + String.format("%8s", Integer.toBinaryString(bytes.get(i) & 0xFF)).replace(' ', '0') + "\n";
                }
            }
            log += "}";

            console.setText(log);
            server.sendPacket(bytes.toArray(new Byte[0]));
        });

        Button unixButton = new Button("UNIX Time");
        unixButton.setOnAction(event -> {
            int unix = (int) Instant.now().getEpochSecond();

            //int low = (((unix << 16) & 0xffff0000) >>> 16);
            short low = (short) (unix & 0xFFFF);
            short high = (short) (unix >>> 16);
            if(low <= 0) {
                dataArea.setText(high + "\n" + (int)low);
            } else {
                dataArea.setText(high + "\n" + low);
            }
        });

        HBox buttonBox = new HBox(unixButton, sendButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);

        //Instant.now().getEpochSecond();
        sendRecievedBox.getChildren().addAll(console, buttonBox);

        vboxContainer.getChildren().addAll(container, new Separator(Orientation.VERTICAL), sendRecievedBox);
        vboxContainer.setBackground(new Background(new BackgroundFill(Color.CORAL, null, null)));

        Scene scene = new Scene(vboxContainer);

        primaryStage.setScene(scene);

        setFills(vboxContainer);

        primaryStage.setOnCloseRequest(event -> {
            on = false;
            primaryStage.close();
        });

        primaryStage.setTitle("Ethernet Server");

        primaryStage.show();
    }

    private void setFills(Node node) {
        if(node instanceof Pane) {
            for (Node child :
                    ((Pane) node).getChildren()) {
                setFills(child);
            }
        }
        else if(node instanceof TextInputControl) {
            node.setOpacity(.9);
        }
    }

    private byte[] shortToByteArray(short s) {
        return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
    }
}
