#### 循环依赖相关问题
1. Spring是如何解决循环依赖的?
2. 为什么循环依赖需要三级缓存，不能有两级?
3. 三级循环依赖分别缓存的是什么？

循环依赖：A依赖B，B依赖A  

在Spring的生命周期里，当一个bean创建中，有这样的流程：实例化->填充属性->回调->初始化,在填充属性时，加入A有B的依赖，则需要B的bean，然而B也依赖了A，导致互相卡死。

 - 正常情况下是如何做的？  
    Spring最终都是想在单例池生成单例bean，这是第一层，Spring在填充属性之前，将A对应的beanDefinition生成一个原型bean，放入一个map，称为二级缓存，当A填充B的属性时，B里面也有A的依赖，此时B直接看A在一级缓存有没有，没有，从二级缓存取，有了，填充进去，经过一系列流程，生成B的单例bean；A就可以填充B到自己的属性中，继续下面的属性填充，直到生成单例bean。
    
 - 那为什么Spring需要三级缓存？  
   同样A依赖B，B依赖A，可是此时B被切面代理，每次在生成B的单例bean的时候，都会生成一个代理对象，此时C也依赖了B，会导致A依赖的B和C依赖的B不一样，此时就添加了中间一级，原先的二级变成三级，key就是beanName，二级里面value就是B的代理对象，每次要生成bean的单例的时候，先从第二级拿一下，如果有直接返回，如果没有就去生成，放到二级里面

 - 真正的Spring中三层依赖分别存储的是什么？  
    DefaultSingletonBeanRegistry类中
    1. 一级缓存：单例池
        key是beanName，value是实例对象
        ```
        /* Cache of singleton objects: bean name to bean instance. */
        private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
        ```
    2. 二级缓存：代理对象池
        key是beanName，value是代理对象
        ```
        /* Cache of early singleton objects: bean name to bean instance. */
        private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
        ```
    3. 三缓存：lamda表达式
        key是beanName，value是lamda表达式，里面是原始对象+后置处理器的方法（如果有AOP的后置处理器，那么执行完生成AOP代理对象放到二级缓存）
        ```
        /* Cache of singleton factories: bean name to ObjectFactory. */
        private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
        ```
#### 解决Spring循环依赖的流程
背景：想要生成A,B的bean，A依赖B，B依赖A，A和B都是非懒加载的单例类，B是被AOP切面切到的类  
首先在ApplicationContext启动时，会创建所有非懒加载的单例bean

1. 【AbstractBeanFactory-doGetBean方法】  
    从单例池去捞 
    ```
    （251） // Eagerly check singleton cache for manually registered singletons.
    （252） Object sharedInstance = getSingleton(beanName);  // Map<>
    ```
2. 【DefaultSingletonBeanRegistry-getSingleton方法】  
    单例池没有，并且不是正在创建的，直接返回null
    ```
    (190) protected Object getSingleton(String beanName, boolean allowEarlyReference) {
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
                //此时下面的都不会执行
                synchronized (this.singletonObjects) {
                    // 没有earlySingletonObjects会怎么样？
                    singletonObject = this.earlySingletonObjects.get(beanName);
                    if (singletonObject == null && allowEarlyReference) {
                        // 为什么需要singletonFactories？
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            singletonObject = singletonFactory.getObject();  // 执行lambda AOp
                            this.earlySingletonObjects.put(beanName, singletonObject);
                            this.singletonFactories.remove(beanName);
                        }
                    }
                }
            }
    (207)}
    ```
3. 【AbstractBeanFactory-doGetBean方法】  
    如果上述bean返回null，去创建一个单例bean
    ```
    // 根据Scope去创建bean
    (236) if (mbd.isSingleton()) {
            // 获取单例bean，如果获取不到则创建一个bean，并且放入单例池中
            sharedInstance = getSingleton(beanName, () -> {
                try {
                    return createBean(beanName, mbd, args);
                }
                catch (BeansException ex) {
                    destroySingleton(beanName);
                    throw ex;
                }
            });
            // sharedInstance可能是一个FactoryBean，所以需要单独再去factoryBeanObjectCache中去获取对应的对象
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
    (353)  }
    ```
