import java.util.HashMap;
import java.util.Map;

/**
 * @Description: key和value都是整形
 * @Author: XinYuZang
 * @Date: 2021/2/22 10:43
 */
public class LruCache {

    /**
     * 内部结构
     */
    private class Node {
        int key;
        int value;
        Node pre;
        Node next;

        public Node() {

        }

        public Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * hash表
     */
    private Map<Integer, Node> cacheMap = new HashMap<>();

    /**
     * 目前数目
     */
    private int size;

    /**
     * 容量
     */
    private int capacity;

    /**
     * 头，尾
     */
    private Node head, tail;

    public LruCache() {

    }

    public LruCache(int capacity) {
        cacheMap = new HashMap<>(capacity);
        head = tail = new Node();
        head.next = tail;
        tail.pre = head;
        this.capacity = capacity;
    }

    /**
     * get方法
     *
     * @return
     */
    public int get(int key) {

        Node node = cacheMap.get(key);
        if (node == null) {
            return -1;
        }
        moveToHead(node);
        return node.value;
    }

    /**
     * put方法
     *
     * @param key
     * @param value
     */
    public void put(int key, int value) {

        Node node = cacheMap.get(key);
        if (node == null) {
            Node newNode = new Node(key, value);
            addToHead(newNode);
            cacheMap.put(key, newNode);
            size++;
            if (size > capacity) {
                int removeKey = removeTail();
                cacheMap.remove(removeKey);
                size--;
            }
        } else {
            node.value = value;
            moveToHead(node);
        }
    }

    /**
     * 添加节点到头部
     *
     * @param node
     */
    private void addToHead(Node node) {

        head.next.pre = node;
        node.next = head.next;
        head.next = node;
        node.pre = head;
    }

    /**
     * 将某个节点移到头节点
     *
     * @param node
     */
    private void moveToHead(Node node) {

        removeNode(node);
        addToHead(node);
    }

    /**
     * 移除尾部节点
     */
    private int removeTail() {

        int removeKey = tail.pre.key;
        tail.pre.pre.next = tail;
        tail.pre = tail.pre.pre;
        return removeKey;
    }

    /**
     * 移除某个节点
     *
     * @param node
     */
    private void removeNode(Node node) {

        node.pre.next = node.next;
        node.next.pre = node.pre;
    }

    public static void main(String[] args) {
        LruCache LruCache = new LruCache(2);
        LruCache.put(1, 1);
        LruCache.put(2, 2);
        LruCache.get(1);
        LruCache.put(3, 3);
        System.out.println(LruCache.get(2));
    }
}