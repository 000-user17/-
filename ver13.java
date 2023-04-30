package lightChasing;//通道冲突，一个业务占用了通道后，不能释放

import java.util.*;

public class ver13 {
    //图的类,包含顶点数量以及边的信息
    static class Graph{
        int vertexNum; //顶点数量
        int edgesNum;
        List<Edge> edges; //边的数量，边的加入顺序刚好就是边的序号

        List<List<List<Integer>>> adjacent; //邻接表和对应的下标, adjacent.get(i).get(j)表示i->j的所有边的信息，并且按照距离长短进行排序，adjacent.get(i).get(j)代表边的编号


        Graph(){};

        public Graph(int vertexNum, int edgesNum, List<Edge> edges, List<List<List<Integer>>> adjacent) {
            this.vertexNum = vertexNum;
            this.edgesNum = edgesNum;
            this.edges = edges;
            this.adjacent = adjacent;
        }

        @Override
        public String toString() {
            return "Graph{" +
                    "vertexNum=" + vertexNum +
                    ", edgesNum=" + edgesNum +
                    ", edges=" + edges +
                    ", adjacent=" + adjacent +
                    '}';
        }
    }

    //每一条边的信息
    static class Edge{
        int OutV;//边的出点
        int Inv; //边的入点
        int edgeIdx; //边的编号
        long distance; //边的距离
        int channelNum; //边的通道数量
        boolean[] channelStatus; //每一条通道被占用的情况

        public Edge(int outV, int inv, int edgeIdx, long distance, int channelNum, boolean[] channelStatus) {
            OutV = outV;
            Inv = inv;
            this.edgeIdx = edgeIdx;
            this.distance = distance;
            this.channelNum = channelNum;
            this.channelStatus = channelStatus;
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "OutV=" + OutV +
                    ", Inv=" + Inv +
                    ", edgeIdx=" + edgeIdx +
                    ", distance=" + distance +
                    ", channelNum=" + channelNum +
                    ", channelStatus=" + Arrays.toString(channelStatus) +
                    '}'+"\n";
        }
    }

    //业务对象
    static class Service{
        int start; //服务的起点对应的顶点
        int end; //服务的终点对应的顶点
        int channelIdx; //服务器所在的通道编号
        long dist; //当前业务走过的距离（放大器后重置为0）
        int curLoc; //服务当前所在的顶点
        int serviceIdx;

        Service(){};

        public Service(int start, int end, int serviceIdx) {
            this.dist = 0;//初始化为0，没有走
            this.start = start;
            this.end = end;
            this.serviceIdx = serviceIdx;
            curLoc = start; //初始化业务时，当前位置在start处
        }

        @Override
        public String toString() {
            return "Service{" +
                    "start=" + start +
                    ", end=" + end +
                    ", channelIdx="+channelIdx+
                    '}';
        }
    }

    static Scanner in = new Scanner(System.in); //输入
    static Random r = new Random();

    static List<Service> services;//保存业务的信息，业务按照输入顺序依次加入
    static Graph graph; //网络结构无向图

    static int time; //时刻信息

    static int channelNum; //每一条边的通道数量
    static long declineDis; //最大衰减距离
    static int vertexNum; //顶点数量
    static int edgeNumOri; //最初的边的数量
    static int serviceNum; //一共有几个业务
    static double prob = 0.5; //选择某一条道路的概率


