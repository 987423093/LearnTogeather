#### 一、SpringAOP如何实现代理？

##### 代理的必要两个条件
1. Advisor（进行增强的类）
    1. pointcut：切点，说明在哪个地方增强
    2. advice：通知，说明如何增强
2. 目标类（需要增强的类）
##### SpringAOP核心代理类：ProxyFactory
实现和代理的逻辑类一一对应
1. SpringAOP代理核心类：ProxyFactory，里面就有两个最最关键的方法
    1. addAdvisor：放入增强类
    2. setTarget：放入需要增强类
2. PointcutAdvisor：增强类三个方法
    1. getPointcut：切点
    2. getAdvice：增强方法
    3. isPerInstance
3. 业务对象

##### 都有哪些Advice？
1. 前置Advice：MethodBeforeAdvice
2. 后置Advice：AfterReturningAdvice
3. 环绕Advice：MethodInterceptor
4. 异常Advice：ThrowsAdvice

***
##### SpringAOP的手动代理
1. ProxyService
```java
public class ProxyService {

    public static void main(String[] args) {

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(new MyAdvisor());
        proxyFactory.setTarget(new AService());
        AService service = (AService) proxyFactory.getProxy();
        service.test();
    }
}
```
2. MyAdvisor
```java
public class MyAdvisor implements PointcutAdvisor {
    
    /**
     * 切点
     * @return
     */
    @Override
    public Pointcut getPointcut() {
        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.addMethodName("test");
        return pointcut;
    }

    @Override
    public Advice getAdvice() {

        MethodBeforeAdvice advice = new MethodBeforeAdvice() {
            @Override
            public void before(Method method, Object[] objects, Object o) throws Throwable {
                System.out.println("before");
            }
        };
        return advice;
    }

    @Override
    public boolean isPerInstance() {
        return false;
    }
}
```
##### SpringAOP的自动代理
1. 一句话原理：使用后置处理器对符合条件的类自动做代理
2. 具体实现：
    1. EnableAspectJAutoProxy注解：使用@import注入AnnotationAwareAspectJAutoProxyCreator，后续单开SpringBoot自动装配篇
    2. 注入AnnotationAwareAspectJAutoProxyCreator的祖类AbstractAutoProxyCreator实现SmartInstantiationAwareBeanPostProcessor
    3. SmartInstantiationAwareBeanPostProcessor继承InstantiationAwareBeanPostProcessor
    4. AbstractAutoProxyCreator实现了postProcessAfterInitialization方法，在初始化之后做操作扫描所有符合条件的advisor，包装成为代理对象
    5. 核心方法wrapIfNecessary
    
***
#### 二、SpringAOP的自动代理模式

1. JDK动态代理：就是Java的动态代理
2. CGLIB动态代理：是Spring自己实现的CGLIB，org.springframework.cglib.proxy.Enhancer

核心判断代码
```java
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
	    // 1.optimize和CGLIB优化有关，默认为false 2.proxyTargetClass，是否开启cglib，默认为false 3.没有添加interface
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 如果代理类是接口(自动注入的时候会把接口转换为具体实现)或者是Proxy派生的类，使用jdk
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// 使用cglib
			return new ObjenesisCglibAopProxy(config);
		}
		else {
		    // 其他都是jdk
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
```
总结：
1. JDK动态代理：实现了非SpringProxy接口的所有类
2. CGLIB代理：没有手动添加代理接口或者开启了CGLIB参数的所有非接口并且非派生于Proxy的类
3. 一般来说，业务开发的时候，都是CGLIB动态代理
4. ×**实现了接口的类走jdk动态代理，不实现接口的走cglib动态代理**，这句话不准确，正确回答应该是**目标类是接口的一定走JDK动态代理，目标类不是接口的可能走CGLIB，可能走JDK动态代理（目标类为Proxy时，取其接口）** 。类实现了接口并不一定走jdk动态代理，依赖注入之后接口最后都变成了实现类，都走：不是接口不是派生于Proxy这条路径。
***
#### 三、核心代理代码解析
1. wrapIfNecessary
    作用：
     1. 收集所有需要被代理的类的缓存，advisedBeans（beanName-true/false）
     2. 收集当前bean匹配的所有advisor并且生成对应代理对象，proxyTypes（beanName-proxy对象）
