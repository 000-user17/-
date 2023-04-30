package lightChasing;
import java.util.*;
import java.util.concurrent.Phaser;

public class ver1 {
    //图的类,包含顶点数量以及边的信息
    static class Graph{
        int vertexNum; //顶点数量
        int edgesNum;
        List<Edge> edges; //边的数量，边的加入顺序刚好就是边的序号

        List<List<int[]>> adjacent; //邻接表和对应的下标, adjacent.get(i).get(j)[0] 代表i->j的边，[1]代表i->j边的下标

        Graph(){};

        public Graph(int vertexNum, int edgesNum, List<Edge> edges, List<List<int[]>> adjacent) {
            this.vertexNum = vertexNum;
            this.edgesNum = edgesNum;
            this.edges = edges;
            this.adjacent = adjacent;
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
        int distance; //边的距离
        int channelNum; //边的通道数量
        boolean[] channelStatus; //每一条通道被占用的情况

        public Edge(int outV, int inv, int edgeIdx, int distance, int channelNum, boolean[] channelStatus) {
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
        int dist; //当前业务走过的距离（放大器后重置为0）
        int curLoc; //服务当前所在的顶点

        Service(){};

        public Service(int start, int end) {
            channelIdx = -1;//初始化为-1，没有分配通道号
            dist = 0;//初始化为0，没有走
            this.start = start;
            this.end = end;
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


    static List<Service> services;//保存业务的信息，业务按照输入顺序依次加入
    static Graph graph; //网络结构无向图

    static int time; //时刻信息
    static List<int[]> edgesPastStatus; //表示上一个时刻相关edge的一些情况, 保存{edge_idx, channel}即边的序号和其对应的channel信息

    static int channelNum; //每一条边的通道数量
    static int declineDis; //最大衰减距离
    static int vertexNum; //顶点数量
    static int edgeNumOri; //最初的边的数量
    static int serviceNum; //一共有几个业务

    static Map<String, List<List<Integer>>> pathsMap = new HashMap<>();
    //构造图
    public static void graphConstruct(){
        Scanner in = new Scanner(System.in);

        time=0;
        vertexNum = in.nextInt();
        int edgeNum = in.nextInt();
        edgeNumOri = edgeNum;//保存最初的边的数量，后续加边，新边编号就从edgeNumOri开始
        serviceNum = in.nextInt();
        channelNum = in.nextInt();
        declineDis = in.nextInt();

        //初始化边和距离，以及边的编号
        List<Edge> edges = new ArrayList<>();

        //初始化邻接表和邻接表每一条边的下标
        List<List<int[]>> adjacent = new ArrayList<>();

        for(int i=0 ; i<edgeNum ; ++i){
            adjacent.add(new ArrayList<>());
        }

        //构造边和距离列表
        for(int i=0 ; i<edgeNum ; ++i){
            int outV = in.nextInt();
            int inV = in.nextInt();
            int dis = in.nextInt();
            edges.add(new Edge(outV, inV, i, dis, channelNum, new boolean[channelNum])); //添加边的信息，出点，入点，编号，距离，通道数量，通道信息(是否被占用)

            adjacent.get(outV).add(new int[]{inV, i});
            adjacent.get(inV).add(new int[]{outV, i}); //无向图，边是双向的
        }

        graph = new Graph(vertexNum, edgeNum, edges, adjacent); //将节点信息和边信息创建图

        //初始化业务的入点和出点
        services = new ArrayList<>(serviceNum);
        //输入每一个业务的出点和入点
        for(int i=0 ; i<serviceNum ; ++i){
            int start = in.nextInt();
            int end = in.nextInt();
            services.add(new Service(start, end));
            services.get(i).channelIdx  = i%channelNum; //考虑到如果对于相同顶点的业务都从0开始设置通道，则两个不同起点的很容易相撞，所以通过业务顺序设置通道号
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


    //寻找各个业务对应的路径有哪些
    static public void findPath(){
        boolean[] visitE = new boolean[graph.edgesNum]; //边是否被遍历过
        boolean[] visitV = new boolean[graph.vertexNum]; //顶点是否被遍历过
        List<Integer> path = new ArrayList<>();
        for(Service service : services){
            int start = service.start, end = service.end;
            if(pathsMap.containsKey(start+" "+end)){ //如果两个业务的起点和重点一致，则跳过
                continue;
            }
            List<List<Integer>> res = new ArrayList<>();
            visitV[start] = true; //当前的起始顶点已经访问
            helpFind(visitV, visitE, start, end, path, res);
            visitV[start] = false; //还原
            Collections.sort(res, new Comparator<List<Integer>>() { //针对路径的总距离进行排序，优先选择总距离短的路劲
                @Override
                public int compare(List<Integer> o1, List<Integer> o2) {
                    int sum1 = 0, sum2 = 0;
                    for(int i=0 ; i<o1.size() ; ++i){
                        sum1 += graph.edges.get(o1.get(i)).distance;
                    }
                    for(int i=0 ; i<o2.size() ; ++i){
                        sum2 += graph.edges.get(o2.get(i)).distance;
                    }
                    return sum1-sum2;
                }
            });
            pathsMap.put(start+" "+end, res); //将[start, end]的所有路径放入到pathsMap中
        }

    }

    static public void helpFind(boolean[] visitV, boolean[] visitE, int cur, int end, List<Integer> path, List<List<Integer>> res){
        if(cur == end){
            res.add(new ArrayList<>(path));
            return;
        }
        for(int[] info : graph.adjacent.get(cur)){
            int v=info[0];//邻接表的出点信息
            int idx=info[1];//邻接表的边的下标
            if(visitE[idx] || visitV[v]){ //如果当前的边或者点被访问过，则进入下一轮循环
                continue;
            }
            //为了保证无向图两个顶点之间有多条边不会反向走，在边不能重复遍历的基础上加上顶点也不能重复遍历，从而只朝着end顶点走，不会多重复
            visitV[v] = true;
            visitE[idx] = true;
            path.add(idx);
            helpFind(visitV, visitE, v, end, path, res);
            path.remove(path.size()-1);
            visitE[idx] = false;
            visitV[v] = false;
        }
    }

    //单一业务在某一时刻的前进状态，是否需要在本节点进行放大,在这里假设，每条边的最长距离都小于等于衰减距离
    public static void forward(Service service, int outV, int inV, List<Integer> path, List<Integer> amplifiers){
        List<int[]> edges = graph.adjacent.get(outV); //获得outV为出点的所有出边信息
        int i=0;
        int min_Dis=Integer.MAX_VALUE;
        for(; i<edges.size() ; ++i){  //遍历所有的出边，寻找可以
            if(edges.get(i)[0] != inV){ //找到入点为inv的边
                continue;
            }
            int edgeIdx = edges.get(i)[1];
            min_Dis = Math.min(graph.edges.get(edgeIdx).distance, min_Dis);//找到所有outV->inV的最短距离, 用于后续的加边
            Edge edge = graph.edges.get(edgeIdx);  //代表编号为edges.get(i)[1]的边

            if(edge.channelStatus[service.channelIdx]) { //如果该服务对应的通道被占用
                continue;
            }else{
                int edgeDist = edge.distance;
                edge.channelStatus[service.channelIdx] = true; //当前时刻该边的通道被占用
                if(service.dist+edgeDist > declineDis){
                    //增加放大器
                    amplifiers.add(outV); //选择的这条边的出点放置放大器
                    service.dist = service.dist+edgeDist-declineDis; //到达另一个顶点时的走过的距离
                }else{
                    service.dist = service.dist+edgeDist;
                }
                path.add(edgeIdx); //将该点的经过边的路径加入

                edgesPastStatus.add(new int[]{edgeIdx, service.channelIdx});//保存该时刻的状态，用于下次将该边对应的channel位置改为false
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
            if(service.dist+edge.distance > declineDis){
                //增加放大器
                amplifiers.add(outV);
            }
            edge.channelStatus[service.channelIdx] = true; //占用该通道
            edgesPastStatus.add(new int[]{edgeNum, service.channelIdx});//保存该时刻的状态，用于下次将该边对应的channel位置改为false
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
        edgesPastStatus = new ArrayList<>(); //初始化操作
        allAmplifiers = new ArrayList<>();
        allPaths = new ArrayList<>();
        for(int i=0 ; i<serviceNum ; ++i){ //每一个业务对应的路径和放大器
            allPaths.add(new ArrayList<>());
            allAmplifiers.add(new ArrayList<>());
        }


        boolean isEnd=false;//判断是否所有的边都到达了终点
        while(!isEnd){
            isEnd = true;
            //先把上一个时刻的节点被占用的通道释放
            for(int[] pastInfo : edgesPastStatus){
                int edgeIdx = pastInfo[0];
                int channelIdx = pastInfo[1];
                graph.edges.get(edgeIdx).channelStatus[channelIdx] = false;
            }
            edgesPastStatus.clear(); //每次处理完之后，都清空过去的状态，下一个forward会增加状态

            //当前时刻的边的处理
            int serviceIdx=0; //对每个顺序的业务的标号
            for(Service service : services){
                if(service.curLoc == service.end){
                    continue;
                }
                //如果有没有到达终点的，则isEnd设置为false
                isEnd = false;

                //根据之前深度遍历得到的各个业务的路径，选择最短的那条作为业务的前进方向
                String serviceSE = service.start+" "+service.end; //根据dfs得到的总距离最短的边来进行业务分配
                int edgeIdx = pathsMap.get(serviceSE).get(0).get(time);
                int outV = graph.edges.get(edgeIdx).OutV;
                int inV = graph.edges.get(edgeIdx).Inv;

                forward(service, outV, inV, allPaths.get(serviceIdx), allAmplifiers.get(serviceIdx));
                ++serviceIdx; //进入到下一个业务选择

            }
            ++time; //时刻增加，用于选取下一次循环所有到的边
        }
    }


    public static void printOutput(){
        int addEdgesNum = graph.edgesNum - edgeNumOri;
        cost+=1000000*addEdgesNum;
        System.out.println(addEdgesNum);
        for(int i=edgeNumOri ; i< graph.edgesNum ; ++i){
            System.out.println(graph.edges.get(i).OutV + " " + graph.edges.get(i).Inv);
        }

        for(int i=0 ; i<services.size() ; ++i){
            int channelIdx=services.get(i).channelIdx;
            int pathNum = allPaths.get(i).size();
            int amplifierNum = allAmplifiers.get(i).size();

            cost+=100*amplifierNum;
            cost+=pathNum;
            System.out.print(channelIdx+" "+pathNum+" "+amplifierNum+" ");
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
        time=0;
        graphConstruct();
        //long time = System.currentTimeMillis();
        //System.out.println(services);
        //showInfo();
        findPath();
        //showPath();
        dynamicForward();
        printOutput();
        //printCost();
        //System.out.println(System.currentTimeMillis() - time);

    }
}