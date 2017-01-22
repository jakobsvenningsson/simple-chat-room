import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class Client{
  private Socket socket;
  private BufferedReader in;
  private PrintStream out;

  public Client(){
    try{
      this.socket = new Socket("localhost", 5200);
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(System.in));
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
    try{
      (new Thread(new MessageListener(socket))).start();
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  public void run() throws Exception{
    String message;
    try{
      System.out.print("> ");
      while((message = in.readLine()) != null){
        out.println(message);
        System.out.print("> ");
      }
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  public static void main(String[] args) throws Exception{
    Client client = new Client();
    client.run();
  }
}

class MessageListener implements Runnable {
  private Socket socket;
  BufferedReader in;
  public MessageListener(Socket socket) throws Exception{
    this.socket = socket;
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }
  public void run() {
    try{
      String message;
      while((message = in.readLine()) != null){
        System.out.println(message);
      }
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }
}
