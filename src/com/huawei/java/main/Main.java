package com.huawei.java.main;

import Resources.Resources;
import Utils.DataWriter;
import com.huawei.java.semifinals.Base;
import com.huawei.java.semifinals.Base2;
import com.huawei.java.semifinals.Base3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


public class Main {

    public static void main(String[] args) throws IOException, CloneNotSupportedException {
        boolean debug = args.length == 0;

        Resources res = new Resources(debug);
        DataWriter writer = new DataWriter(res.getId2Client(), res.getId2Site(), debug);
        Base3 base = new Base3(res);
        int[][] surplus_flow = base.getSurFlow();
        int[][][] solutions = base.globalHandle();
        Set<Integer> nightNode = base.getNightNode();
        writer.write2File(solutions, surplus_flow, res.getDemandList(), nightNode);

    }
}
