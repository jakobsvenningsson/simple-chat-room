private void transfer_file(Message message){
    if(!message.content.matches(".+-to [a-zA-Z0-9]+")){
      clients.get(message.from).println("Specify a reciever!");
      return;
    }
    int index = message.content.indexOf("-send-file");
    String filename = message.content.substring(index+11, message.content.indexOf(" ", index + 11)).trim();
    index = message.content.indexOf("-to");
    String reciever = message.content.substring(index + 4, message.content.length()).trim();

    if(!clients.containsKey(reciever)){
      clients.get(message.from).println("User not found!");
      return;
    }
    clients.get(reciever).println("START-TRANSFER-RECIEVER");
    clients.get(reciever).println(message.from + " wants to send you the file: " + filename + " ,accept?(y/n)");
    try{
      String response;
      Boolean valid_response = false;
      while(!valid_response){
        response = in.readLine();
        if(response.equals("n")){
          out.println("CANCEL-TRANSFER");
          return;
        }else if(response.equals("y")){
          valid_response = true;
          out.println("START-TRANSFER");
        }else{
          clients.get(message.from).println("(y/n)?");
        }
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("---------");

    try{
      String port = in.readLine();
      clients.get(reciever).println("START-TRANSFER-RECIEVER:" + port + ";" + filename + "&");
    }catch(Exception e){
      e.printStackTrace();
    }
}
