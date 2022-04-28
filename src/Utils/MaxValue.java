package Utils;


import java.util.ArrayList;
import java.util.Scanner;

/**
 * 使用二维数组非递归的方法求解0/1背包问题
 */
public class MaxValue {
    // N表示物体的个数，V表示背包的载重
    int N,V;
    //用于存储每个物体的重量，下标从1开始
    private int[] weight;
    //存储每个物体的收益，下标从1开始
    private int[] value;
    //二维数组，用来保存每种状态下的最大收益
    private int[][] F;

    /**
     * 使用非递归方式，求解F[0 .. N][0 .. V]，即for循环从下至上求解
     */
    public MaxValue(int N, int V, ArrayList<Integer> surFlow){
        //下标从1开始，表示第1个物品
        weight = new int[N + 1];
        value = new int[N + 1];
        F= new int[N + 1][V + 1];//注意是 N + 1，因为需要一个初始状态F[0][0]，表示前0个物品放进空间为0的背包的最大收益
        this.N = N;
        this.V = V;
        for(int i = 1; i <= N; i++) {
            weight[i] = surFlow.get(i - 1);
            value[i] = surFlow.get(i - 1);
        }
    }
    public void ZeroOnePackNonRecursive() {
        //注意边界问题，i是从1开始的，j是从0开始的
        //因为F[i - 1][j]中i要减1
        for(int i = 1; i <= N; i++) {
            for(int j = 0; j <= V; j++) {
                //如果容量为j的背包放得下第i个物体
                if(j >= weight[i]) {
                    F[i][j] = Math.max(F[i - 1][j - weight[i]] + value[i], F[i - 1][j]);
                }else {
                    //放不下，只能选择不放第i个物体
                    F[i][j] = F[i - 1][j];
                }
            }
        }
    }
    public boolean[] printResult() {
        ZeroOnePackNonRecursive();
        boolean[] isAdd = new boolean[N + 1];
        for(int i = N; i >= 1; i--) {
            if(F[i][V] == F[i-1][V])
                isAdd[i] = false;
            else {
                isAdd[i] = true;
                V -= weight[i];
            }
        }
        return isAdd;
    }
}