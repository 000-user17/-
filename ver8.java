package lightChasing;//通道冲突，一个业务占用了通道后，不能释放
import java.util.*;

public class ver8 {
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
            service.channelIdx  = i%channelNum; //考虑到如果对于相同顶点的业务都从0开始设置通道，则两个不同起点的很容易相撞，所以通过业务顺序设置通道号
            services.add(service);
        }

    }

    static public void showInfo(){
        System.out.println("图信息：");
        System.out.println(graph);
        System.out.println("业务信息：");
        System.out.println(services);
    }

    static public void showPath(){
        for(String pair : pathsMap.keySet()){
            System.out.println("业务的起点和终点: "+pair);
            System.out.println(pathsMap.get(pair));
        }
    }




    public static void dijkstra(int start, int end) {
        // 从顶点vO出发,查找到vi的最短路径

        // listU 记录还未处理的节点
        ArrayList<Integer> listU = new ArrayList<>();
        // dist[] 记录各个节点到起始节点的最短权值
        long[] dist = new long[vertexNum];
        // 记录各个节点的上一级节点(用来联系该节点到起始节点的路径)
        int[] path = new int[vertexNum];

        // 初始化U集合
        for (int i = 0; i < vertexNum; i++) {
            if (i == start) { // S={start}
                continue;
            }
            listU.add(i); // u={vi}/{start}
        }
        // 初始化dist[],path[]
        for (int i = 0; i < vertexNum; i++) {
            // dist[]的当前节点权值就等于start->i节点的权值;初始化所有节点到源点的最短距离
            dist[i] = graph.edgeMatrix[start][i];
            if (graph.edgeMatrix[start][i] == Long.MAX_VALUE) {
                path[i] = -1; // 节点i不可达
            } else {
                path[i] = start; // 若start能直达某点,则表示节点i可以直接访问到start;
            }

        }

        // 记录最小值的节点序号
        int minIndex;
        // int minIndexByI=0;
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
                if (graph.edgeMatrix[minIndex][i] < Long.MAX_VALUE) { //加上了路径长度可以为0的情况
                    // 找到minIndex的所有邻接点
                    if (path[minIndex]!=-1 && graph.edgeMatrix[minIndex][i] + dist[minIndex] < dist[i]) {
                        // 新加入节点更短
                        dist[i] = graph.edgeMatrix[minIndex][i] + dist[minIndex];
                        path[i] = minIndex;
                    }
                }
            }
        } while (minIndex != end && !listU.isEmpty());

        List<Integer> edgePath = new ArrayList<>();//存放dijkstra通过边的顺序
        String key = start+" "+end;
        int cur=end; //path[end]代表最短路径中到end的之前的点
        do{
            edgePath.add(graph.edgeIdxMatrix[path[cur]][cur]);
            cur = path[cur];
        }while(cur!=start);
        Collections.reverse(edgePath);
        pathsMap.put(start+" "+end, edgePath);
        //System.out.println("边的路径为："+edgePath);

        //打印节点顺序
