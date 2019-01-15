package mytube;

import com.google.gson.Gson;
import java.awt.PageAttributes.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;
import java.util.AbstractCollection;
import java.util.Collection;
import mytube.Content;

public class MyTubeImpl extends UnicastRemoteObject implements MyTubeInterface{

    List<Content> comodin = new ArrayList<>();
    Map<Integer, Content> contents = new HashMap();
    Map<String, Vector> topics=new HashMap();
    private String dir = "//Users//Nya//Servidor//"; //Mirar com s'agafa el document
    private static Vector clientList;
    String rmi;
    
    public MyTubeImpl(String reg) throws RemoteException{super();clientList=new Vector();this.rmi=reg;}

    public void recoverServer(String name) {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/server/"+name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            conn.disconnect();
            
            Gson g = new Gson();
            Content[] contentsArray = g.fromJson(output, Content[].class);
            comodin = new ArrayList<>(Arrays.asList(contentsArray));
            for(Content c : comodin) {
                contents.put(c.getKey(), c);
            }
            System.out.println(contents+"\n");
			
            } catch (Exception e) { 
            }
    }
    @Override
    public void registerForCallback(CallbackInterface callbackclientObject,String topic) throws RemoteException {
        clientList=new Vector();
        if(topics.containsKey(topic)){
            clientList= topics.get(topic);
        }
        if(!(clientList.contains(callbackclientObject))) {
            clientList.addElement(callbackclientObject);
        }
        topics.put(topic, clientList);
        System.out.println(topics);
    } 
    private synchronized void doCallbacks(String title, String topic) throws RemoteException {
        clientList= topics.get(topic);
        if(clientList!=null){
            for(int i=0; i<clientList.size();i++) {
                CallbackInterface nextClient= (CallbackInterface) clientList.elementAt(i);
                nextClient.notifyMe("**A Client creates content "+ title+" at topic " +topic+".**");
            }
        }
    }
    @Override
    public int getContentKey (Content content) throws RemoteException {  
        URL url;
        System.out.print(content);
        try {
            content.setServer(this.rmi);
                        
            url = new URL ("http://localhost:8080/myRESTweb/rest/content");
            System.out.println("Open connection");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            Gson ret = new Gson();
            String input=ret.toJson(content);
            OutputStream os= conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            
            int status = conn.getResponseCode();
            

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            int key = Integer.parseInt(br.readLine());
            conn.disconnect();
            
            content.setKey(key);
            doCallbacks(content.getDescription(),content.getTopic());

            return key;
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(MyTube.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        } catch (IOException ex) {
            Logger.getLogger(MyTube.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }   
    @Override
    public int registerClient(String username, String password) throws RemoteException {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/client/"+username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            
            Gson ret = new Gson();
            String input=ret.toJson(password);
            OutputStream os= conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            
            int status = conn.getResponseCode();
            System.out.println("status register client: "+status);
            conn.disconnect();
            if(status==201) {
                return 201;
            }
            return 200;
            
            } catch (Exception e) {
                return -1;
            }
        
    }
    @Override
    public int upload(Content content, byte[] bytes) throws RemoteException {  
      
        try {
            content.setKey(getContentKey(content));
            content.setServer(this.toString());
      
            File file=new File(dir+content.getKey()); //Generem el file amb el key
            file.mkdir();
            Path directory = Paths.get(dir+content.getKey()+"//"+content.getFile());
            Files.write(directory, bytes);
            doCallbacks(content.getDescription(),content.getTopic());
            
            content.setServer(this.rmi);
            contents.put(content.getKey(), content);
            
            return content.getKey();
        } catch (IOException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return content.getKey(); 

    }
    @Override
    public byte[] download(Integer key, String username) throws RemoteException {  
        try {
            if(this.rmi.equals(contents.get(key).getServer())) {
                byte[] bytes = Files.readAllBytes(new File(dir+key.toString()+"//"+contents.get(key).getDescription()+".mp4").toPath());
                return bytes;
            }else {
                MyTubeInterface mt2 = (MyTubeInterface)Naming.lookup(contents.get(key).getServer());
                byte[] bytes=mt2.download(key, username);
                return bytes;
            }      
            
        } catch (IOException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (NotBoundException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    @Override
    public List<Content> getAllContents() throws RemoteException {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/contents");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if(conn.getResponseCode() != 200) {
		return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            conn.disconnect();
            
            Gson g = new Gson();
            Content[] contentsArray = g.fromJson(output, Content[].class);
            comodin = new ArrayList<>(Arrays.asList(contentsArray));
            return comodin;
			
            } catch (Exception e) { 
                return new ArrayList<>(); 
            }
    }
    @Override
    public List<Content> getContents(String topic) throws RemoteException {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/contents/topic/"+topic);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if(conn.getResponseCode() != 200) {
		return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            conn.disconnect();
            
            Gson g = new Gson();
            Content[] contentsArray = g.fromJson(output, Content[].class);
            comodin = new ArrayList<>(Arrays.asList(contentsArray));
            return comodin;
			
            } catch (Exception e) { 
                return new ArrayList<>(); 
            }
    }
    @Override
    public List<Content> getContents2(String description) throws RemoteException {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/contents/desc/"+description);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if(conn.getResponseCode() != 200) {
		return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            conn.disconnect();
            
            Gson g = new Gson();
            Content[] contentsArray = g.fromJson(output, Content[].class);
            comodin = new ArrayList<>(Arrays.asList(contentsArray));
            return comodin;
			
            } catch (Exception e) { 
                return new ArrayList<>(); 
            }
    }
    @Override
    public List<Content> getContents3(String username) throws RemoteException {
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/contents/client/"+username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if(conn.getResponseCode() != 201) {
		return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            System.out.println("Output: "+output);
            conn.disconnect();
            
            Gson g = new Gson();
            Content[] contentsArray = g.fromJson(output, Content[].class);
            comodin = new ArrayList<>(Arrays.asList(contentsArray));
            return comodin;
			
            } catch (Exception e) { 
                return new ArrayList<>(); 
            }  
    }
    @Override
    public Content getContent(String title) throws RemoteException {  
        for(Content content : contents.values()) {
            if(content.getDescription().equals(title)){
                return content;
            }  
        }
        return null;
    }
    @Override
    public void modifyTitle (Integer key, String new_title, String username) throws RemoteException{
    URL url;
        try {
            url = new URL("http://localhost:8080/myRESTweb/rest/content/"+username+"/"+key+"/edit");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            
            OutputStream os= conn.getOutputStream();
            os.write(new_title.getBytes());
            os.flush();
            conn.getResponseCode();
            conn.disconnect(); 
            
        } catch (MalformedURLException ex ) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void deleteContent (Integer key, String username) throws RemoteException{
        URL url;
        try {
            url = new URL ("http://localhost:8080/myRESTweb/rest/content/"+key+"/delete");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            
            int status = conn.getResponseCode();
            conn.disconnect();
        
        } catch (MalformedURLException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MyTubeImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

