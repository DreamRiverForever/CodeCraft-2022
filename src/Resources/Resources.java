package Resources;

import Utils.DataReader;
import com.huawei.java.Bean.TimeBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Resources {
    ArrayList<TimeBean> DemandList;           // 所有客户端在某一时刻的流量需求
    HashMap<String, Integer> BandWidthMap;  // 节点到最大带宽的映射
    String[] ClientName;                    // 客户端名称
    String[] NodeName;                      // 节点名称
    int qosConstraint;                      // 时延上限值
    HashMap<String,Integer> Client2Id;      // 将客户端名称转为id映射
    HashMap<String,Integer> Site2Id;        // 将节点名称转为id映射
    HashMap<Integer, String> Id2Site;       // 将id转为节点名称映射
    HashMap<Integer, String> Id2Client;     // 将id转为客户端名称映射
    int[][] QosMap;                         // 节点到客户端的时延
    int baseCost;                           // 基础花费
    DataReader reader;

    public Resources(boolean debug) throws IOException {
        reader = new DataReader(debug);
        loadDemand();
        loadBandWidth();
        loadMaxQos();
        type2Id();
        loadQos();
        loadId2Name();
    }

    public int getBaseCost() {
        return baseCost;
    }

    public void loadDemand() throws IOException {
        ClientName = reader.readClientNum();    // 读取客户端名称
        DemandList = reader.readFlowBean();         // 读取每一时刻带宽需求

    }

    public void loadMaxQos() throws IOException {
        int[] ans = reader.readQosConstraint(); // 读取最大时延上限
        qosConstraint = ans[0];
        baseCost = ans[1];
    }

    public void loadBandWidth() throws IOException {
        // 读取没个边缘节点的带宽上限
        BandWidthMap = reader.readBandWidth();
    }

    public void loadId2Name() {
        Id2Site = new HashMap<>();
        for (String key : Site2Id.keySet())
            Id2Site.put(Site2Id.get(key), key);

        Id2Client = new HashMap<>();
        for (String key : Client2Id.keySet())
            Id2Client.put(Client2Id.get(key), key);

    }

    public void type2Id(){
        Client2Id = new HashMap<>();
        Site2Id = new HashMap<>();
        int idx = 0;
        for(String s : ClientName)
            Client2Id.put(s , idx++);
        NodeName = new String[BandWidthMap.size()];
        int i = 0;
        for (String key : BandWidthMap.keySet())
            NodeName[i++] = key;
        idx = 0;
        for(String s : NodeName){
            Site2Id.put(s , idx++);
        }

    }

    public void loadQos() throws IOException {
        // 读取节点到每个客户端的时延
        QosMap = reader.readQos(Client2Id,Site2Id);
    }


    public ArrayList<TimeBean> getDemandList() {
        return DemandList;
    }

    public HashMap<String, Integer> getBandWidthMap() {
        return BandWidthMap;
    }

    public String[] getClientName() {
        return ClientName;
    }

    public int getQosConstraint() {
        return qosConstraint;
    }

    public String[] getNodeName() {
        return NodeName;
    }

    public HashMap<String, Integer> getClient2Id() {
        return Client2Id;
    }

    public HashMap<String, Integer> getSite2Id() {
        return Site2Id;
    }

    public int[][] getQosMap() {
        return QosMap;
    }

    public HashMap<Integer, String> getId2Site() {
        return Id2Site;
    }

    public HashMap<Integer, String> getId2Client() {
        return Id2Client;
    }
}
