package com.huawei.java.Bean;

import java.util.HashMap;

public class TimeBean{
    int[][] clientAndFlow;
    HashMap<String, Integer> flow2Id;
    HashMap<Integer, String> id2Flow;

    public TimeBean(int[][] clientAndFlow, HashMap<String, Integer> flow2Id, HashMap<Integer, String> id2Flow) {
        this.clientAndFlow = clientAndFlow;
        this.flow2Id = flow2Id;
        this.id2Flow = id2Flow;
    }

    public int[][] getClientAndFlow() {
        return clientAndFlow;
    }

    public HashMap<String, Integer> getFlow2Id() {
        return flow2Id;
    }

    public HashMap<Integer, String> getId2Flow() {
        return id2Flow;
    }
}
