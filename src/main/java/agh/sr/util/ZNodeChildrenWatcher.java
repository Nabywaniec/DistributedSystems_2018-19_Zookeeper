package agh.sr.util;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static agh.sr.ZooKeeperClient.TEST_NODE;

public class ZNodeChildrenWatcher implements Watcher, ZooKeeperErrorHandler  {

    private final ZooKeeper zooKeeper;

    private boolean shouldBeRegistered = false;
    private int childrenAmmount = -1;


    public ZNodeChildrenWatcher(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        registerAsWatcher();
    }

    public void process(WatchedEvent watchedEvent) {

            registerAsWatcher();

            try {
                this.zooKeeper.getChildren(TEST_NODE, this);
                handleCountingDescendants();
            } catch (KeeperException e) {
                handleKeeperException(e, TEST_NODE);
            } catch (InterruptedException e) {
                handleOtherException();
            }
        }


    private void handleCountingDescendants() throws KeeperException, InterruptedException {
        Stat stat = this.zooKeeper.exists(TEST_NODE, false);
        if (stat != null) {
            int descendants = countDescendantsOfMainNode();
            stat = this.zooKeeper.exists(TEST_NODE, false);
            if (stat != null) {
                if (childrenAmmount != descendants) {
                    System.out.println("Liczba potomków : ");
                    System.out.println(descendants);
                    childrenAmmount = descendants;
                }
            }
        }
    }

    private int countDescendantsOfMainNode() {
        return countDescendantsOfGivenNode(TEST_NODE);
    }

    private int countDescendantsOfGivenNode(String node) {
        try {
            List<String> children  = this.zooKeeper.getChildren(node, false);
            return children.size() + children.stream().mapToInt(child -> countDescendantsOfGivenNode(node.concat("/" + child))).sum();
        } catch (Exception e) {
            return 0;
        }

    }


    private void registerAsWatcher() {
        try {
            if (zooKeeper.exists(TEST_NODE, this) != null) {
                final Queue<String> remainedNodes = new LinkedList<>();
                remainedNodes.add(TEST_NODE);

                while (!remainedNodes.isEmpty()) {
                    final String node = remainedNodes.poll();
                    final List<String> children = zooKeeper.getChildren(node, this);
                    remainedNodes.addAll(children.stream().map(childrenName -> node + "/" + childrenName).collect(
                            Collectors.toList()));
                }
            }
        } catch (final KeeperException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void register() {
        shouldBeRegistered = true;
        registerAsWatcher();
    }

    public void unregister() {
        shouldBeRegistered = false;
    }

}