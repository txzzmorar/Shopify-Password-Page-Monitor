/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package passwordpagemonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Tarun
 */
public class PasswordMonitor implements Runnable {
    
    public int taskNum;
    public String site;
    public int delay;
    public String webhook;
    public int currentProxyIdx;
    public Proxy currentProxy;
    public boolean useProxy;
    public ArrayList<String> proxies;
    
    public PasswordMonitor(int i,int del,String web, String get) throws FileNotFoundException {
        site = get;
        taskNum = i;
        delay = del;
        webhook = web;
        
        ThreadLocalAuthenticator.setAsDefault();
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes","");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes","");
        Scanner scanProx = new Scanner(new File("proxies.txt"));
        proxies = new ArrayList<String>();
        while (scanProx.hasNextLine()) {
            proxies.add(scanProx.nextLine());
        }
        currentProxyIdx = 0;
        
    }
    /*
    Check if the password page is up, if up wait for it to go down
    */

    @Override
    public void run() {
        boolean running = true;
        while(running){
            try {
                rotateProxy();
                boolean hasPassword = passwordState();
                while(!hasPassword){
                    System.out.println(getTime() +"["+site+"]"+ "Password page not found");
                    hasPassword = passwordState();
                    Thread.sleep(delay);
                    rotateProxy();
                }
                System.out.println(getTime() +"["+site+"]"+ "Password page live!");
                sendWebhook(true);
                boolean didPassGoDown = false;
                while(!didPassGoDown){
                    boolean StillLive = passwordState();
                    if (StillLive){
                        System.out.println(getTime() +"["+site+"]"+ "Password Still Up");
                        Thread.sleep(delay);
                        rotateProxy();
                    }
                    else if (!StillLive){
                        System.out.println(getTime() +"["+site+"]"+ "Password Down! - Live!");
                        sendWebhook(false);
                        didPassGoDown = true;
                        rotateProxy();
                    }
                }
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(PasswordMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
    public boolean passwordState() throws IOException{
             boolean passwordState;
            //System.out.println("["+taskNum+"]Checking for Password on: "+site);
            Connection.Response init = Jsoup.connect(site)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute();
            if(init.url().toString().contains("password")){
                //System.out.println("["+taskNum+"]Password Page is up for: "+site);
                passwordState = true;
            }
            else{
               passwordState = false;
            }
            return passwordState;
    }
    
    public String getTime(){
        String timeStamp = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
        String newTime = "["+timeStamp+"]";
        return newTime;
    }
    
    public void sendWebhook(boolean passwordState) throws IOException{
        JSONObject iArr = new JSONObject();
        String info = "";
        String color = "";
        if(passwordState){
            info = "Password Page is Up!";
            color = "ff0000"; 
        }
        else if(!passwordState){
            info = "Password Page is Down!";
            color = "00FF00"; 
        }
        iArr.put("title", site);
        iArr.put("value", info);
        iArr.put("short",true);
        
        
        JSONArray arr2 = new JSONArray();
        arr2.put(iArr);

        
        JSONObject emb = new JSONObject();
            emb.put("color","#"+color);
            emb.put("title", "Password Page Monitor");
            emb.put("title_link",site);
            emb.put("fields", arr2);
            emb.put("footer",getTime() + " | @Tazzmorar ");
            
            JSONArray arr = new JSONArray();
            arr.put(emb);

            JSONObject Final = new JSONObject();
            Final.put("attachments",arr);
            
            Document doc = Jsoup.connect(webhook)
                    .requestBody(Final.toString())
                    .header("Content-type", "application/json")
                    .post();
    }
    
    public void rotateProxy() throws FileNotFoundException{
        int lenOfProxies = proxies.size();
        if (lenOfProxies == 0){
            useProxy = false;
        }
        else{
            useProxy = true;
            String selectedProxy = proxies.get((currentProxyIdx++%proxies.size()));
            String[] proxyArr = selectedProxy.split(":");
            if(proxyArr.length ==2){
                String host = proxyArr[0];
                int port = Integer.parseInt(proxyArr[1]);
                currentProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host,port));
            }
            else{
                String host = proxyArr[0];
                int port = Integer.parseInt(proxyArr[1]);
                String username = proxyArr[2];
                String password = proxyArr[3];
                ThreadLocalAuthenticator.setProxyAuth(username, password);
                currentProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host,port));
            }
        }
    }
    
}
