package lightChasing;//通道冲突，一个业务占用了通道后，不能释放
import java.util.*;

public class ver11 {
    //图的类,包含顶点数量以及边的信息
    static class Graph{
        int vertexNum; //顶点数量
        int edgesNum;
        List<Edge> edges; //边的数量，边的加入顺序刚好就是边的序号

        List<List<int[]>> adjacent; //邻接表和对应的下标, adjacent.get(i).get(j)[0] 代表i->j的边，[1]代表i->j边的下标

        long[][] edgeMatrix; //存放点与点之间的最短距离，用于dijkstra算法
        int[][] edgeIdxMatrix; //存放点与点之间最短距离边的编号

        Graph(){};

        public Graph(int vertexNum, int edgesNum, List<Edge> edges, List<List<int[]>> adjacent, long[][] edgeMatrix, int[][] edgeIdxMatrix) {
            this.vertexNum = vertexNum;
            this.edgesNum = edgesNum;
            this.edges = edges;
            this.adjacent = adjacent;
            this.edgeMatrix = edgeMatrix;
            this.edgeIdxMatrix = edgeIdxMatrix;
        }

        @Override
        public String toString() {

            for(int i=0 ; i<graph.adjacent.size() ; ++i){//打印邻接表
                System.out.println(i);
                for(int[] info : graph.adjacent.get(i)){
                    System.out.print(Arrays.toString(info));
                }
                System.out.println();
            }

            return "Graph{" +
                    "vertexNum=" + vertexNum + "\n"+
                    ", edges=" + edges +
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
        List<List<int[]>> adjacent = new ArrayList<>();

        long[][] edgeMatrix = new long[vertexNum][];
        int[][] edgeIdxMatrix = new int[vertexNum][]; //保存两点之间距离最短的边的序号，用于后续选择两点间距离最短的那条边
        for(int i=0 ; i<vertexNum ; ++i){
            edgeMatrix[i] = new long[vertexNum];
            edgeIdxMatrix[i] = new int[vertexNum];
            Arrays.fill(edgeMatrix[i], Long.MAX_VALUE); //初始化全部元素为最大值
            Arrays.fill(edgeIdxMatrix[i], -1); //初始化全部为-1，即没有边相连
        }

        for(int i=0 ; i<vertexNum ; ++i){
            adjacent.add(new ArrayList<>());
        }

        //构造边和距离列表
        for(int i=0 ; i<edgeNum ; ++i){
            String[] edgeInfo = in.nextLine().split(" ");
            int outV = Integer.parseInt(edgeInfo[0]);
            int inV = Integer.parseInt(edgeInfo[1]);
            long dis = Long.parseLong(edgeInfo[2]);
            edges.add(new Edge(outV, inV, i, dis, channelNum, new boolean[channelNum])); //添加边的信息，出点，入点，编号，距离，通道数量，通道信息(是否被占用)

            adjacent.get(outV).add(new int[]{inV, i});
            adjacent.get(inV).add(new int[]{outV, i}); //无向图，边是双向的

            //有连接的边进行设置，并且取距离最短的边
            if(edgeMatrix[outV][inV] > dis){
                edgeMatrix[outV][inV] = dis;
                edgeMatrix[inV][outV] = edgeMatrix[outV][inV]; //对称
                edgeIdxMatrix[outV][inV] = i; //将距离更短的那条边的编号放入
                edgeIdxMatrix[inV][outV] = i;
            }
        }

        graph = new Graph(vertexNum, edgeNum, edges, adjacent, edgeMatrix, edgeIdxMatrix); //将节点信息和边信息创建图

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




    //【这里没有将两个节点之间的所有边纳入dfs搜寻】
    //【可以优化把距离之和较短的边放在前面】
    //增加了service，如果service的channelIdx在某一条边上是被占用的，则dfs终止，不加入这条边
    public static List<Map.Entry<List<Integer>, Boolean>> findPath(int start, int end, Service service){ //寻找start到end的所有路径，并且按照距离进行排序
        //保存所有路径（如果有边得通道被占用，则为true，没占用为false， 通过边的条数来排序
        Map<List<Integer>, Boolean> map = new HashMap<>();
        boolean[] visit = new boolean[vertexNum];
        boolean isBusy=false;
        visit[start] = true;
        dfs(start, end, 0, map, new ArrayList<>(), visit, service, isBusy);
        visit[start] = false;

        List<Map.Entry<List<Integer>, Boolean>> paths = new ArrayList<>(map.entrySet());
        Collections.sort(paths, new Comparator<Map.Entry<List<Integer>, Boolean>>() {
            @Override
            public int compare(Map.Entry<List<Integer>, Boolean> o1, Map.Entry<List<Integer>, Boolean> o2) {
                if(o1.getKey().size() == o2.getKey().size()){
                    long sum1 = 0, sum2 = 0;
                    for(Integer edgeIdx : o1.getKey()){
                        sum1+=graph.edges.get(edgeIdx).distance;
                    }
                    for(Integer edgeIdx : o2.getKey()){
                        sum2+=graph.edges.get(edgeIdx).distance;
                    }
                    return (int) (sum1-sum2);
                }
                return o1.getKey().size() - o2.getKey().size();
            }
        });

        return paths;

    }

    public static void dfs(int cur, int end, long dist,  Map<List<Integer>, Boolean> paths, List<Integer> path, boolean[] visit, Service service, boolean flag){

        if(paths.size() == 10){//对于每个业务只寻找8条路线
            return;
        }

        if(cur == end){
            paths.put(new ArrayList<>(path), flag);
            return;
        }
        List<int[]> edgesInfo = graph.adjacent.get(cur);
        for(int[] info : edgesInfo){

            int inV = info[0];
            int edgeIdx = info[1];
            if(visit[inV]){ //不访问访问过的节点
                continue;
            }
            boolean isBusy; //判断当前边的对应业务的通道是否被占用
            if(flag){
                isBusy = true; //如果被占用了，保持被占用的状态
            }else{
                isBusy = graph.edges.get(edgeIdx).channelStatus[service.channelIdx];
            }


            visit[inV] = true;
            path.add(edgeIdx);
            dfs(inV, end, dist+ graph.edges.get(edgeIdx).distance, paths, path, visit, service, isBusy);
            path.remove(path.size()-1);
            visit[inV] = false;
        }
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


    //在所有通路中寻找一条要添加新边最少的路径
    public static List<Integer> findMinAddPath(List<Map.Entry<List<Integer>, Boolean>> paths, Service service){{
        int addMin=graph.edgesNum+1;
        int channelIdx = service.channelIdx;
        List<Integer> res = new ArrayList<>();
        for(Map.Entry<List<Integer>, Boolean> path : paths){
            int addNum=0;
            for(Integer edgeIdx : path.getKey()){
                if(graph.edges.get(edgeIdx).channelStatus[channelIdx]){
                    ++addNum;
                }
            }
            if(addMin > addNum){
                res = path.getKey();
                addMin = addNum;
            }
        }
        return res;
    }

    }

    //单一业务在某一时刻的前进状态，是否需要在本节点进行放大,在这里假设，每条边的最长距离都小于等于衰减距离
    //每隔一跳就重新规划路径 【可以增加跳数进行优化从而降低时间复杂度】
    public static void forward(Service service, int outV, int inV, List<Integer> path, List<Integer> amplifiers){
        List<Map.Entry<List<Integer>, Boolean>> paths = findPath(outV, inV, service); //outV到intV的所有路径
        //System.out.println(outV+" "+ inV+" " + paths);

        for(Map.Entry<List<Integer>, Boolean> entry : paths){  //遍历所有的出边
            if(entry.getValue()){
                continue;  //当前路线通道被占用，换另一条路径查看
            }
            List<Integer> p = entry.getKey();//找到了可用的线路
            addAmplifiersAndPath(p, path, amplifiers, service);//增加放大器和路劲

            service.curLoc = inV;//当前业务节点变为了inV


            //System.out.println(path);

            allPaths.add(path);
            allAmplifiers.add(amplifiers);
            return; //有了路径，退出函数
        }

        //新增加边的情况，即没有找到合适的边
        //将最短的路径边加上
        List<Integer> p = findMinAddPath(paths, service);
        List<Integer> newP = new ArrayList<>(); //加边后的新路径
        for(Integer edgeIdx : p){
            if(graph.edges.get(edgeIdx).channelStatus[service.channelIdx]){ //如果路径上的该边被占用，则增加一条
                int newInv = graph.edges.get(edgeIdx).Inv;
                int newOutV = graph.edges.get(edgeIdx).OutV;
                graph.edges.add(new Edge(newOutV, newInv, graph.edgesNum, graph.edgeMatrix[newInv][newOutV], channelNum, new boolean[channelNum])); //增加到图中
                graph.adjacent.get(newOutV).add(new int[]{newInv, graph.edgesNum});//增加到邻接表中
                graph.adjacent.get(newInv).add(new int[]{newOutV, graph.edgesNum}); //无向图添加双向边信息
                newP.add(graph.edgesNum);
                graph.edgesNum++;
            }else{
                newP.add(edgeIdx);
            }
        }
        //增加完边后，将可行路径放入
        addAmplifiersAndPath(newP, path, amplifiers, service);
        service.curLoc = inV;//当前业务节点变为了inV

        //System.out.println(path);


        allPaths.add(path);
        allAmplifiers.add(amplifiers);
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

        boolean isEnd=false;//判断是否所有的边都到达了终点

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