//        System.out.println("节点" + start + "=>" + end + "最短路径是: " + dist[end]);
//        String str = "" + end;
//        int i = end;
//        do {
//            i = path[i];
//            str = i + "=>" + str;
//        } while (i != start);
//        System.out.println(str);
    }



    //寻找各个业务对应的最短路径
    static public void findPath(){
        pathsMap.clear(); //每次对新业务寻找最短路径时，都重置每个业务对应的最短路径map
        for(Service service : services){
            int start = service.start, end = service.end;
            if(pathsMap.containsKey(start+" "+end)){ //如果两个业务的起点和重点一致，则跳过
                continue;
            }
            dijkstra(start, end);
        }
    }


    //单一业务在某一时刻的前进状态，是否需要在本节点进行放大,在这里假设，每条边的最长距离都小于等于衰减距离
    public static void forward(Service service, int outV, int inV, List<Integer> path, List<Integer> amplifiers){
        List<int[]> edges = graph.adjacent.get(outV); //获得outV为出点的所有出边信息
        Collections.sort(edges, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                long dis1 = graph.edges.get(o1[1]).distance;
                long dis2 = graph.edges.get(o2[1]).distance;
                return (int) (dis1-dis2);
            }
        });

        int i=0;
        long min_Dis = graph.edgeMatrix[outV][inV]; //连通的两点间最小的权值, [更改了bug这里continue可能跳过对于最小值的比较]
        for(; i<edges.size() ; ++i){  //遍历所有的出边，寻找可以
            if(edges.get(i)[0] != inV){ //找到入点为inv的边
                continue;
            }
            int edgeIdx = edges.get(i)[1];
            Edge edge = graph.edges.get(edgeIdx);  //代表编号为edges.get(i)[1]的边

            if(edge.channelStatus[service.channelIdx]) { //如果该服务对应的通道被占用
                continue;
            }else{
                long edgeDist = edge.distance;
                edge.channelStatus[service.channelIdx] = true; //当前时刻该边的通道被占用
                if(service.dist+edgeDist >= declineDis){
                    //增加放大器
                    amplifiers.add(outV); //选择的这条边的出点放置放大器
                    service.dist = edgeDist; //到达另一个顶点时的走过的距离
                }else{
                    service.dist = service.dist+edgeDist;
                }
                path.add(edgeIdx); //将该点的经过边的路径加入
                break; //如果找到了可以使用的边，则退出循环

            }
        }
        //新增加边的情况，即没有找到合适的边
        if(i==edges.size()){
            //如果没有找到对应的出边，新增加一条边,并且增加到图和邻接表中
            int edgeNum = graph.edgesNum; //新边的编号
            graph.edges.add(new Edge(outV, inV, edgeNum, min_Dis, channelNum, new boolean[channelNum])); //增加到图中
            graph.adjacent.get(outV).add(new int[]{inV, edgeNum});//增加到邻接表中
            graph.adjacent.get(inV).add(new int[]{outV, edgeNum}); //无向图添加双向边信息
            Edge edge = graph.edges.get(edgeNum);
            long edgeDist = edge.distance;
            if(service.dist+edgeDist >= declineDis){
                //增加放大器
                amplifiers.add(outV);
                service.dist = edgeDist; //放大器在OutV作用后，光业务的距离从零开始，走过该条边变为edgeDist
            }else{
                service.dist = service.dist+edgeDist;
            }
            edge.channelStatus[service.channelIdx] = true; //占用该通道

            path.add(edgeNum); //将该点的经过边的路径加入
            ++graph.edgesNum;//图的边数增加1

        }
        service.curLoc = inV;//当前业务节点变为了inV

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
        while(!isEnd){
            isEnd = true;

            //当前时刻的边的处理
            for(Service service : services){
                if(service.curLoc == service.end){
                    continue;
                }
                //如果有没有到达终点的，则isEnd设置为false
                isEnd = false;

                //根据之前深度遍历得到的各个业务的路径，选择最短的那条作为业务的前进方向

                String serviceSE = service.start+" "+service.end; //根据dfs得到的总距离最短的边来进行业务分配
                int edgeIdx = pathsMap.get(serviceSE).get(time);
                int outV = graph.edges.get(edgeIdx).OutV;
                int inV = graph.edges.get(edgeIdx).Inv;
                //无向边找到当前节点的起点和终点
                int nextV=-1;
                if(service.curLoc == outV){
                    nextV = inV;
                }else if(service.curLoc == inV){
                    nextV = outV;
                }

                forward(service, service.curLoc, nextV, allPaths.get(service.serviceIdx), allAmplifiers.get(service.serviceIdx));

            }
            ++time; //时刻增加，用于选取下一次循环所有到的边
        }
    }


    public static void printOutput(){
        int addEdgesNum = graph.edgesNum - edgeNumOri;
        //cost+=1000000*addEdgesNum;
        System.out.println(addEdgesNum);
        for(int i=edgeNumOri ; i< graph.edgesNum ; ++i){
            System.out.println(graph.edges.get(i).OutV + " " + graph.edges.get(i).Inv);
        }

        for(int i=0 ; i<serviceNum ; ++i){
            int channelIdx=services.get(i).channelIdx;
            int pathNum = allPaths.get(i).size();
            int amplifierNum = allAmplifiers.get(i).size();
            System.out.print(channelIdx + " " + pathNum +" "+ amplifierNum+" ");
            //cost+=100*amplifierNum;
            //cost+=pathNum;
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

    public static void main(String[] args){
        graphConstruct();

        //long timePre = System.currentTimeMillis();

        //showInfo();
        findPath();
        //showPath();//显示dijkstra得到的每个业务的最短路径
        dynamicForward();
        printOutput();
        //printCost();

        //System.out.println(System.currentTimeMillis()-timePre);


    }
}