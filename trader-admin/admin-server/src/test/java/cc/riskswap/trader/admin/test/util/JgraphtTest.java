package cc.riskswap.trader.admin.test.util;

import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JgraphtTest {

    @Test
    public void testJgrapht() {
        // 创建无向图，相当于nx.Graph()
        Graph<Object, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        
        // 添加边，相当于g.add_edges_from([(1, 2), (1, 3), (2, 3),('E', 'F')])
        g.addVertex(1);
        g.addVertex(2);
        g.addVertex(3);
        g.addVertex("E");
        g.addVertex("F");
        g.addVertex("G");
        
        g.addEdge(1, 2);
        g.addEdge(1, 3);
        g.addEdge(2, 3);
        g.addEdge("E", "F");
        g.addEdge(1, "E");
        
        // 获取连通分量，相当于nx.connected_components(g)
        ConnectivityInspector<Object, DefaultEdge> inspector = new ConnectivityInspector<>(g);
        List<Set<Object>> components = inspector.connectedSets();
        
        // 打印每个连通分量
        for (Set<Object> component : components) {
            System.out.println(component);
        }
    }
}
