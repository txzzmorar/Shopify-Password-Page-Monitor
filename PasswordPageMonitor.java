/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package passwordpagemonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

/**
 *
 * @author Tarun
 */
public class PasswordPageMonitor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // TODO code application logic here
        Scanner scan = new Scanner(new File("links.txt"));
        List<String> links = new ArrayList<String>();
        while (scan.hasNextLine()) {
            links.add(scan.nextLine());
        }
        String configObj = FileUtils.readFileToString(new File("config.json"));
        JSONObject confObj = new JSONObject(configObj);
        System.out.println("Config Successfully loaded");
        int delay = confObj.getInt("delay");
        String webhook = confObj.getString("webhook");
        
        ExecutorService executor = Executors.newFixedThreadPool(links.size());
        for(int i = 0; i < links.size(); i++){
            executor.execute(new PasswordMonitor(i,delay,webhook, links.get(i)));
        }
    }
    
}
