package Utils;

import com.huawei.java.Bean.TimeBean;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DataWriter {
    FileWriter out;
    HashMap<Integer, String> id2Client;
    HashMap<Integer, String> id2Site;

    public DataWriter(HashMap<Integer, String> ic, HashMap<Integer, String> is, boolean debug) throws IOException {
        id2Client = ic;
        id2Site = is;

        String output = "/output/solution.txt";
        if (debug) {
            output = "solution.txt";
        }
        out = new FileWriter(output);
    }

    public void write2File(int[][][] solutions, int[][] surplus_flow, ArrayList<TimeBean> demand, Set<Integer> nightNode) throws IOException {
        int days = solutions.length;
        String nightString = "";
        for (int node_Id : nightNode){
            if (nightString.equals("")) nightString += id2Site.get(node_Id);
            else nightString = nightString +','+ id2Site.get(node_Id);
        }
        nightString += "\n";
        out.write(nightString);
        for (int i = 0; i < days; i++) {
            HashMap<Integer, String> map = demand.get(i).getId2Flow();
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < id2Client.size(); col++) {
                StringBuilder[] sbs = new StringBuilder[id2Site.size()];
                sb.append(id2Client.get(col)).append(":");
                if (surplus_flow[i][col] != 0) {
                    for (int j = 0 ; j < solutions[0][0].length ; j++){
                        if (solutions[i][col][j] != -1){
                            if(sbs[solutions[i][col][j]] == null) sbs[solutions[i][col][j]] = new StringBuilder().append("<").append(id2Site.get(solutions[i][col][j]));
                            sbs[solutions[i][col][j]].append(",").append(map.get(j));
                        }
                    }
                }
                for (StringBuilder s : sbs){
                    if(s != null)
                        sb.append(s.append('>')).append(",");
                }
                if(sb.charAt(sb.length() - 1) == ',')
                    sb.deleteCharAt(sb.length() - 1);
                sb.append("\n");
            }
            //往文件写入
            if (i == days - 1) sb.delete(sb.length() - 1, sb.length());
            out.write(sb.toString());
        }
        //刷新IO内存流
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