    static Map<String, List<Integer>> pathsMap = new HashMap<>();
    //构造图
    public static void graphConstruct(){
        String[] s1 = in.nextLine().split(" ");

        time=0;
        vertexNum = Integer.parseInt(s1[0]);
        int edgeNum = Integer.parseInt(s1[1]);
        edgeNumOri = edgeNum;//保存最初的边的数量，后续加边，新边编号就从edgeNumOri开始
        serviceNum = Integer.parseInt(s1[2]);
        channelNum = Integer.parseInt(s1[3]);
        declineDis = Long.parseLong(s1[4]);

        //初始化边和距离，以及边的编号
        List<Edge> edges = new ArrayList<>();

        //初始化邻接表和邻接表每一条边的下标
        List<List<List<Integer>>> adjacent = new ArrayList<>();

        for(int i=0 ; i<vertexNum ; ++i){
            adjacent.add(new ArrayList<>());
            for(int j=0 ; j<vertexNum ; ++j){
                adjacent.get(i).add(new ArrayList<>());
            }
        }

        //构造边和距离列表
        for(int i=0 ; i<edgeNum ; ++i){
            String[] edgeInfo = in.nextLine().split(" ");
            int outV = Integer.parseInt(edgeInfo[0]);
            int inV = Integer.parseInt(edgeInfo[1]);
            long dis = Long.parseLong(edgeInfo[2]);
            edges.add(new Edge(outV, inV, i, dis, channelNum, new boolean[channelNum])); //添加边的信息，出点，入点，编号，距离，通道数量，通道信息(是否被占用)

            adjacent.get(outV).get(inV).add(i);
            adjacent.get(inV).get(outV).add(i); //无向图，边是双向的
        }


        graph = new Graph(vertexNum, edgeNum, edges, adjacent); //将节点信息和边信息创建图

        //根据距离对adjacent进行排序,距离越短的排在越前面
        for(int i=0 ; i<vertexNum ; ++i){
            for(int j=0 ; j<vertexNum ; ++j){

                if(adjacent.get(i).get(j).size() > 1){
                    Collections.sort(adjacent.get(i).get(j), new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return (int) (graph.edges.get(o1).distance-graph.edges.get(o2).distance);
                        }
                    });
                }
            }
        }

        //初始化业务的入点和出点
        services = new ArrayList<>(serviceNum);
        //输入每一个业务的出点和入点
        for(int i=0 ; i<serviceNum ; ++i){
            String[] serviceInfo = in.nextLine().split(" ");
            int start = Integer.parseInt(serviceInfo[0]);
            int end = Integer.parseInt(serviceInfo[1]);
            Service service = new Service(start, end, i);
            //service.channelIdx  = i%channelNum; //考虑到如果对于相同顶点的业务都从0开始设置通道，则两个不同起点的很容易相撞，所以通过业务顺序设置通道号
            service.channelIdx = i%channelNum;
            services.add(service);
        }

    }


    static public void showInfo(){
        System.out.println("图信息：");
        System.out.println(graph);
        System.out.println("业务信息：");
        System.out.println(services);
    }

    //改进dijkstra,保证了不会走已经走过的顶点，所以在这里不用对相应的通道设置占用
    //isBusy表示是否考虑通道被占用的情况
    public static List<Integer> dijkstra(int start, int end, Service service, boolean isBusy) {
        // 从顶点vO出发,查找到vi的最短路径

        int channelIdx = service.channelIdx;
        // listU 记录还未处理的节点
        ArrayList<Integer> listU = new ArrayList<>();
        // dist[] 记录各个节点到起始节点的最短权值
        long[] dist = new long[vertexNum];
        // 记录各个节点的上一级节点(用来联系该节点到起始节点的路径)
        int[] path = new int[vertexNum];//存放一个节点到另外一个节点的边的索引

        // 初始化U集合
        for (int i = 0; i < vertexNum; i++) {
            dist[i] = Long.MAX_VALUE;//初始化start都不可到达任何一个节点
            if (i == start) { // S={start}
                continue;
            }
            listU.add(i); // u={vi}/{start}
        }
        // 初始化dist[],path[]
        for (int i = 0; i < vertexNum; i++) {
            // dist[]的当前节点权值就等于start->i节点的权值;初始化所有节点到源点的最短距离
            int edgeChoose=-1;
            double selectProb = 0.2;
            for(Integer edgeIdx : graph.adjacent.get(start).get(i)){
                if(isBusy && graph.edges.get(edgeIdx).channelStatus[channelIdx]){ //考虑通道繁忙的情况，跳过被占用的路线
                    selectProb += 0.05;
                    continue;
                }
                dist[i] = graph.edges.get(edgeIdx).distance;//从start到i的距离最短的这条边没有被占用
                if(((double)r.nextInt(1000))/1000<selectProb){
                    edgeChoose = edgeIdx; //以一定概率选择该点可以到达
                    selectProb += 0.05;
                    break;
                }
            }
            path[i] = edgeChoose; //从start到i的边的选择，如果没有合适的，则为-1
        }

        // 记录最小值的节点序号
        int minIndex;
        // int minIndexByI=0;
        int count=0;
        do {
            //System.out.println("集合U的状态: " + listU);
            // dist数组下标
            minIndex = listU.get(0);
            for (int i = 1; i < listU.size(); i++) {
                if (dist[listU.get(i)] < dist[minIndex]) {
                    minIndex = listU.get(i);
                    // minIndexByI = i;
                }
            }
            listU.remove((Integer) minIndex);
            // listU.remove(minIndexByI);

            // 更新dist和path数组,主要考察minIndex节点纳入S,即新加入节点最短路径变化.
            for (int i = 0; i < vertexNum; i++) {
                int edgeChoose = -1;
                for(Integer edgeIdx : graph.adjacent.get(minIndex).get(i)){

                    if(isBusy && graph.edges.get(edgeIdx).channelStatus[channelIdx]){
                        continue;
                    }
                    edgeChoose = edgeIdx; //找到了合适的边
                    break;
                }
                if(edgeChoose != -1){ //minIndex可以到达i
                    if(path[minIndex] != -1 && dist[minIndex]+graph.edges.get(edgeChoose).distance < dist[i]){//如果start可以到达minIndex并且以minINdex中转到i个距离更短
                        dist[i] = graph.edges.get(edgeChoose).distance + dist[minIndex];
                        path[i] = edgeChoose;
                    }
                }
            }

            if(isBusy){ //考虑冲突情况才增加count
                ++count; //如果不连通，最大寻找次数为100次
            }

        } while (minIndex != end && !listU.isEmpty() && count<100);

        List<Integer> edgePath = new ArrayList<>();//存放dijkstra通过边的顺序
        //System.out.println(Arrays.toString(path));
       if(count<100){ //没有超过限制便找到了start到end的一条可行最短路径
           int cur=end; //path[end]代表最短路径中到end的之前的点
           do{
               if(path[cur] == -1){ //如果路径不通则
                   return new ArrayList<>();
               }
               edgePath.add(path[cur]);
               int inV = graph.edges.get(path[cur]).Inv;
               int outV = graph.edges.get(path[cur]).OutV;
               cur = (cur == inV?outV:inV);
           }while(cur!=start);
       }
        Collections.reverse(edgePath);
       return edgePath;

    }



    public static void addAmplifiersAndPath(List<Integer> p, List<Integer> path, List<Integer> amplifiers, Service service){ //在该路径上增加放大器
        int loc = service.curLoc;
        for(Integer edgeIdx : p){
            graph.edges.get(edgeIdx).channelStatus[service.channelIdx] = true; //设置通道被占用
            long edgeDist = graph.edges.get(edgeIdx).distance;
            if(service.dist + edgeDist >= declineDis){
                service.dist = edgeDist;
                amplifiers.add(loc);
            }else{
                service.dist = service.dist+edgeDist;
            }

            if(graph.edges.get(edgeIdx).OutV == loc){
                loc = graph.edges.get(edgeIdx).Inv;
            }else{
                loc = graph.edges.get(edgeIdx).OutV;
            }
            path.add(edgeIdx);
        }
    }




    //单一业务在某一时刻的前进状态，是否需要在本节点进行放大,在这里假设，每条边的最长距离都小于等于衰减距离
    //每隔一跳就重新规划路径 【可以增加跳数进行优化从而降低时间复杂度】
    public static void forward(Service service, int outV, int inV, List<Integer> path, List<Integer> amplifiers){
        List<Integer> p1 = dijkstra(outV, inV, service, true); //outV到intV的所有路径
        //System.out.println(outV+" "+ inV+" " + paths);

        if(p1.size() > 0){ //说明找到了当前service未被占用的路线
            addAmplifiersAndPath(p1, path, amplifiers, service); //对路径增加放大器，并且将路径让如path中
            allPaths.add(path);
            allAmplifiers.add(amplifiers);
        }
        //新增加边的情况，即没有找到合适的边
        //将最短的路径边加上
        else{
            List<Integer> p2 = dijkstra(outV, inV, service, false);
            List<Integer> newP = new ArrayList<>(); //加边后的新路径
            for(Integer edgeIdx : p2){
                if(graph.edges.get(edgeIdx).channelStatus[service.channelIdx]){
                    int newInv = graph.edges.get(edgeIdx).Inv;
                    int newOutV = graph.edges.get(edgeIdx).OutV;
                    graph.edges.add(new Edge(newOutV, newInv, graph.edgesNum, graph.edges.get(graph.adjacent.get(newOutV).get(newInv).get(0)).distance, channelNum, new boolean[channelNum])); //增加到图中
                    graph.adjacent.get(newOutV).get(newInv).add(0,graph.edgesNum);
                    graph.adjacent.get(newInv).get(newOutV).add(0, graph.edgesNum);
                    newP.add(graph.edgesNum);
                    ++graph.edgesNum;
                }else{
                    newP.add(edgeIdx);
                }
            }
            addAmplifiersAndPath(newP, path, amplifiers, service);
            allAmplifiers.add(amplifiers);
            allPaths.add(path);
        }

    }


    static int cost; //总成本
    static List<List<Integer>> allPaths;//保存所有的业务的通路
    static List<List<Integer>> allAmplifiers;//保存所有业务的放大器

    //所有业务的动态前进状态
    //该算法对各个业务选择路径最短的路径作为其之后要行进的路线
    public static void dynamicForward(){

        allAmplifiers = new ArrayList<>();
        allPaths = new ArrayList<>();
        for(int i=0 ; i<serviceNum ; ++i){ //每一个业务对应的路径和放大器
            allPaths.add(new ArrayList<>());
            allAmplifiers.add(new ArrayList<>());
        }

        //当前时刻的边的处理
        for(Service service : services){
            forward(service, service.start, service.end, allPaths.get(service.serviceIdx), allAmplifiers.get(service.serviceIdx));
        }

    }


    public static void printOutput(){
        int addEdgesNum = graph.edgesNum - edgeNumOri;
        cost+=1000000*addEdgesNum;
        System.out.println(addEdgesNum);
        for(int i=edgeNumOri ; i< graph.edgesNum ; ++i){
            System.out.println(graph.edges.get(i).OutV + " " + graph.edges.get(i).Inv);
        }

        for(int i=0 ; i<serviceNum ; ++i){
            int channelIdx=services.get(i).channelIdx;
            int pathNum = allPaths.get(i).size();
            int amplifierNum = allAmplifiers.get(i).size();
            System.out.print(channelIdx + " " + pathNum +" "+ amplifierNum+" ");
            cost+=100*amplifierNum;
            cost+=pathNum;
            for(int j=0 ; j<pathNum ; ++j){
                System.out.print(allPaths.get(i).get(j)+" ");
            }
            for(int j=0 ; j<amplifierNum ; ++j){
                System.out.print(allAmplifiers.get(i).get(j)+" ");
            }
            System.out.println(); //空格打印下一个业务的信息
        }
    }

    public static void printCost(){
        System.out.println(cost);
    }

    public static void showPath(){

    }

    public static void main(String[] args){
        graphConstruct();


        //long timePre = System.currentTimeMillis();

        //showInfo();
        dynamicForward();
        printOutput();
        //printCost();

        //System.out.println(System.currentTimeMillis()-timePre);


    }
}