4. 【DefaultSingletonBeanRegistry-getSingleton方法（和上面的重载，不是同一个方法）】  
    1.声明当前bean正在被创建 2.执行上面的createBean方法
    ```
    (236) beforeSingletonCreation(beanName);
     ...
    (245) // singletonFactory是外面传进来的lambda表达式,执行lambda表达式
    (246) singletonObject = singletonFactory.getObject();  // createBean()
    ```
5. 【AbstractAutowiredCapableBeanFactory】  
    1.做实例化之前的工作 2.创建原始对象 3.生成二级，三级缓存
    ```
    (528) Object beanInstance = doCreateBean(beanName, mbdToUse, args);
     ...
    (573) instanceWrapper = createBeanInstance(beanName, mbd, args);
     ...
    (602) // 如果当前创建的是单例bean，并且允许循环依赖，并且还在创建过程中，那么则提早暴露
          boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                    isSingletonCurrentlyInCreation(beanName));
          if (earlySingletonExposure) {
             if (logger.isTraceEnabled()) {
                logger.trace("Eagerly caching bean '" + beanName +
                            "' to allow for resolving potential circular references");
             }
             // 此时的bean还没有完成属性注入，是一个非常简单的对象
             // 构造一个对象工厂添加到singletonFactories中
             // 第四次调用后置处理器
             addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));  // AService
    (613) }
    ```
6. 【DefaultSingletonBeanRegistry-addSingletonFactory方法】  
    在三级缓存添加包含A的原始对象的lamda表达式，依赖于是否有AOP生成代理对象还是原始对象
    ```
    (152) protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
            Assert.notNull(singletonFactory, "Singleton factory must not be null");
                synchronized (this.singletonObjects) {
                    if (!this.singletonObjects.containsKey(beanName)) {
                        // 三级缓存
                        this.singletonFactories.put(beanName, singletonFactory);
                        // 二级缓存
                        this.earlySingletonObjects.remove(beanName);
                        this.registeredSingletons.add(beanName);
                    }
                }
    (163)  }
    ```
7. 【AbstractAutowiredCapableBeanFactory-getEarlyBeanReference方法】  
    aop后置处理器执行位置,AutowiredAnnotationBeanPostProcessor继承了SmartInstantiationAwareBeanPostProcessor，并重写getEarlyBeanReference方法.  
    当前A设定为没有AOP就返回了一个普通对象
    ```
    (1001) protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
             Object exposedObject = bean;
             if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                for (BeanPostProcessor bp : getBeanPostProcessors()) {  // AOP
                   if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                       SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                       exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
                    }
                }
             }
             return exposedObject;
    (1012)}
    ```
8. 【AbstractAutowiredCapableBeanFactory-doCreateBean】  
    去填充A里面字段的属性
    ```
    (620) populateBean(beanName, mbd, instanceWrapper);
    ```
9. 【AbstractAutowiredCapableBeanFactory-populateBean方法】  
    当前这个A的bean是byType还是byName的，如果是beanName，他里面的属性都是根据byName去寻找对应的bean，此时就找B的bean，发现Bean没有，重新从第一步开始执行
    ```
    (1478) // Add property values based on autowire by name if applicable.
           if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
              autowireByName(beanName, mbd, bw, newPvs);
           }
           // Add property values based on autowire by type if applicable.
           if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
             autowireByType(beanName, mbd, bw, newPvs);
    (1484) }
    ```
10. 再执行九步骤一直执行到B属性填充A的位置，A去getBean然后又回到了第一步
 
