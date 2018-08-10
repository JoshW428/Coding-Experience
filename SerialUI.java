//these libraries are necessary for running program (java.comm)
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class SerialUI extends Application {

    private static TextArea area;
    private static boolean open = true;
    private static int size;
    private static int count;
    private static String text = "";
    private static List<Byte> bytes = new ArrayList<>();

    public static void main( String... args) {
    
        //set up whihc com port to send and recieve data from
        SerialPort comPort = SerialPort.getCommPort("COM5");
        //open port
        comPort.openPort();
        Thread thread = new Thread(() -> {
            //activates while the COMPORT is open
            while (open) {
                //read incoming data
                if (comPort.bytesAvailable() > 0) {
                    //create a buffer to store incoming bytes
                    byte[] readBuffer = new byte[comPort.bytesAvailable()];
                    int numRead = comPort.readBytes(readBuffer, readBuffer.length);
                    if(count == 0) {
                        text += "Recieved: {\n";
                    }
                    //get size of readBuffer
                    if(count < 2 && readBuffer.length > 1) {
                        size = readBuffer[1 - count] & 0x1f;
                        //System.out.println(size);
                    }
                    //set up buffer array to print out
                    for (int i = 0; i < numRead; i++) {
                        bytes.add(readBuffer[i]);
                        text += String.format("%8s", Integer.toBinaryString(readBuffer[i] & 0xFF)).replace(' ', '0') +"\n";
                    }
                    count += readBuffer.length;
                    if(size > 0 && count >= size * 2 + 4) {
                        if(area != null) {
                            text += "}\n";
                            Platform.runLater(() -> area.setText(area.getText() + "\n" + text));
                        }
                        //organize incoming bytes
                        byte[] bytesArr = new byte[bytes.size()];
                        for (int i = 0; i < bytesArr.length; i++) {
                            bytesArr[i] = bytes.get(i);
                        }
                        //give us our bytes
                        comPort.writeBytes(bytesArr, bytes.size());
                        text = "";
                        count = 0;
                        size = 0;
                        bytes.clear();
                    }
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        launch();
        //close the port to end the program
        comPort.closePort();
    }

    @Override
    
    //gui for the sending and recieivng data
    public void start(Stage primaryStage) {

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);

        area = new TextArea();
        area.setEditable(false);

        vbox.getChildren().addAll(area);
        vbox.setSpacing(5);

        Scene scene = new Scene(vbox);

        primaryStage.setScene(scene);

        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            primaryStage.close();
            open = false;
        });
    }
}
