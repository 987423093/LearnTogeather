1. 线程安全的，适用于读多写少的场景,不能保证数据一致性，只能保证最终一致性
2. 内部使用ReentrantLock在增，删，改的时候加锁
3. 每次进行增，删，改的时候都通过ReentrantLock加锁，并且拷贝副本，此时读操作，读的是旧的数据

1. 增
```
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```
2. 修改
```
public E set(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        E oldValue = get(elements, index);

        if (oldValue != element) {
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            newElements[index] = element;
            setArray(newElements);
        } else {
            // Not quite a no-op; ensures volatile write semantics
            setArray(elements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

3. 删除
```
public E remove(int index) {
     final ReentrantLock lock = this.lock;
     lock.lock();
     try {
         Object[] elements = getArray();
         int len = elements.length;
         E oldValue = get(elements, index);
         int numMoved = len - index - 1;
         if (numMoved == 0)
             setArray(Arrays.copyOf(elements, len - 1));
         else {
             Object[] newElements = new Object[len - 1];
             System.arraycopy(elements, 0, newElements, 0, index);
             System.arraycopy(elements, index + 1, newElements, index,
                              numMoved);
             setArray(newElements);
         }
         return oldValue;
     } finally {
         lock.unlock();
     }
}
```
