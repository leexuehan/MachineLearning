import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.*;

/**
 * SGA 简单遗传算法
 * <p>
 * 求下面表达式在x的指定区间上的最大值
 * f(x)=x*x
 * x∈[−1,2]
 * 求解精度为 e=0.01
 */
public class SGAAlgorithm {
    private static final int BIT_NUM = 9;
    private static final int POPULATION_SCALE = 40;
    private static final int GENERATION_NUM = 100;


    public static void main(String[] args) throws Exception {
        List<String> populations = initPopulations(POPULATION_SCALE);
        System.out.println("最大值:" + getMaximumValue(populations, GENERATION_NUM));
    }

    private static List<String> initPopulations(int num) {
        Random random = new Random();
        List<String> populations = new ArrayList<String>();
        for (int i = 0; i < num; i++) {
            String binaryStr = generateBinaryStr(random);
            while (populations.contains(binaryStr)) {
                binaryStr = generateBinaryStr(random);
            }
            populations.add(binaryStr);
        }
        return populations;
    }

    private static String generateBinaryStr(Random random) {
        StringBuilder binaryStr = new StringBuilder();
        for (int bit = 0; bit < BIT_NUM; bit++) {
            binaryStr.append(random.nextInt(2));
        }
        return binaryStr.toString();
    }

    private static double getMaximumValue(List<String> populations, int generationNums) throws Exception {
        int generation = 0;
        List<String> tmpPops = populations;
        double finalValue = 0;
        while (generation < generationNums) {
            System.out.println("迭代次数 :" + generation);
            List<String> nextGeneration = selectNextGeneration(tmpPops);
            System.out.println("next generation selected is:" + nextGeneration);
            System.out.println("next generation size is:" + nextGeneration.size());
            Set<String> forOutput = new HashSet<String>(nextGeneration);
//            for (String ng : forOutput) {
//                System.out.println(ng);
//            }
            System.out.println("种群个体数:" + forOutput.size());
            tmpPops = variation(crossGeneration(nextGeneration, populations.size() / 4));
            double max = getMax(tmpPops);
            System.out.println("max value in generation is :" + max);
            if (max > finalValue) {
                finalValue = max;
            }
            generation++;
        }

        return finalValue;
    }

    private static double getMax(List<String> tmpPops) throws Exception {
        double max = 0.0;
        String binaryStr = "";
        for (String pops : tmpPops) {
            double value = fitnessValue(decode(pops));
            if (value >= max) {
                max = value;
                binaryStr = pops;
            }
        }
        System.out.println("最大值为:" + max);
        System.out.println("二进制串:" + binaryStr);
        return max;
    }


    //将9位二进制串转化为区间[-1,2]内的十进制数
    private static double decode(String binaryStr) throws Exception {
        if (binaryStr == null || binaryStr.length() != 9) {
            throw new Exception("input binaryStr is illegal :" + binaryStr);
        }
        int value = Integer.parseInt(binaryStr, 2);
        return value * 3.0 / (Math.pow(2, BIT_NUM) - 1) * 1.0 - 1;
    }

    //求解适应度值f(x)=x∗sin(10∗π∗x)+2
    private static double fitnessValue(double value) {
        return value * Math.sin(10 * Math.PI * value) + 2;
    }

    //计算生存概率
    private static Map<String, Double> surviveProbability(List<String> population) throws Exception {
        Map<String, Double> probabilities = new HashMap<String, Double>();
        double sum = 0;
        for (String item : population) {
            double value = decode(item);
            sum += fitnessValue(value);
        }

        for (String pop : population) {
            double value = decode(pop);
            //重复个体
            if (probabilities.containsKey(pop)) {
                probabilities.put(pop, probabilities.get(pop) + fitnessValue(value) / sum);
            } else {
                probabilities.put(pop, fitnessValue(value) / sum);
            }
        }

        return probabilities;
    }


