package main.V2;

import main.V1.Constant;
import main.V1.MapInfo;
import main.V1.Service;
import main.V1.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;

/**
* Created by I002008 on 2014/9/15.
*/
public class PortMapper {

    private static Logger logger = Logger.getLogger(PortMapper.class);
    private Selector selector;

    private static Set<MapInfo> MapInfos = new HashSet<MapInfo>();
    private Set<Service> serviceSet = new HashSet<Service>();

    public Set<Service> getServiceSet() {
        return serviceSet;
    }

    private static void parseCfgFile(String cfgFile){

        try {
            BufferedReader br = new BufferedReader(new FileReader(cfgFile));
            String line;
            while((line = br.readLine())!=null){
                String[] str = line.split(",");
                if(validator(str)) {
                    MapInfos.add(new MapInfo(str[0], Integer.parseInt(str[1]), Integer.parseInt(str[2])));
                }
            }
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }

    }

    public void openServices(String cfgFile){
        parseCfgFile(cfgFile);
        try {
            for (MapInfo mapInfo : MapInfos) {
                new Thread(new LocalService(mapInfo)).start();
                logger.info("New service: " + mapInfo.toString());
            }
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }

    private static boolean validator(String[] str){
        if(str.length < 3){
            return false;
        }else{
            try {
                int dstPort = Integer.parseInt(str[1]);
                int localPort = Integer.parseInt(str[2]);
                if (dstPort < Constant.PORT_MIN || localPort > Constant.PORT_MAX) {
                    return false;
                }
            }catch (NumberFormatException e){
                logger.error("Error in configuration file: Port number format");
                Utils.printErrorLog(e);
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        new PortMapper().openServices(args[0]);
    }

}
