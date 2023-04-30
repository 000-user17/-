package lightChasing;

import java.util.ArrayList;
import java.util.List;

public class test {
    public static void main(String[] args){
        List<List<int[]>> list=  new ArrayList<>();
        for(int i=0 ; i<3 ; ++i){
            list.add(new ArrayList<>());
        }
        System.out.println(list.get(0).get(3) == null);
    }
}
