jedis-x
=======

Jedis-x is a [Redis](http://redis.io) Java client and an extension of [Jedis](https://github.com/xetorthio/jedis). 
It was primarily built to detect and remove the failed node automatically, 
also add the failed node that back to normal automatically.<br>

#Reference
[1] [Redis](http://redis.io)<br>
[2] [Redis - @江南白衣](https://github.com/springside/springside4/wiki/Redis)<br>
[3] [Jedis 源码剖析笔记](https://github.com/EdwardLee03/jedis-sr)<br>

#Change log
### 2015.1.6
* 调整整个Redis集群节点的有效性检测方式，由基于Commons Pool 2的"空闲对象驱逐检测机制"改为"定期的Redis服务器状态检测机制"<br>

### 2014.12.27
* 增加Redis服务定义及基于Jedis的自定义服务实现类<br>
* 基于Jedis定制实现的"Redis服务器异常(宕机)时自动摘除，恢复正常时自动添加"的功能<br>
* 基于Spring工厂Bean实现的"自定义数据分片的Jedis连接池"工厂<br>
