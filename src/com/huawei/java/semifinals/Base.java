package com.huawei.java.semifinals;

import Resources.Resources;
import com.huawei.java.Bean.TimeBean;

import java.sql.Array;
import java.util.*;

public class Base {

    String[] clientName;                    // 客户端名称,顺序存取
    HashMap<String, Integer> bandWidthMap;  // 节点到最大带宽的映射
    ArrayList<TimeBean> demandList;           // 所有客户端在某一时刻的流量需求
    int qosConstraint;                      // 最大时延上限qosMax
    int[][] qosMap;                         // 节点到客户端的时延
    HashMap<String, Integer> client2Id;      // 客户端名称转为id映射
    HashMap<String, Integer> site2Id;        // 节点名称转为id映射
    HashMap<Integer, ArrayList<Integer>> client2sites = new HashMap<>(); // 客户端id对应的可使用节点
    HashMap<Integer, String> id2Site;       // 将id转为节点名称映射
    HashMap<Integer, String> id2Client;     // 将id转为客户端名称映射
    HashMap<Integer, ArrayList<Integer>> sites2client = new HashMap<>(); // 节点对应所有可以使用的客户端
    int baseCost;


    public Base(Resources res) {
        clientName = res.getClientName();
        bandWidthMap = res.getBandWidthMap();
        demandList = res.getDemandList();
        qosConstraint = res.getQosConstraint();
        qosMap = res.getQosMap();
        client2Id = res.getClient2Id();
        site2Id = res.getSite2Id();
        id2Client = res.getId2Client();
        id2Site = res.getId2Site();
        baseCost = res.getBaseCost();
        initClient2Sites();
        initSites2Client();
    }

    public void initClient2Sites() {
        for (int i = 0; i < client2Id.size(); i++) {
            ArrayList<Integer> sites = new ArrayList<>();
            for (int j = 0; j < site2Id.size(); j++) {
                if (qosMap[j][i] < qosConstraint) {
                    sites.add(j);
                }
            }
            client2sites.put(i, sites);
        }
    }

    public void initSites2Client() {
        for (int i = 0; i < site2Id.size(); i++) {
            ArrayList<Integer> client = new ArrayList<>();
            for (int j = 0; j < client2Id.size(); j++) {
                if (qosMap[i][j] < qosConstraint) {
                    client.add(j);
                }
            }
            sites2client.put(i, client);
        }

    }

    public int[][][] globalHandle() {
        // 获取所有时刻长度
        int days = demandList.size();
        // 用来存节点宽带分配,第i天第j个客户端将g个流量大小分配给[][][]其中这个节点
        int[][][] solution = new int[days][clientName.length][100];
        for (int i = 0 ; i < days ; i++){
            for (int j = 0 ; j < clientName.length ; j++)
                Arrays.fill(solution[i][j], -1);
        }
        Arrays.fill(solution[0][0], -1);
        // 95计费起始处
        int safeIndex = getSafeIndex(days);
        // 用于记录每一节点每一个时刻还剩多少带宽，i是节点Id，j是第几天
        int[][] D_sumFlow = getDSumFlow(days);
        ArrayList<Integer> list = getMinNodeAggregate();
        // 最大时刻流量分配
        distributionMaxDays3(days, D_sumFlow, solution, safeIndex, list);
        distributionAvg(days, D_sumFlow, solution);
        return solution;

    }

