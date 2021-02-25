import java.util.HashMap;

/**
 * @Description: lru缓存工具类
 * @Author: xinyuzang
 * @Date: 2021/2/18 14:34
 */
public class LruCacheHash<K, V> {

    /**
     * hash表，快速查找
     */
    private HashMap<Integer, Node> hashMap;

    /**
     * 双端链表
     */
    private NodeList nodeList;

    /**
     * 无参私有化
     */
    private LruCacheHash() {

    }

    /**
     * 构造
     *
     * @param capacity
     */
    public LruCacheHash(Integer capacity) {
        hashMap = new HashMap<>(capacity);
        nodeList = new NodeList(capacity);
    }

    /**
     * put方法
     *
     * @param key
     * @param value
     */
    public void put(K key, V value) {

        int hashCode = (key == null) ? 0 : key.hashCode();
        Node node = hashMap.get(hashCode);

        Node newNode = new Node(hashCode, value);
        // 如果没有节点，放到最前面

        if (node == null) {
            nodeList.addHead(newNode);
            hashMap.put(hashCode, newNode);
        } else {
            // 如果有，要删掉再加一个
            if (node.key.equals(key)) {
                nodeList.remove(node);
                nodeList.addHead(newNode);
                hashMap.put(hashCode, newNode);
            } else {
                Node nextNode = node;
                boolean flag = false;
                while ((nextNode = nextNode.next) != null) {
                    // 如果hashCode不一样直接返回null
                    if (nextNode.key.hashCode() != hashCode) {
                        break;
                    }
                    if (nextNode.key.equals(key)) {
                        nodeList.remove(nextNode);
                        nodeList.addHead(newNode);
                        hashMap.put(hashCode, newNode);
                        flag = true;
                        break;
                    }
                }
                // 如果是因为hashcode不一致/遍历完都没有，即将当前节点挂到最后
                if (!flag) {
                    if (nextNode != null) {
                        if (nextNode.pre != null) {
                            nextNode.pre.next = newNode;
                            newNode.pre = nextNode.pre;
                        }
                        if (nextNode.next != null) {
                            newNode.next = nextNode.next;
                            nextNode.next.pre = newNode;
                        }
                    }
                }


                if (nextNode != null) {
                    nextNode.pre = newNode;
                }
                newNode.next = nextNode;
            }
        }
    }


    /**
     * get方法
     *
     * @param key
     * @return
     */
    public V get(K key) {

        int hashCode = (key == null) ? 0 : key.hashCode();
        Node node = hashMap.get(hashCode);
        if (node != null) {
            // 如果hash值一样但是key值不一样，往下找hash值一样的key也一样的节点
            if (!node.key.equals(key)) {
                Node nextNode = node;
                while ((nextNode = nextNode.next) != null) {
                    // 如果hashCode不一样直接返回null
                    if (nextNode.key.hashCode() != hashCode) {
                        return null;
                    }
                    if (nextNode.key.equals(key)) {
                        node = nextNode;
                        break;
                    }
                }
            }
            // 删除当前node节点
            nodeList.remove(node);
            // 将node节点添加到头部
            nodeList.addHead(node);
            return node.value;
        } else {
            return null;
        }
    }


    class NodeList {

        /**
         * 头节点、尾节点
         */
        private Node tail, head;
        /**
         * 容量
         */
        private int maxCapacity;
        /**
         * 当前数目
         */
        private int size;

        public NodeList(Integer capacity) {
            if (capacity == null || capacity <= 0) {
                throw new RuntimeException("容量必须大于0");
            }
            this.maxCapacity = capacity;
        }

        /**
         * 添加节点到头上
         *
         * @param node
         */
        private void addHead(Node node) {

            if (head == null) {
                head = tail = node;
            } else {
                node.next = head;
                head.pre = node;
                head = node;
            }
            if (++size > maxCapacity) {
                removeLast();
            }

        }

        /**
         * 删除node节点
         *
         * @param node
         */
        private void remove(Node node) {

            if (node.pre != null) {
                if (node.next != null) {
                    node.pre.next = node.next;
                    node.next.pre = node.pre;
                } else {
                    node.pre.next = null;
                    tail = node.pre;
                }
            } else {
                if (node.next != null) {
                    node.next.pre = null;
                    head = node.next;
                } else {
                    head = tail = null;
                }
            }
            size--;
        }

        /**
         * 删除最后一个节点
         */
        private void removeLast() {

            if (size == 1) {
                head = tail = null;
            }
            tail.pre = tail;
        }
    }


    class Node {

        /**
         * 映射hash值
         */
        public Integer key;
        /**
         * 存储值
         */
        public V value;
        /**
         * 前一个节点
         */
        public Node pre;
        /**
         * 后一个节点
         */
        public Node next;

        public Node(Integer key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