11. 【DefaultSingletonBeanRegistry-getSingleton方法】  
    B注入A成功，注入的是A的二级缓存里面的代理对象。此时缓存情况:  
    【A】：二级缓存 【B】：三级缓存  
    此时三级缓存里面有数据了，执行三级缓存里面的lamda表达式生成代理对象/普通对象放到二级缓存，并且删去三级缓存，保证三级缓存二级缓存之间互相转换，没有同时存在的时刻，加了锁，保证原子性。返回二级缓存数据，之前的else全部不执行。
    ```
    （190) protected Object getSingleton(String beanName, boolean allowEarlyReference) {
             Object singletonObject = this.singletonObjects.get(beanName);
             if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
                synchronized (this.singletonObjects) {
                   // 没有earlySingletonObjects会怎么样？
                   singletonObject = this.earlySingletonObjects.get(beanName);
        
                if (singletonObject == null && allowEarlyReference) {
                   // 为什么需要singletonFactories？
                   ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                   if (singletonFactory != null) {
                      // 执行lambda AOP
                      singletonObject = singletonFactory.getObject();  
                      this.earlySingletonObjects.put(beanName, singletonObject);
                      this.singletonFactories.remove(beanName);
                   }
                }
             }
    （206) }
    ```
12. 【AbstractAutowiredCapableBeanFactory-doCreateBean】  
    填充属性之后在初始化之后生成b的代理对象
    ```
    (619) // 3、填充属性 @Autowired
           populateBean(beanName, mbd, instanceWrapper);
    (623) // 4、 初始化 和 BeanPostProcessor
           exposedObject = initializeBean(beanName, exposedObject, mbd);
     ...
    (1910) if (mbd == null || !mbd.isSynthetic()) {
           // 4.4、初始化后 AOP  （）
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    (1913) }
    ```
13. 【AbstractAutoProxyCreator-postProcessAfterInitialization】  
    ``` 
    (318) public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
            if (bean != null) {
                Object cacheKey = getCacheKey(bean.getClass(), beanName);
                // earlyProxyReferences中存的是哪些提前进行了AOP的bean，beanName:AOP之前的对象
                // 注意earlyProxyReferences中并没有存AOP之后的代理对象  BeanPostProcessor
                if (this.earlyProxyReferences.remove(cacheKey) != bean) {
                    // 没有提前进行过AOP，则进行AOP
                    return wrapIfNecessary(bean, beanName, cacheKey);
                }
            }
            // 为什么不返回代理对象呢？因为这是后置处理器，后续可能会用到该bean
            return bean;
    (330) }
    ```
14. 【AbstractAutoProxyCreator-wrapIfNecessary】  
    判断是否还要进行aop，如果已经进行过了就不要进行aop
    ```
    (381) if (specificInterceptors != DO_NOT_PROXY) {
             this.advisedBeans.put(cacheKey, Boolean.TRUE);
             // 基于bean对象和Advisor创建代理对象
             Object proxy = createProxy(
                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
             // 存一个代理对象的类型
             this.proxyTypes.put(cacheKey, proxy.getClass());
             return proxy;
             }
             this.advisedBeans.put(cacheKey, Boolean.FALSE);
    (392)    return bean;
    ```
    **因为B没有二级缓存，直接返回B的代理对象，然后再执行A的bean生命周期**
15. 【AbstractAutowiredCapableBeanFactory-doCreateBean】  
    A直接从二级缓存获取bean，放到一级缓存
    ```
    (638) if (earlySingletonExposure) {
            // 在解决循环依赖时，当AService的属性注入完了之后，从getSingleton中得到AService AOP之后的代理对象
            Object earlySingletonReference = getSingleton(beanName, false);  // earlySingletonObjects
            if (earlySingletonReference != null) {
                // 如果提前暴露的对象和经过了完整的生命周期后的对象相等，则把代理对象赋值给exposedObject
                // 最终会添加到singletonObjects中去
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference;
                }
                ...
    (667)   
    ```
    

为什么要第15的代码？
如果提前进行了AOP，就不进行AOP，earlySingletonExposure为true，将二级缓存的代理对象赋值给exposedObject
如果没有循环依赖，exposedObject为false，直接就是代理对象