    public ArrayList<Integer> getMinNodeAggregate(){
        Set<Integer> set = new HashSet<>();
        ArrayList<Integer> lists = new ArrayList<>();
        for (int i = 0 ; i < clientName.length ; i++){
            set.add(getMaxNodeId(client2sites.get(i)));
        }
        ArrayList list = new ArrayList<>(set);
        list.sort(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
            }
        });
        lists.addAll(list);
        int node_No_Client = 0;
        for (int i = 0 ; i < qosMap.length ; i++){
            if (sites2client.get(i).size() == 0) node_No_Client++;
        }
        while (lists.size() != qosMap.length - node_No_Client){
            ArrayList<Integer> temp = new ArrayList<>();
            for (int i = 0 ; i < clientName.length ; i++){
                ArrayList<Integer> res = (ArrayList<Integer>) client2sites.get(i).clone();
                for (int id : res) {
                    if (!set.contains(id)) {
                        set.add(id);
                        temp.add(id);
                        break;
                    }
                }
            }
            if (temp.size() != 0){
                temp.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
                    }
                });
                lists.addAll(temp);
            }
        }
        return lists;
    }

    public int getMaxNodeId(ArrayList<Integer> list){
        int max = list.get(0);
        for (int id : list){
            if (bandWidthMap.get(id2Site.get(id)) > bandWidthMap.get(id2Site.get(max))) max = id;
        }
        return max;
    }

    public void distributionAvg(int days, int[][] D_sumFlow, int[][][] solution) {
        ArrayList<Integer> clients = new ArrayList<>();
        for (int i = 0; i < clientName.length; i++) {
            clients.add(i);
        }
        // 安排好最大时刻后，开始其他时刻分配，采用随机思想
        for (int idx = 0; idx < days; idx++) {
            // 对每一个客户端进行分配
            int[][] surplus_flow = demandList.get(idx).getClientAndFlow();
            for (Integer col : clients) {
                // 如果当前节点剩余的流量可以吃下这个服务器的流
                ArrayList<Integer> node_Id = (ArrayList<Integer>) client2sites.get(col).clone();
                node_Id.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
                    }
                });
                PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                    @Override
                    public int compare(int[] o1, int[] o2) {
                        return o2[2] - o1[2];
                    }
                });
                for (int k = 0; k < surplus_flow.length; k++) {
                    if (surplus_flow[k][col] != 0)
                        queue.add(new int[]{col, k, surplus_flow[k][col]});
                }
                while (!queue.isEmpty()){
                    int[] temp = queue.poll();
                    for (Integer node_id : node_Id) {
                        if (temp[2] != 0 && D_sumFlow[node_id][idx] >= temp[2]) {
                            // 当前时刻当前节点剩余流量要减少
                            D_sumFlow[node_id][idx] -= temp[2];
                            // 流量分配记录更新
                            solution[idx][col][temp[1]] = node_id;
                            // 客户端流量要减少
                            surplus_flow[temp[1]][temp[0]] = 0;
                            break;
                        }
                    }
                }
            }
        }
    }


    public int getSafeIndex(int days) {
        // 用于记录安全区下标，就是安全区后面的流量不要钱
        double index_1 = days * 0.95;
        // 向上取整
        int safeIndex = (int) Math.ceil(index_1);
        return safeIndex;
    }

    public void distributionMaxDays(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex) {
        // 用于存储每个节点对应的所有时刻下标，就是边缘节点带宽序列下标列表
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);

        ArrayList<Integer> maxLen = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) maxLen.add(i);
        // 开始分配每个节点的安全区到每个时刻
        maxLen.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
            }
        });
        for (int i : maxLen) {
            // 先统计每个节点在每一个时刻可以分配的最大流量可以是多少
            int[] node_totalFlow = new int[days];
            for (int d = 0; d < days; d++) {
                // 把每个时刻的客户端拥有的流拿出来
                int[][] surplus_flow = demandList.get(d).getClientAndFlow();
                int node_sumFlow = 0;
                // 遍历这个节点对应的所有client，看看能够提供多大的流量
                for (Integer clientId : sites2client.get(i)) {
                    // 遍历这个客户端拥有的流种类
                    for (int raw = 0; raw < surplus_flow.length; raw++)
                        node_sumFlow += surplus_flow[raw][clientId];
                }
                node_totalFlow[d] = node_sumFlow;
            }
            // 开始分配节点i的最大时刻,把每一时刻可以分配的最大流量序列升序排列
            // 这里是拷贝所有时刻下标用于排序
            ArrayList<Integer> flowList = (ArrayList<Integer>) tempList.clone();
            Collections.sort(flowList, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    // 根据每一时刻可以分配的最大流量序列升序排列
                    return node_totalFlow[o1] - node_totalFlow[o2];
                }
            });
            // 遍历这个排序链表取出最大的节点流，这个节点可以有days - safeIndex的安全时刻，流量不要钱，开始分配这些时刻
            for (int s = safeIndex; s < days; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        // 如果当前节点剩余的流量可以吃下这个服务器的流
                        if (surplus_flow[k][c] != 0 && D_sumFlow[i][time] >= surplus_flow[k][c] && surplus_flow[k][c] > baseCost) {
                            // 当前时刻当前节点剩余流量要减少
                            D_sumFlow[i][time] -= surplus_flow[k][c];
                            // 流量分配记录更新
                            solution[time][c][k] = i;
                            // 客户端流量要减少
                            surplus_flow[k][c] = 0;
                        }
                    }
                    if (D_sumFlow[i][time] == 0) break;
                    for (int k = 0; k < surplus_flow.length; k++) {
                        // 如果当前节点剩余的流量可以吃下这个服务器的流
                        if (surplus_flow[k][c] != 0 && D_sumFlow[i][time] >= surplus_flow[k][c]) {
                            // 当前时刻当前节点剩余流量要减少
                            D_sumFlow[i][time] -= surplus_flow[k][c];
                            // 流量分配记录更新
                            solution[time][c][k] = i;
                            // 客户端流量要减少
                            surplus_flow[k][c] = 0;
                        }
                    }
                    if (D_sumFlow[i][time] == 0) break;
                }
            }

            for (int s = 0; s < safeIndex; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        // 如果当前节点剩余的流量可以吃下这个服务器的流
                        if (surplus_flow[k][c] != 0 && D_sumFlow[i][time] >= surplus_flow[k][c]) {
                            if (bandWidthMap.get(id2Site.get(i)) - D_sumFlow[i][time] + surplus_flow[k][c] > baseCost) continue;
                            // 当前时刻当前节点剩余流量要减少
                            D_sumFlow[i][time] -= surplus_flow[k][c];
                            // 流量分配记录更新
                            solution[time][c][k] = i;
                            // 客户端流量要减少
                            surplus_flow[k][c] = 0;
                        }
                    }
                    if (D_sumFlow[i][time] == 0) break;
                }
            }
        }
    }

    public void distributionMaxDays2(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex) {
        // 用于存储每个节点对应的所有时刻下标，就是边缘节点带宽序列下标列表
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);

        ArrayList<Integer> maxLen = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) maxLen.add(i);
        // 开始分配每个节点的安全区到每个时刻
        maxLen.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
            }
        });
        for (int i : maxLen) {
            // 先统计每个节点在每一个时刻可以分配的最大流量可以是多少
            int[] node_totalFlow = new int[days];
            for (int d = 0; d < days; d++) {
                // 把每个时刻的客户端拥有的流拿出来
                int[][] surplus_flow = demandList.get(d).getClientAndFlow();
                int node_sumFlow = 0;
                // 遍历这个节点对应的所有client，看看能够提供多大的流量
                for (Integer clientId : sites2client.get(i)) {
                    // 遍历这个客户端拥有的流种类
                    for (int raw = 0; raw < surplus_flow.length; raw++)
                        node_sumFlow += surplus_flow[raw][clientId];
                }
                node_totalFlow[d] = node_sumFlow;
            }
            // 开始分配节点i的最大时刻,把每一时刻可以分配的最大流量序列升序排列
            // 这里是拷贝所有时刻下标用于排序
            ArrayList<Integer> flowList = (ArrayList<Integer>) tempList.clone();
            Collections.sort(flowList, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    // 根据每一时刻可以分配的最大流量序列升序排列
                    return node_totalFlow[o1] - node_totalFlow[o2];
                }
            });
            // 遍历这个排序链表取出最大的节点流，这个节点可以有days - safeIndex的安全时刻，流量不要钱，开始分配这些时刻
            for (int s = safeIndex; s < days; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                    @Override
                    public int compare(int[] o1, int[] o2) {
                        return o2[2] - o1[2];
                    }
                });

                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        if (surplus_flow[k][c] != 0)
                            queue.add(new int[]{c, k, surplus_flow[k][c]});
                    }
                }
                while(!queue.isEmpty()){
                    int[] temp = queue.poll();
                    // 如果当前节点剩余的流量可以吃下这个服务器的流
                    if (D_sumFlow[i][time] >= temp[2]) {
                        // 当前时刻当前节点剩余流量要减少
                        D_sumFlow[i][time] -= temp[2];
                        // 流量分配记录更新
                        solution[time][temp[0]][temp[1]] = i;
                        // 客户端流量要减少
                        surplus_flow[temp[1]][temp[0]] = 0;
                    }
                }
            }

            for (int s = 0; s < safeIndex; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                    @Override
                    public int compare(int[] o1, int[] o2) {
                        return o2[2] - o1[2];
                    }
                });

                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        if (surplus_flow[k][c] != 0)
                            queue.add(new int[]{c, k, surplus_flow[k][c]});
                    }
                }
                while(!queue.isEmpty()){
                    int[] temp = queue.poll();
                    // 如果当前节点剩余的流量可以吃下这个服务器的流
                    if (D_sumFlow[i][time] >= temp[2]) {
                        if (bandWidthMap.get(id2Site.get(i)) - D_sumFlow[i][time] + temp[2] > baseCost) continue;
                        // 当前时刻当前节点剩余流量要减少
                        D_sumFlow[i][time] -= temp[2];
                        // 流量分配记录更新
                        solution[time][temp[0]][temp[1]] = i;
                        // 客户端流量要减少
                        surplus_flow[temp[1]][temp[0]] = 0;
                    }
                }

            }
        }
    }

    public void distributionMaxDays3(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex, ArrayList<Integer> list) {
        // 用于存储每个节点对应的所有时刻下标，就是边缘节点带宽序列下标列表
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);

        ArrayList<Integer> maxLen = list;
        for (int i : maxLen) {
            // 先统计每个节点在每一个时刻可以分配的最大流量可以是多少
            int[] node_totalFlow = new int[days];
            for (int d = 0; d < days; d++) {
                // 把每个时刻的客户端拥有的流拿出来
                int[][] surplus_flow = demandList.get(d).getClientAndFlow();
                int node_sumFlow = 0;
                // 遍历这个节点对应的所有client，看看能够提供多大的流量
                for (Integer clientId : sites2client.get(i)) {
                    // 遍历这个客户端拥有的流种类
                    for (int raw = 0; raw < surplus_flow.length; raw++)
                        node_sumFlow += surplus_flow[raw][clientId];
                }
                node_totalFlow[d] = node_sumFlow;
            }
            // 开始分配节点i的最大时刻,把每一时刻可以分配的最大流量序列升序排列
            // 这里是拷贝所有时刻下标用于排序
            ArrayList<Integer> flowList = (ArrayList<Integer>) tempList.clone();
            Collections.sort(flowList, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    // 根据每一时刻可以分配的最大流量序列升序排列
                    return node_totalFlow[o1] - node_totalFlow[o2];
                }
            });
            // 遍历这个排序链表取出最大的节点流，这个节点可以有days - safeIndex的安全时刻，流量不要钱，开始分配这些时刻
            for (int s = safeIndex; s < days; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                    @Override
                    public int compare(int[] o1, int[] o2) {
                        return o2[2] - o1[2];
                    }
                });

                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        if (surplus_flow[k][c] != 0)
                            queue.add(new int[]{c, k, surplus_flow[k][c]});
                    }
                }
                while(!queue.isEmpty()){
                    int[] temp = queue.poll();
                    // 如果当前节点剩余的流量可以吃下这个服务器的流
                    if (D_sumFlow[i][time] >= temp[2]) {
                        // 当前时刻当前节点剩余流量要减少
                        D_sumFlow[i][time] -= temp[2];
                        // 流量分配记录更新
                        solution[time][temp[0]][temp[1]] = i;
                        // 客户端流量要减少
                        surplus_flow[temp[1]][temp[0]] = 0;
                    }
                }
            }

            for (int s = 0; s < safeIndex; s++) {
                // 获取可以最大流量分配的时刻
                int time = flowList.get(s);
                // 开始分配这个时刻的节点
                // 首先遍历这个节点拥有的所有client的id
                ArrayList<Integer> temp_clients = sites2client.get(i);
                // 获取这时刻内的数组
                int[][] surplus_flow = demandList.get(time).getClientAndFlow();
                PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                    @Override
                    public int compare(int[] o1, int[] o2) {
                        return o2[2] - o1[2];
                    }
                });

                for (int c : temp_clients) {
                    for (int k = 0; k < surplus_flow.length; k++) {
                        if (surplus_flow[k][c] != 0)
                            queue.add(new int[]{c, k, surplus_flow[k][c]});
                    }
                }
                while(!queue.isEmpty()){
                    int[] temp = queue.poll();
                    // 如果当前节点剩余的流量可以吃下这个服务器的流
                    if (D_sumFlow[i][time] >= temp[2]) {
                        if (bandWidthMap.get(id2Site.get(i)) - D_sumFlow[i][time] + temp[2] > baseCost) continue;
                        // 当前时刻当前节点剩余流量要减少
                        D_sumFlow[i][time] -= temp[2];
                        // 流量分配记录更新
                        solution[time][temp[0]][temp[1]] = i;
                        // 客户端流量要减少
                        surplus_flow[temp[1]][temp[0]] = 0;
                    }
                }

            }
        }
    }


    public int[][] getDSumFlow(int days) {
        // 用于记录每一节点每一个时刻还剩多少带宽，i是节点Id，j是第几天
        int[][] D_sumFlow = new int[qosMap.length][days];

        // 初始化每个节点剩的带宽是满的
        for (int i = 0; i < site2Id.size(); i++) {
            for (int d = 0; d < days; d++) {
                D_sumFlow[i][d] = bandWidthMap.get(id2Site.get(i));
            }
        }
        return D_sumFlow;
    }

    public int[][] getSurFlow() {
        int days = demandList.size();
        // 每一天的流量序列，转整数，i是天数，j是客户端编号为j的带宽流量大小
        int[][] surplus_flow = new int[days][clientName.length];
        for (int i = 0; i < days; i++) {
            int[][] demand = demandList.get(i).getClientAndFlow();
            for (int j = 0; j < demand.length; j++) {
                for (int k = 0; k < demand[0].length; k++) {
                    surplus_flow[i][k] += demand[j][k];
                }
            }
        }
        return surplus_flow;
    }

    public HashMap<Integer, ArrayList<Integer>> getClient2sites() {
        return client2sites;
    }
}
