package lightChasing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class testEx {
    public static void main(String[] args){
        examples(5,5,5,5,5);
    }
    public static List<String> examples(int vertexNum, int edgeNum, int serviceNum, int channelsNum, int declineDis){
        List<String> res = new ArrayList<>();
        String info = vertexNum+" "+edgeNum+" "+serviceNum+" "+channelsNum+" "+declineDis;
        res.add(info);
        Random r = new Random();
        for(int i=0 ; i<edgeNum ; ++i){
            int outV = r.nextInt(vertexNum);
            int inV = r.nextInt(vertexNum);
            while(inV == outV){
                inV = r.nextInt(vertexNum);
            }
            int dist = r.nextInt(declineDis)+1;
            String edge = outV+" "+inV+" "+dist;
            res.add(edge);
        }
        for(int i=0 ; i<serviceNum ; ++i){
            int start = r.nextInt(vertexNum);
            int end = r.nextInt(vertexNum);
            while(end == start){
                end = r.nextInt(vertexNum);
            }
            res.add(start+" "+end);
        }
        return res;

    }

    public static List<String> examGen(){
        Random r = new Random();

        int vertexNum = r.nextInt(10000)+1;
        int edgeNum = r.nextInt(30000)+1;
        int serviceNum = r.nextInt(5000)+1;
        int channelNum = r.nextInt(20)+1;
        int declineDis = r.nextInt(500)+1;
        List<String> examples = testEx.examples(vertexNum, edgeNum, serviceNum, channelNum, declineDis);
        return examples;
    }

}