```java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		// 当前这个bean不用被代理
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}

		// 如果是基础bean，或者@Aspect修饰的类不需要被代理
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		// 获取当前beanClass所匹配的advisors
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);

		// 如果匹配的advisors不等于null，那么则进行代理，并返回代理对象
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			// 基于bean对象和Advisor创建代理对象
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			// 存一个代理对象的类型
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
	// ...
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);	// 从this复制配置参数
		// 是否指定了必须用cglib进行代理
		if (!proxyFactory.isProxyTargetClass()) {
			// 如果没有指定，那么则判断是不是应该进行cglib代理（判断BeanDefinition中是否指定了要用cglib）
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				// 是否进行jdk动态代理
				evaluateProxyInterfaces(beanClass, proxyFactory); // 判断beanClass有没有实现接口
			}
		}

		// 添加一些commonInterceptors
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);	// 向ProxyFactory中添加advisor
		proxyFactory.setTargetSource(targetSource); // 被代理的对象
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);	//
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// 生成代理对象
		return proxyFactory.getProxy(getProxyClassLoader());
	}
}
```
2. shouldSkip
    作用：
    1. 创建所有@Advisor修饰的bean
    2. 判断当前bean是否需要跳过代理
```java
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {
	@Override
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: Consider optimization by caching the list of the aspect names
		// 得到类型为Advisor的bean，以及被@Aspect注解修饰了的类中所定义的@Before等
		List<Advisor> candidateAdvisors = findCandidateAdvisors();

		// 如果当前beanName是AspectJPointcutAdvisor，那么则跳过
		for (Advisor advisor : candidateAdvisors) {
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		return super.shouldSkip(beanClass, beanName);
	}
}

// 获取Advisor类型的bean
public class BeanFactoryAdvisorRetrievalHelper {
    public List<Advisor> findAdvisorBeans() {
        // ...
        advisors.add(this.beanFactory.getBean(name, Advisor.class));
        // ...
    }
}
```

3. JdkDynamicAopProxy的invoke/CglibAopProxy的intercept 
***
#### 四、Java的动态代理

##### JDK动态代理
1. 三要素
    1. 类加载器
    2. 被代理类实现的接口，接口数组
    3. 增强方法，实现InvocationHandler
2. 实现原理：【利用反射】生成代理类，继承Proxy，实现被代理类；Proxy里面有一个属性protected InvocationHandler h，在生成的代理类中每一个方法都会调用this.h.invoke方法，在invoke方法（实现InvocationHandler的类）里面就可以自定义增强，再反射调用之前的方法；

Demo

1. 代理逻辑
```java
public class JdkProxy {

    public static void main(String[] args) {

        Class[] interfaces = {AService.class};
        AService aService = (AService) Proxy.newProxyInstance(JdkProxy.class.getClassLoader(), interfaces, new AServiceProxy(new AServiceImpl()));
        aService.test();
    }
}
```
2. 代理类
```java
public class AServiceProxy implements InvocationHandler {

    private Object target;

    AServiceProxy(Object target) {
        this.target = target;
    }
  
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("jdk代理前");
        Object obj = method.invoke(target, args);
        System.out.println("jdk代理后");
        return obj;
    }
}
```
##### CGLIB动态代理
1. CGLIB需要自己引入pom;net.sf.cglib.proxy
```xml
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib</artifactId>
    <version>2.2.2</version>
</dependency>
```
2. 实现原理：使用ASM字节码技术，生成代理类，继承被代理类

Demo

1. 代理逻辑
```java
public class CglibProxy  {

    public static void main(String[] args) {

        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new BServiceProxy());
        enhancer.setSuperclass(BService.class);
        BService bService = (BService) enhancer.create();
        bService.test();
    }
}
```
2. 代理类
```java
public class BServiceProxy implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("cglib代理前");
        Object o1 = methodProxy.invokeSuper(o, objects);
        System.out.println("cglib代理后");
        return o1;
    }
}
```
***
##### 总结
1. SpringAOP自动代理实现原理：引入AnnotationAwareAspectJAutoProxyCreator，可以使用@Aspect注明是切面类，是AbstractAutoProxyCreator的子类，在初始化之后的后置处理器内部生成代理对象
2. 什么时候使用JDK动态代理/CGLIB动态代理：当一个bean的目标类是接口/Proxy时，使用JDK动态代理，否则使用CGLIB动态代理
3. SpringAOP在Spring生命周期的什么阶段：  
    1. 实例化之前，（1）校验当前bean是否要被切，在AspectJAwareAdvisorAutoProxyCreator的shouldSkip内，懒加载所有的切面信息，（2）如果自己实现了customTargetSourceCreators就提前产生代理对象，返回
    2. 实例化与实例化之后（指的是实例化后的后置处理器方法）之间，为了解决循环依赖，提早进行AOP 
    3. 初始化之后，使用earlyProxyReferences（key为beanName，value为原始对象）作为缓存判断是否进行过AOP，如果没有进行aop，进行aop后返回，如果提早进行过aop，返回原始对象 
4. 一个类能被多个切面切吗？ 可以
5. 同一个切面顺序：around before -> before -> invoke -> around after -> after
6. @Order 可以调整切面顺序