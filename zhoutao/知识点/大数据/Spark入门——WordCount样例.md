#### WordCount模型学习
给两个文件，文件内有字符串文本，要求对字符串分隔出单词，然后聚合统计每个单词出现的次数

##### 方法一、基础方法
1. 读取文件，读出数据行
2. 分词，对数据行分成一个个单词
3. 分组，将相同单词分到同一组
4. 转换，计算出同一组的数目
5. 打印

```
object Spark01_WordCount {

  def main(args: Array[String]): Unit = {

    // 1. 建立和Spark框架的连接
    val sparkConf = new SparkConf().setMaster("local").setAppName("WordCount")
    val sc = new SparkContext(sparkConf)
    // 2. 执行业务操作

    // 2.1 读取文件，数据行读取
    val lines: RDD[String] = sc.textFile(path = "datas/*")
    // 2.2 分词 扁平化：将整体拆分成个体
    val words: RDD[String] = lines.flatMap(_.split(" "))
    // 2.3 分组
    val wordGroup: RDD[(String, Iterable[String])] = words.groupBy(word => word)
    // 2.4 转换
    val wordToCount = wordGroup.map {
      case (word, list) => {
        (word, list.size)
      }
    }
    // 2.5 采集到控制台
    val array: Array[(String, Int)] = wordToCount.collect()
    array.foreach(println)
    // 3. 关闭连接
    sc.stop()
  }
}
```

##### 方法二、利用聚合思想，提前对于每一个词设定统计值为1
1. 读取文件，读出数据行
2. 分词，对数据行分成一个个单词
3. 初始化，对每一个单词，初始化值为1
4. 分组，将相同单词分到同一组
5. 转换，归并求和
6. 打印
```
object Spark02_WordCount {

  def main(args: Array[String]): Unit = {

    // 1. 建立和Spark框架的连接
    val sparkConf = new SparkConf().setMaster("local").setAppName("WordCount")
    val sc = new SparkContext(sparkConf)
    // 2. 执行业务操作

    // 2.1 读取文件，数据行读取
    val lines: RDD[String] = sc.textFile(path = "datas/*")
    // 2.2 分词 扁平化：将整体拆分成个体
    val words: RDD[String] = lines.flatMap(_.split(" "))

    // 2.3 初始化
    val wordToOne = words.map(
      word => (word, 1)
    )
    // 2.4 分组
    val wordGroup: RDD[(String, Iterable[(String, Int)])] = wordToOne.groupBy(t => t._1)

    // 2.5 转换
    val wordToCount = wordGroup.map {
      case (word, list) => {
        list.reduce(
          (t1, t2) => {
            (t1._1, t1._2 + t2._2)
          }
        )
      }
    }
    // 2.6 采集到控制台
    val array: Array[(String, Int)] = wordToCount.collect()
    array.foreach(println)
    // 3. 关闭连接
    sc.stop()
  }
}
```
##### 方法三、使用Spark的API，分组聚合函数（reduceByKey）
1. 读取文件，读出数据行
2. 分词，对数据行分成一个个单词
3. 初始化，对每一个单词，初始化值为1
4. 分组聚合，将相同单词分到同一组
5. 打印

```
object Spark03_WordCount {

  def main(args: Array[String]): Unit = {

    // 1. 建立和Spark框架的连接
    val sparkConf = new SparkConf().setMaster("local").setAppName("WordCount")
    val sc = new SparkContext(sparkConf)
    // 2. 执行业务操作

    // 2.1 读取文件，数据行读取
    val lines: RDD[String] = sc.textFile(path = "datas/*")
    // 2.2 分词 扁平化：将整体拆分成个体
    val words: RDD[String] = lines.flatMap(_.split(" "))

    // 2.3 初始化
    val wordToOne = words.map(
      word => (word, 1)
    )

    // 2.4
    // Spark：分组和聚合可以使用一个方法实现——reduceByKey:相同的key，可以对value进行reduce聚合
    // 自简原则 如果参数只被使用一次，可以使用下划线代替
    val wordToCount: RDD[(String, Int)] = wordToOne.reduceByKey(_ + _)

    // 2.5 采集到控制台
    val array: Array[(String, Int)] = wordToCount.collect()
    array.foreach(println)
    // 3. 关闭连接
    sc.stop()
  }
}

```