    //产生下一代群体
    private static List<String> selectNextGeneration(List<String> population) throws Exception {
        Map<String, Double> surviveProbability = surviveProbability(population);
        System.out.println("生存概率:");
        System.out.println(surviveProbability);
        Map<String, double[]> boundsOfPops = new HashMap<String, double[]>();
        //根据概率打造轮盘
        System.out.println("轮盘开始打造");
        double leftBoarder = -1;
        for (String aPopulation : surviveProbability.keySet()) {
            double[] bounds = new double[2];//存放下界和上界
            if (leftBoarder == -1) {
                bounds[0] = -1;
                bounds[1] = -1 + surviveProbability.get(aPopulation) * 3;
            } else {
                bounds[0] = leftBoarder;
                bounds[1] = leftBoarder + surviveProbability.get(aPopulation) * 3;
            }
//            System.out.println(bounds[0] + "," + bounds[1]);
            boundsOfPops.put(aPopulation, bounds);
            leftBoarder = bounds[1];
        }
        System.out.println("轮盘打造完毕");
        System.out.println("轮盘区间数:" + boundsOfPops.size());
//        for (Map.Entry<String, double[]> entry : boundsOfPops.entrySet()) {
//            System.out.println(Arrays.toString(entry.getValue()));
//        }
        //每次选择1个
        int pickTimes = POPULATION_SCALE;
        System.out.println("pick " + pickTimes + " times");
        List<String> nextGeneration = new ArrayList<String>();
        for (int time = 0; time < pickTimes; time++) {
            double cursor = generateBoundedRandomDouble(-1, 2);
            boolean foundDistrict = false;
            for (String pop : boundsOfPops.keySet()) {
                double[] limits = boundsOfPops.get(pop);
                if (limits[0] <= cursor && cursor <= limits[1]) {
//                    System.out.println("落在区间:[" + limits[0] + "," + limits[1] + "]游标为:" + cursor);
//                    System.out.println("select:" + pop);
                    foundDistrict = true;
                    nextGeneration.add(pop);
                }
            }
            if (!foundDistrict) {
                System.out.println("not find district,cursor is:" + cursor);
            }
        }
        return nextGeneration;

    }

    private static double generateBoundedRandomDouble(double min, double max) {
        double boundedDouble = new RandomDataGenerator().getRandomGenerator().nextDouble();
        return min + boundedDouble * (max - min);
    }

    //群体间交叉
    private static List<String> crossGeneration(List<String> populations, int pairNum) {
        List<String> newPopulations = new ArrayList<String>();
        //随机配对
        int size = populations.size();
        Random random = new Random();
        List<int[]> selectedPairs = generatePairs(pairNum, size);
        for (int[] pair : selectedPairs) {
            int index1 = pair[0];
            int index2 = pair[1];
            String one = populations.get(index1);
            String another = populations.get(index2);
            //随机产生交叉点
            int crossIndex = random.nextInt(BIT_NUM);
            //交叉
            String newOne = one.substring(0, crossIndex) + another.substring(crossIndex);
            String newAnother = another.substring(0, crossIndex) + one.substring(crossIndex);
            newPopulations.add(newOne);
            newPopulations.add(newAnother);

        }

        return newPopulations;
    }

    private static List<int[]> generatePairs(int pairNum, int size) {
        List<int[]> selectedPairs = new ArrayList<int[]>();
        Set<Integer> selectedIndex = new HashSet<Integer>();
        Random random = new Random();
        for (int times = 0; times < pairNum; times++) {
            //随机产生两个配对的下标
            int i = random.nextInt(size);
            int j = random.nextInt(size);
            while (i == j || selectedIndex.contains(i) || selectedIndex.contains(j)) {
//                System.out.println("pair index not suit , regenerate them");
                i = random.nextInt(size);
                if (i == 0) {
                    j = random.nextInt(i + 1);
                } else {
                    j = random.nextInt(i);
                }
//                System.out.println("generate pair:" + i + "," + j);
            }
//            System.out.println("suit pair:" + i + "," + j);
            selectedIndex.add(i);
            selectedIndex.add(j);
            int[] pair = new int[]{i, j};
            selectedPairs.add(pair);
        }
        return selectedPairs;

    }

    //变异
    private static List<String> variation(List<String> populations) {
        double variationPro = 0.01;
        double varNum = populations.size() * variationPro;
        if (varNum >= 1) {
            //如果有大于一个种群个体可以变异
            Random random = new Random();
            int selected = random.nextInt(populations.size());
            //变异方式为随机改变其中一位
            int index = random.nextInt(BIT_NUM);
            String pop = populations.get(selected);
            char var = pop.charAt(index) == '0' ? '1' : '0';
            populations.set(selected, pop.replace(pop.charAt(index), var));
        }
        return populations;

    }


}
