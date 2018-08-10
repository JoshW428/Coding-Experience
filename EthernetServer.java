import java.io.IOException;
import java.net.*;

public class EthernetServer {

    public EthernetServer() {

    }

    public void sendPacket(Byte[] arr) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName("192.168.123.177");
            System.out.println(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        byte[] bytes = new byte[arr.length];
        for(int i = 0; i < arr.length; i++) {
            bytes[i] = arr[i];
        }
        DatagramPacket packet = new DatagramPacket(bytes, arr.length, address, 24);

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                System.out.println("SENT");
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (socket != null) {
            System.out.println("Closed!");
            socket.close();
        }
    }

    public byte[] recievePacket() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(24);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, 1024);
        try {
            if (socket != null) {
                socket.setSoTimeout(1000);
                socket.receive(packet);
            }
        } catch (IOException e) {
            //.printStackTrace();
        }
        if(socket != null) {
            socket.close();
        }
        return buffer;
    }
}
