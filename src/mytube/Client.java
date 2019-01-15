package mytube;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("serial")
public class Client implements Serializable{
    private String username;
    private String password;
    
    String dir = "//Users//Nya//Client//";
    Scanner scanIn = new Scanner(System.in);
    String input;

    public Client(){
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void registerClient(MyTubeInterface mt) throws IOException {
            System.out.println("Welcome! Please login");
            System.out.println("Username:");
            this.setUsername(scanIn.nextLine());
            System.out.println("Password:"); 
            this.setPassword(scanIn.nextLine());
            int status=mt.registerClient(this.getUsername(), this.getPassword());
            
            System.out.println("Hello "+ this.getUsername()+" to MyTube, let's start! What do you want to do?");      
    }
    public void uploadContent (MyTubeInterface mt) throws IOException {
                    System.out.println("Introduce a title:");
                    String description = scanIn.nextLine();
                    System.out.println("Choose a topic");
                    String topic = scanIn.nextLine();
                    System.out.println("And now, tell me the complete name of the file as it appears in the directory");
                    String file = scanIn.nextLine();
                    
                    byte[] bytes = Files.readAllBytes(new File(dir+file).toPath());
                    Content content=new Content(description,topic,this.username,file);
                    
                    System.out.println("You are uploading the content "+description+"...");
                    int key=mt.upload(content,bytes);
                    System.out.println("Content uploaded successfully!");
    }
    public void downloadContent (MyTubeInterface mt) throws IOException {
        List<Content> list=mt.getAllContents(); Content content=new Content();
        System.out.println("The contents to download are: "+list+".\nWhat content do you want to download?");
        Integer key=Integer.parseInt(scanIn.nextLine());
        byte[] bytes=mt.download(key, this.username);
        for(Content c : list) {
            if(c.getKey()==key){
                content=c;
                break;
            }
        }
        if(bytes!=null){
            Path directory = Paths.get(dir+content.getDescription()+".mp4");
            Files.write(directory, bytes);
            System.out.println("Content downloaded successfully! ");
        }else{
            System.out.println("The content doesn't exist.");
        }
    }
    public void getContents2 (MyTubeInterface mt) throws IOException {
        System.out.println("Introduce the description of the content");
        String desc = scanIn.nextLine();
        System.out.println(mt.getContents2(desc));
    }
    public void getAllContents (MyTubeInterface mt) throws IOException {
        System.out.println(mt.getAllContents());
    }
    public void getContents (MyTubeInterface mt) throws IOException {
        System.out.println("Introduce the topic of the content");
        String topic = scanIn.nextLine();
        System.out.println(mt.getContents(topic));
    }
     public void registerforCallback (MyTubeInterface mt) throws IOException {
        System.out.println("Introduce the topic to subscribe");
        String topic = scanIn.nextLine();
        CallbackInterface callbackObj=new CallbackImpl();
        mt.registerForCallback(callbackObj,topic);
    }   
     public int modifyTitle (MyTubeInterface mt) throws IOException {
        List<Content> mycontents=mt.getContents3(this.username);
        System.out.println("These are you contents: "+mycontents+"\nWhat content do you want to modify?");
        Integer key=Integer.parseInt(scanIn.nextLine());
        System.out.println("Introduce the new name");
        String title_new = scanIn.nextLine();
        for(Content content : mycontents) {
            if(content.key==key){
                mt.modifyTitle(key, title_new, username);
                System.out.println("Content modified succesfully");
                return 0;
            }
        }
        System.out.println("You can not modify the content!");
        return -1;

    }   
    public int deleteContent (MyTubeInterface mt) throws IOException {
        List<Content> mycontents=mt.getContents3(this.username);
        System.out.println("These are you contents: "+mycontents+"\nWhat content do you want to delete?");
        Integer key=Integer.parseInt(scanIn.nextLine());
        for(Content content : mycontents) {
            if(content.key==key){
                mt.deleteContent(key, username);
                System.out.println("Content deleted succesfully");
                return 0;
            }
        }
        System.out.println("You can not delete the content!");
        return -1;
    } 
}
