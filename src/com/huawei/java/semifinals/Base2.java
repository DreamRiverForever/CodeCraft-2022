package com.huawei.java.semifinals;

import Resources.Resources;
import com.huawei.java.Bean.TimeBean;

import java.util.*;

public class Base2 {

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
    ArrayList<TimeBean> tempDemandList;


    public Base2(Resources res) {
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

    // 删除一些无用的边缘节点
    public void deleteNode(int times) {

        ArrayList<Integer> node = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) node.add(i);
        node.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return sites2client.get(o1).size() - sites2client.get(o2).size();
            }
        });

        for (int i = 0; i < qosMap.length && times > 0; i++) {
            if (sites2client.get(i).size() == 0) continue;
            times--;
            int node_Id = node.get(i);
            boolean flag = true;
            for (int client : sites2client.get(node_Id)) {
                if (client2sites.get(client).size() == 1) {
                    flag = false;
                    break;
                }
            }
            if (flag) deleteNodeId(node_Id);
        }
    }

    public void deleteNodeId(int node_Id) {
        for (int client : sites2client.get(node_Id)) {
            ArrayList<Integer> temp = client2sites.get(client);
            // 删除这个id
            int i;
            for (i = 0; i < temp.size(); i++) {
                if (temp.get(i) == node_Id) break;
            }
            temp.remove(i);
        }
        ArrayList<Integer> nodeList = sites2client.get(node_Id);
        nodeList.removeAll(nodeList);
    }

    public int[][][] globalHandle() throws CloneNotSupportedException {
        // 获取所有时刻长度
        int days = demandList.size();
        // 用来存节点宽带分配,第i天第j个客户端将g个流量大小分配给[][][]其中这个客户端
        int[][][] solution = new int[days][clientName.length][100];
        for (int i = 0; i < days; i++) {
            for (int j = 0; j < clientName.length; j++)
                Arrays.fill(solution[i][j], -1);
        }
        Arrays.fill(solution[0][0], -1);
        getDemandList(days);


        // 95计费起始处
        int safeIndex = getSafeIndex(days);
        // 用于记录每一节点每一个时刻还剩多少带宽，i是节点Id，j是第几天
        int[][] D_sumFlow = getDSumFlow(days);
        distributionMaxDays2(days, D_sumFlow, solution, safeIndex);


        distributionAvg2(days, D_sumFlow, solution);


        int[][] sumFlow = new int[qosMap.length][days];


        for (int i = 0; i < qosMap.length; i++) {
            for (int j = 0; j < days; j++) {
                sumFlow[i][j] = bandWidthMap.get(id2Site.get(i)) - D_sumFlow[i][j];
            }
        }

        if (safeIndex > 1) {
            transferFlowType(days, D_sumFlow, solution, safeIndex, sumFlow, 15);
            transferFlowTypeTwo(days, D_sumFlow, solution, safeIndex, sumFlow, 5);
        }

//        for (int i = 0 ; i < days ; i++){
//            int[][] res = tempDemandList.get(i).getClientAndFlow();
//            for (int j = 0 ; j < res.length ; j++){
//                for (int c = 0 ; c < res[0].length ; c++)
//                    System.out.println(res[j][c]);
//            }
//        }


        return solution;

    }

    public void distributionAvg(int days, int[][] D_sumFlow, int[][][] solution) {
        ArrayList<Integer> clients = new ArrayList<>();
        for (int i = 0; i < clientName.length; i++) {
            clients.add(i);
        }
        clients.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return client2sites.get(o1).size() - client2sites.get(o2).size();
            }
        });
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
                while (!queue.isEmpty()) {
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

    public void distributionAvg2(int days, int[][] D_sumFlow, int[][][] solution) {
        ArrayList<Integer> clients = new ArrayList<>();
        for (int i = 0; i < clientName.length; i++) {
            clients.add(i);
        }
        clients.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return client2sites.get(o1).size() - client2sites.get(o2).size();
            }
        });
        List[] lists = new List[clientName.length];
        for (int i = 0 ; i < lists.length ; i++)
            lists[i] = (List) client2sites.get(i).clone();
        // 安排好最大时刻后，开始其他时刻分配，采用随机思想
        for (int idx = 0; idx < days; idx++) {
            // 对每一个客户端进行分配
            int[][] surplus_flow = demandList.get(idx).getClientAndFlow();
            for (Integer col : clients) {
                // 如果当前节点剩余的流量可以吃下这个服务器的流
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
                // 用来存边缘节点
                ArrayList<Integer> nodeList = (ArrayList<Integer>) lists[col];
                int finalIdx = idx;
                while (!queue.isEmpty()) {
                    int[] temp = queue.poll();
                    int suitId = 0;
                    double rate = -1.0;
                    for(int m : nodeList){
                        if (D_sumFlow[m][idx] >= temp[2]){
                            double k = D_sumFlow[m][idx] / bandWidthMap.get(id2Site.get(m));
                            if (k > rate) {
                                rate = k;
                                suitId = m;
                            }
                        }
                    }
                    if (D_sumFlow[suitId][idx] >= temp[2]){
                        // 当前时刻当前节点剩余流量要减少
                        D_sumFlow[suitId][idx] -= temp[2];
                        // 流量分配记录更新
                        solution[idx][col][temp[1]] = suitId;
                        // 客户端流量要减少
                        surplus_flow[temp[1]][temp[0]] = 0;
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
        for (int i = 0; i < qosMap.length; i++) {
            if (sites2client.get(i).size() != 0)
                maxLen.add(i);
        }
        // 开始分配每个节点的安全区到每个时刻
        maxLen.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return sites2client.get(o2).size() - sites2client.get(o1).size();
            }
        });
        for (int i : maxLen) {
            // 这里是拷贝所有时刻下标用于排序
            ArrayList<Integer> flowList = tempList;
            // 遍历这个排序链表取出最大的节点流，这个节点可以有days - safeIndex的安全时刻，流量不要钱，开始分配这些时刻
            for (int s = 0; s < days; s++) {
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
                while (!queue.isEmpty()) {
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
        }


    }

    public void distributionMaxDays2(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex) {
        // 用于存储每个节点对应的所有时刻下标，就是边缘节点带宽序列下标列表
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);

        ArrayList<Integer> maxLen = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) {
            if (sites2client.get(i).size() != 0)
                maxLen.add(i);
        }
        // 开始分配每个节点的安全区到每个时刻
        maxLen.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return bandWidthMap.get(id2Site.get(o2)) - bandWidthMap.get(id2Site.get(o1));
            }
        });
        for (int idx = 0; idx < maxLen.size(); idx++) {
            int i = maxLen.get(idx);
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
                while (!queue.isEmpty()) {
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
        }

        for (int idx = 0; idx < maxLen.size(); idx++) {
            int i = maxLen.get(idx);
            // 这里是拷贝所有时刻下标用于排序
            ArrayList<Integer> flowList = tempList;
            // 遍历这个排序链表取出最大的节点流，这个节点可以有days - safeIndex的安全时刻，流量不要钱，开始分配这些时刻
            for (int s = 0; s < days; s++) {
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
                while (!queue.isEmpty()) {
                    int[] temp = queue.poll();
                    // 如果当前节点剩余的流量可以吃下这个服务器的流
                    if (D_sumFlow[i][time] >= temp[2]) {
                        if (bandWidthMap.get(id2Site.get(i)) - D_sumFlow[i][time] + surplus_flow[temp[1]][temp[0]] > baseCost)
                            continue;
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


    public void transferFlowType(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex, int[][] sumFlow, int c) {
        // 开始流量迁移

        // 对每一个节点开始动态调节
        ArrayList<Integer>[] node_days = new ArrayList[qosMap.length]; // 用于存储每个节点的所有时刻下标
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);
        for (int i = 0; i < node_days.length; i++) {
            node_days[i] = (ArrayList<Integer>) tempList.clone();
        }

        ArrayList<Integer> sitesIndex = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) sitesIndex.add(i);

        Collections.sort(sitesIndex, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return bandWidthMap.get(id2Site.get(o1)) - bandWidthMap.get(id2Site.get(o2));
            }
        });


        for (int g = 0; g < c; g++) {
            // 遍历所有节点Id
            for (int idm = 0; idm < qosMap.length; idm++) {
                int idx = sitesIndex.get(idm);
                // 获取这个节点对应的所有客户端
                for (int i : sites2client.get(idx)) {
                    // 获取这个客户端对应的所有节点
                    for (int raw : client2sites.get(i)) {
                        // 如果这个节点重复了跳过
                        if (idx == raw) continue;
                        int finalI = raw;
                        // 开始按照节点时刻流量上升排列
                        Collections.sort(node_days[raw], new Comparator<Integer>() {
                            @Override
                            public int compare(Integer o1, Integer o2) {
                                return sumFlow[finalI][o1] - sumFlow[finalI][o2];
                            }
                        });
                    }

                }
                for (int k = safeIndex - 1; k >= 0 ; k--) {
                    // 95%处,对应的时刻
                    int safeDaysId = node_days[idx].get(k);
                    // 开始对第safeDaysId天的第idx个节点进行流量调度
                    // 获取95%前一时刻的值
                    // 需要迁移的流量，这个是总流量，所有客户端加起来的
                    if (sumFlow[idx][safeDaysId] <= baseCost) break;
                    int d_flow = sumFlow[idx][safeDaysId] - baseCost;
                    if (d_flow <= 0) continue;
                    // 整理这个节点中放的流种类和来自哪个客户端
                    PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                        @Override
                        public int compare(int[] o1, int[] o2) {
                            return o2[2] - o1[2];
                        }
                    });
                    // 获取这个节点存放的流种类，大小
                    int[][] surFlow = tempDemandList.get(safeDaysId).getClientAndFlow();
                    for (int a : sites2client.get(idx)) {
                        for (int j = 0; j < surFlow.length; j++) {
                            if (solution[safeDaysId][a][j] == idx)
                                queue.add(new int[]{a, j, Integer.valueOf(surFlow[j][a])});
                        }
                    }

                    // 开始迁移
                    while (!queue.isEmpty() && d_flow > 0) {
                        // 获取第一个要迁移的流量
                        int[] arr = queue.poll();
                        // 如果迁移它超过了最大可迁移流量停止
                        if (arr[2] > d_flow) continue;
                        // 遍历这个流量对应的客户端所有的边缘节点
                        for (int node_Id : client2sites.get(arr[0])) {
                            if (idx != node_Id && node_days[node_Id].get(safeIndex - 1) != safeDaysId) {
                                // 计算当前节点可以迁移多少流量，就是95%流量减去当前流量
                                int temp_safeDaysId = node_days[node_Id].get(safeIndex - 1);
                                int can_flow ;
                                if (sumFlow[node_Id][temp_safeDaysId] <= baseCost)
                                    can_flow= baseCost - sumFlow[node_Id][safeDaysId];
                                else can_flow = sumFlow[node_Id][temp_safeDaysId] - sumFlow[node_Id][safeDaysId];

                                if (can_flow < 0) can_flow = Integer.MAX_VALUE;

                                // 迁移节点能够完全吃下
                                if (can_flow >= arr[2] && D_sumFlow[node_Id][safeDaysId] >= arr[2]) {

                                    D_sumFlow[node_Id][safeDaysId] -= arr[2];
                                    sumFlow[node_Id][safeDaysId] += arr[2];

                                    solution[safeDaysId][arr[0]][arr[1]] = node_Id;

                                    D_sumFlow[idx][safeDaysId] += arr[2];
                                    sumFlow[idx][safeDaysId] -= arr[2];
                                    d_flow -= arr[2];
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public void transferFlowTypeTwo(int days, int[][] D_sumFlow, int[][][] solution, int safeIndex, int[][] sumFlow, int c) {
        // 开始流量迁移

        // 对每一个节点开始动态调节
        ArrayList<Integer>[] node_days = new ArrayList[qosMap.length]; // 用于存储每个节点的所有时刻下标
        ArrayList<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < days; i++) tempList.add(i);
        for (int i = 0; i < node_days.length; i++) {
            node_days[i] = (ArrayList<Integer>) tempList.clone();
        }
        int[] totalSumFlow = new int[qosMap.length];
        for (int i = 0 ; i < qosMap.length ; i++){
            int total = 0;
            for (int j = 0 ; j < days ; j++){
                total += sumFlow[i][j];
            }
            totalSumFlow[i] = total;
        }



        ArrayList<Integer> sitesIndex = new ArrayList<>();
        for (int i = 0; i < qosMap.length; i++) sitesIndex.add(i);

        Collections.sort(sitesIndex, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return totalSumFlow[o1] - totalSumFlow[o2];
            }
        });


        for (int g = 0; g < c; g++) {
            // 遍历所有节点Id
            for (int idm = 0; idm < qosMap.length; idm++) {
                int idx = sitesIndex.get(idm);
                // 获取这个节点对应的所有客户端
                for (int i : sites2client.get(idx)) {
                    // 获取这个客户端对应的所有节点
                    for (int raw : client2sites.get(i)) {
                        // 如果这个节点重复了跳过
                        if (idx == raw) continue;
                        int finalI = raw;
                        // 开始按照节点时刻流量上升排列
                        Collections.sort(node_days[raw], new Comparator<Integer>() {
                            @Override
                            public int compare(Integer o1, Integer o2) {
                                return sumFlow[finalI][o1] - sumFlow[finalI][o2];
                            }
                        });
                    }

                }
                for (int k = days - 1 ; k >= 0 ; k--) {
                    // 95%处,对应的时刻
                    int safeDaysId = node_days[idx].get(k);
                    // 开始对第safeDaysId天的第idx个节点进行流量调度
                    // 获取95%前一时刻的值
                    // 需要迁移的流量，这个是总流量，所有客户端加起来的
                    // 整理这个节点中放的流种类和来自哪个客户端
                    PriorityQueue<int[]> queue = new PriorityQueue<>(new Comparator<int[]>() {
                        @Override
                        public int compare(int[] o1, int[] o2) {
                            return o2[2] - o1[2];
                        }
                    });
                    // 获取这个节点存放的流种类，大小
                    int[][] surFlow = tempDemandList.get(safeDaysId).getClientAndFlow();
                    for (int a : sites2client.get(idx)) {
                        for (int j = 0; j < surFlow.length; j++) {
                            if (solution[safeDaysId][a][j] == idx)
                                queue.add(new int[]{a, j, Integer.valueOf(surFlow[j][a])});
                        }
                    }

                    // 开始迁移
                    while (!queue.isEmpty()) {
                        // 获取第一个要迁移的流量
                        int[] arr = queue.poll();
                        // 遍历这个流量对应的客户端所有的边缘节点
                        for (int node_Id : client2sites.get(arr[0])) {
                            if (idx != node_Id && node_days[node_Id].get(safeIndex - 1) != safeDaysId) {
                                // 计算当前节点可以迁移多少流量，就是95%流量减去当前流量
                                int temp_safeDaysId = node_days[node_Id].get(safeIndex - 1);
                                int can_flow = sumFlow[node_Id][temp_safeDaysId] - sumFlow[node_Id][safeDaysId];
                                if (can_flow < 0) can_flow = Integer.MAX_VALUE;
                                // 迁移节点能够完全吃下
                                if (can_flow >= arr[2] && D_sumFlow[node_Id][safeDaysId] >= arr[2]) {
                                    D_sumFlow[node_Id][safeDaysId] -= arr[2];
                                    sumFlow[node_Id][safeDaysId] += arr[2];
                                    solution[safeDaysId][arr[0]][arr[1]] = node_Id;
                                    D_sumFlow[idx][safeDaysId] += arr[2];
                                    sumFlow[idx][safeDaysId] -= arr[2];
                                    break;
                                }
                            }
                        }
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

    public void getDemandList(int days) throws CloneNotSupportedException {
        tempDemandList = new ArrayList<>();
        for (int i = 0 ; i < days ; i++){
            TimeBean bean = demandList.get(i);
            int[][] clientAndFlow = bean.getClientAndFlow();
            int[][] temp = new int[clientAndFlow.length][clientAndFlow[0].length];
            for (int id = 0 ; id < clientAndFlow.length ; id++){
                for (int j = 0 ; j < clientAndFlow[0].length ; j++)
                    temp[id][j] = clientAndFlow[id][j];
            }
            HashMap<String, Integer> flow2Id = (HashMap<String, Integer>) bean.getFlow2Id().clone();
            HashMap<Integer, String> id2Flow = (HashMap<Integer, String>) bean.getId2Flow().clone();
            TimeBean bean1 = new TimeBean(temp, flow2Id, id2Flow);
            tempDemandList.add(bean1);
        }
    }

    public HashMap<Integer, ArrayList<Integer>> getClient2sites() {
        return client2sites;
    }
}
