package Utils;

import com.huawei.java.Bean.TimeBean;

import java.io.*;
import java.lang.reflect.Array;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataReader {
    // 客户端文件路径
    private String demandPath = "/data/demand.csv";

    // 节点文件路径
    private String bandwidthPath = "/data/site_bandwidth.csv";

    // 节点-客户端时延文件路径
    private String qosPath = "/data/qos.csv";

    // QoS 约束上限值
    private String qos_constraintPath = "/data/config.ini";

    public DataReader(boolean debug) {
        if (debug) {
            demandPath = "data/demand.csv";
            bandwidthPath = "data/site_bandwidth.csv";
            qosPath = "data/qos.csv";
            qos_constraintPath = "data/config.ini";
        }
    }

    public static BufferedReader readUtils(String filePath) throws FileNotFoundException {
        File file = new File(filePath);// Text文件
        return new BufferedReader(new FileReader(file));
    }

    public ArrayList<String> readFlow() throws IOException {
        // 通过读取demand文件，获取时刻流量需求
        ArrayList<String> flow = new ArrayList<>();
        BufferedReader br = readUtils(demandPath);
        // 去除第一行头数据
        String s = br.readLine();
        while ((s = br.readLine()) != null) {// 使用readLine方法，一次读一行
            flow.add(s);
        }
        br.close();
        return flow;
    }

    public ArrayList<TimeBean> readFlowBean() throws IOException {
        BufferedReader br = readUtils(demandPath);
        // 去除第一行头数据
        String s = br.readLine();
        // 通过读取demand文件，获取时刻流量需求
        String pre = br.readLine();
        String time = pre.substring(0, pre.indexOf(","));
        ArrayList<ArrayList<String>> timeFlow = new ArrayList<>();
        while(true){
            ArrayList<String> flow = new ArrayList<>();
            flow.add(pre);
            while((s = br.readLine()) != null){
                String tempTime = s.substring(0, s.indexOf(","));
                if (tempTime.equals(time)) flow.add(s);
                else{
                    pre = s;
                    time = s.substring(0, s.indexOf(","));
                    break;
                }
            }
            timeFlow.add(flow);
            if (s == null) break;
        }
        return transferBean(timeFlow);
    }

    public ArrayList<TimeBean> transferBean(ArrayList<ArrayList<String>> timeFlow){
        int col = timeFlow.get(0).get(0).split(",").length - 2;
        ArrayList<TimeBean> res = new ArrayList<>();
        for (ArrayList<String> flow : timeFlow){
            int row = flow.size();
            int[][] client2Flow = new int[row][col];
            String[] flowName = new String[row];
            int index = 0;
            for (String s : flow){
                String[] tempStr = s.split(",");
                // 放流量名称
                flowName[index] = tempStr[1];
                // 放每个客户端需要的这种流量
                for (int i = 2 ; i < tempStr.length ; i++){
                    client2Flow[index][i - 2] = Integer.valueOf(tempStr[i]);
                }
                index++;
            }
            HashMap<String, Integer> name2id = name2Id(flowName);
            HashMap<Integer, String> id2name = id2Name(flowName);
            TimeBean bean = new TimeBean(client2Flow ,name2id, id2name);
            res.add(bean);
        }
        return res;
    }

    public HashMap<String, Integer> name2Id(String[] names){
        HashMap<String , Integer> map = new HashMap<>();
        for (int i = 0 ; i < names.length ; i++)
            map.put(names[i], i);
        return map;
    }

    public HashMap<Integer, String> id2Name(String[] names){
        HashMap<Integer, String> map = new HashMap<>();
        for (int i = 0 ; i < names.length ; i++)
            map.put(i, names[i]);
        return map;
    }

    public String[] readClientNum() throws IOException {
        // 通过读取demand文件，获取所有客户节点
        int clientNum = 0;
        BufferedReader br = readUtils(demandPath);// 构造一个BufferedReader类来读取文件
        String s = br.readLine();
        br.close();
        String[] strs = s.split(",");
        String[] res = new String[strs.length - 2];
        for (int i = 2; i < strs.length; i++) {
            res[i - 2] = strs[i];
        }
        return res;
    }

    public HashMap<String,Integer> readBandWidth() throws IOException {
        // 通过读取site_bandwidth文件，获取所有节点宽带上限
        BufferedReader br = readUtils(bandwidthPath);// 构造一个BufferedReader类来读取文件
        String s = br.readLine();
        HashMap<String,Integer> map = new HashMap<>();
        while ((s = br.readLine()) != null) {// 使用readLine方法，一次读一行
            String[] strs = s.split(",");
            map.put(strs[0], Integer.valueOf(strs[1]));
        }
        br.close();
        return map;
    }

    public int[] readQosConstraint() throws IOException {
        BufferedReader br = readUtils(qos_constraintPath);// 构造一个BufferedReader类来读取文件
        String s = br.readLine();
        s = br.readLine();
        String s2 = br.readLine();
        br.close();
        return new int[]{Integer.parseInt(s.split("=")[1]) , Integer.parseInt(s2.split("=")[1])};
    }

    public int[][] readQos(HashMap<String,Integer> client,HashMap<String,Integer> node) throws IOException {
        // 通过读取site_bandwidth文件，获取所有节点宽带上限
        int[][] qosMap = new int[node.size()][client.size()];
        BufferedReader br = readUtils(qosPath);     // 构造一个BufferedReader类来读取文件
        String s = br.readLine();
        String[] clientList = s.split(",");
        while ((s = br.readLine()) != null) {       // 使用readLine方法，一次读一行
            String[] strs = s.split(",");
            for (int i = 1 ; i < strs.length ; i++){
                qosMap[node.get(strs[0])][client.get(clientList[i])] = Integer.parseInt(strs[i]);
            }
        }
        br.close();
        return qosMap;
    }
}
